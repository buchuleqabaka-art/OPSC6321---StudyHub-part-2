package com.studyhub.app.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.studyhub.app.data.local.SettingsStore
import com.studyhub.app.data.remote.PushTokenRequest
import com.studyhub.app.data.remote.RetrofitClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/** Registers this device with the authenticated REST API; no FCM server key is stored in the app. */
object PushRegistration {
    private const val TAG = "PushRegistration"

    suspend fun register(context: Context, refreshedToken: String? = null) {
        val appContext = context.applicationContext
        if (!SettingsStore(appContext).notifications.first()) return
        val user = FirebaseAuth.getInstance().currentUser ?: return

        runCatching {
            val token = refreshedToken ?: FirebaseMessaging.getInstance().token.await()
            val idToken = user.getIdToken(false).await().token ?: return
            val response = RetrofitClient.api.registerPushToken(
                "Bearer $idToken",
                PushTokenRequest(token = token)
            )
            check(response.isSuccessful) { "Push token registration returned ${response.code()}" }
            Log.d(TAG, "Push token registered for signed-in account")
        }.onFailure { error ->
            // Push registration is optional; an unavailable API must not block sign-in.
            Log.w(TAG, "Could not register push token. Check API_BASE_URL and Firebase Admin setup.", error)
        }
    }

    suspend fun unregister() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            val idToken = user.getIdToken(false).await().token ?: return
            val response = RetrofitClient.api.unregisterPushToken(
                "Bearer $idToken",
                PushTokenRequest(token = token)
            )
            check(response.isSuccessful) { "Push token unregistration returned ${response.code()}" }
            Log.d(TAG, "Push token removed from signed-in account")
        }.onFailure { error ->
            // The messaging service also checks the active account before showing a push.
            Log.w(TAG, "Could not unregister the push token; account matching still protects the device.", error)
        }
    }
}
