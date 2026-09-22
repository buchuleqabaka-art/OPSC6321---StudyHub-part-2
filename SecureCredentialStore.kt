package com.studyhub.app.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.studyhub.app.util.PasswordSecurity

/**
 * Stores the "remember me" email plus a salted PBKDF2 hash of the password, so a
 * returning student can be verified offline without the raw password ever
 * touching the device. The file itself is AES-256-GCM encrypted by
 * EncryptedSharedPreferences with a key held in the Android Keystore.
 */
class SecureCredentialStore(context: Context) {

    companion object {
        private const val TAG = "SecureCredentialStore"
        private const val FILE = "studyhub_secure_prefs"
        private const val KEY_EMAIL = "cached_email"
        private const val KEY_SALT = "cached_salt"
        private const val KEY_HASH = "cached_hash"
    }

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun remember(email: String, password: String) {
        val salt = PasswordSecurity.generateSalt()
        prefs.edit()
            .putString(KEY_EMAIL, email.lowercase())
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, PasswordSecurity.hash(password, salt))
            .apply()
        Log.d(TAG, "Cached encrypted credentials for offline verification")
    }

    fun rememberedEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun verifyOffline(email: String, password: String): Boolean {
        val savedEmail = prefs.getString(KEY_EMAIL, null) ?: return false
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val hash = prefs.getString(KEY_HASH, null) ?: return false
        return savedEmail.equals(email.trim(), ignoreCase = true) &&
                PasswordSecurity.verify(password, salt, hash)
    }

    fun clear() = prefs.edit().clear().apply()
}