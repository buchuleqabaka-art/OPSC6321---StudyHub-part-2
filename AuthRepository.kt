package com.studyhub.app.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.studyhub.app.BuildConfig
import com.studyhub.app.data.model.UserProfile
import com.studyhub.app.util.AuthError
import com.studyhub.app.util.AuthField
import com.studyhub.app.util.PasswordSecurity
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

/**
 * All authentication goes through here: email/password, Google (Credential
 * Manager) and Apple/iCloud (Firebase OAuth provider). Every method returns a
 * Result so the ViewModel can map failures onto a specific input field.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "AuthRepository"
        private const val USERS = "users"
        private const val EMAIL_INDEX = "emailIndex"
    }

    val currentUser: FirebaseUser? get() = auth.currentUser
    fun isLoggedIn(): Boolean = auth.currentUser != null

    // --- Email lookup -------------------------------------------------------

    /**
     * Checks whether an address already belongs to a StudyHub account.
     *
     * We cannot use fetchSignInMethodsForEmail because Firebase's
     * email-enumeration protection makes it return an empty list, and opening up
     * the users collection to unauthenticated reads would leak everyone's email.
     * Instead every registration writes a document to emailIndex keyed by the
     * SHA-256 of the address - readable before sign-in, but it contains no
     * personal data and cannot be reversed into an email list.
     */
    suspend fun emailExists(email: String): Boolean {
        val key = emailKey(email)
        return try {
            val doc = firestore.collection(EMAIL_INDEX).document(key).get().await()
            val exists = doc.exists()
            Log.d(TAG, "emailExists check for ${maskEmail(email)} -> $exists")
            exists
        } catch (e: Exception) {
            Log.w(TAG, "emailExists lookup failed, assuming unknown", e)
            // Offline or rules blocked the read - let the sign-in attempt decide
            true
        }
    }

    /** Adds the address to the lookup index. Stores the hash only, never the email. */
    private suspend fun indexEmail(email: String) {
        runCatching {
            firestore.collection(EMAIL_INDEX)
                .document(emailKey(email))
                .set(mapOf("registered" to true))
                .await()
        }.onFailure { Log.w(TAG, "Could not write email index entry", it) }
    }

    private fun emailKey(email: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(email.trim().lowercase().toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    // --- Registration -------------------------------------------------------

    suspend fun register(
        name: String,
        email: String,
        password: String,
        dateOfBirth: String
    ): Result<UserProfile> {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()
        return try {
            // Firebase hashes the password with scrypt server-side; the raw value
            // never leaves this call and is never persisted by the app.
            Log.d(TAG, "Registering ${maskEmail(cleanEmail)} pwdPrint=${PasswordSecurity.fingerprint(password)}")
            val result = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val uid = result.user?.uid ?: return Result.failure(
                IllegalStateException("Firebase returned no user")
            )

            val profile = UserProfile(
                uid = uid,
                name = cleanName,
                email = cleanEmail,
                dateOfBirth = dateOfBirth.trim(),
                provider = "password"
            )
            firestore.collection(USERS).document(uid).set(profile).await()
            indexEmail(cleanEmail)
            Result.success(profile)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(AuthException(AuthError(AuthField.EMAIL, "That email is already registered. Try logging in instead.")))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(AuthException(AuthError(AuthField.PASSWORD, "Password is too weak. Use at least 8 characters with a number.")))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(AuthException(AuthError(AuthField.EMAIL, "That email address is not valid.")))
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            Result.failure(AuthException(AuthError(AuthField.GENERAL, friendlyMessage(e))))
        }
    }

    // --- Email / password login --------------------------------------------

    suspend fun login(email: String, password: String): Result<UserProfile> {
        val cleanEmail = email.trim().lowercase()
        return try {
            val result = auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val uid = result.user?.uid ?: return Result.failure(
                IllegalStateException("Firebase returned no user")
            )
            Result.success(loadOrCreateProfile(uid, cleanEmail, result.user?.displayName.orEmpty(), "password"))
        } catch (e: FirebaseAuthInvalidUserException) {
            // No account for this address - flag the email field, not the password
            Result.failure(AuthException(AuthError(AuthField.EMAIL, "This email is not registered. Check the address or create an account.")))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            // Could be a bad password OR a non-existent account when enumeration
            // protection is on, so ask Firestore which one it actually is.
            val known = emailExists(cleanEmail)
            if (!known) {
                Result.failure(AuthException(AuthError(AuthField.EMAIL, "This email is not registered. Check the address or create an account.")))
            } else {
                Result.failure(AuthException(AuthError(AuthField.PASSWORD, "Incorrect password. Try again or reset it.")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            Result.failure(AuthException(AuthError(AuthField.GENERAL, friendlyMessage(e))))
        }
    }

    // --- Continue with Google ----------------------------------------------

    /**
     * Uses Credential Manager (the replacement for the deprecated
     * GoogleSignInClient) to fetch an ID token, then trades it for a Firebase
     * credential.
     */
    suspend fun signInWithGoogle(context: Context): Result<UserProfile> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // show every account on the device
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = CredentialManager.create(context).getCredential(context, request)
            val credential = response.credential

            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return Result.failure(AuthException(AuthError(AuthField.GENERAL, "Unexpected Google credential type.")))
            }

            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
            val result = auth.signInWithCredential(firebaseCredential).await()
            val user = result.user ?: return Result.failure(
                IllegalStateException("Firebase returned no user")
            )

            Result.success(
                loadOrCreateProfile(
                    uid = user.uid,
                    email = user.email.orEmpty(),
                    name = googleCredential.displayName ?: user.displayName.orEmpty(),
                    provider = "google.com",
                    photoUrl = googleCredential.profilePictureUri?.toString().orEmpty()
                )
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Sign-in failed: ${e.message}", e)
            val errorMessage = if (e.message?.contains("cancel", ignoreCase = true) == true) {
                "Sign-in was cancelled."
            } else {
                "Google Sign-in failed. Please verify your SHA-1 fingerprint and Web Client ID in the Firebase Console."
            }
            Result.failure(AuthException(AuthError(AuthField.GENERAL, errorMessage)))
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed with unexpected error", e)
            Result.failure(AuthException(AuthError(AuthField.GENERAL, friendlyMessage(e))))
        }
    }


    // --- Password reset and sign out ---------------------------------------

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (!emailExists(cleanEmail)) {
            return Result.failure(
                AuthException(AuthError(AuthField.EMAIL, "This email is not registered."))
            )
        }
        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AuthException(AuthError(AuthField.GENERAL, friendlyMessage(e))))
        }
    }

    fun signOut() {
        Log.d(TAG, "Signing out ${auth.currentUser?.uid}")
        auth.signOut()
    }

    suspend fun currentProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            firestore.collection(USERS).document(uid).get().await().toObject(UserProfile::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Could not load profile", e)
            null
        }
    }

    // --- Helpers ------------------------------------------------------------

    private suspend fun loadOrCreateProfile(
        uid: String,
        email: String,
        name: String,
        provider: String,
        photoUrl: String = ""
    ): UserProfile {
        val doc = firestore.collection(USERS).document(uid)
        val existing = doc.get().await().toObject(UserProfile::class.java)
        if (existing != null) return existing

        // First social sign-in: build the profile document now
        val profile = UserProfile(
            uid = uid,
            name = name.ifBlank { email.substringBefore("@") },
            email = email.lowercase(),
            photoUrl = photoUrl,
            provider = provider
        )
        doc.set(profile).await()
        if (email.isNotBlank()) indexEmail(email)
        return profile
    }

    private fun friendlyMessage(e: Exception): String = when {
        e.message?.contains("network", ignoreCase = true) == true ->
            "No internet connection. Check your network and try again."
        e.message?.contains("permission-denied", ignoreCase = true) == true ||
        e.message?.contains("permission denied", ignoreCase = true) == true ->
            "Firestore Permission Denied. Check your security rules in the Firebase Console."
        e.message?.contains("web client id", ignoreCase = true) == true ->
            "Social Sign-in is not configured. The Web Client ID is missing or invalid."
        else -> e.localizedMessage ?: "Something went wrong. Please try again."
    }

    private fun maskEmail(email: String): String {
        val at = email.indexOf('@')
        if (at <= 1) return "***"
        return email.first() + "***" + email.substring(at)
    }
}

/** Carries the field-specific error up to the UI layer. */
class AuthException(val error: AuthError) : Exception(error.message)