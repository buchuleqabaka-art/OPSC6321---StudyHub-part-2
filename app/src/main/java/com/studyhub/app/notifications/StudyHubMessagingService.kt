package com.studyhub.app.notifications

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.studyhub.app.R
import com.studyhub.app.data.local.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StudyHubMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        serviceScope.launch { PushRegistration.register(applicationContext, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data[KEY_TYPE] == TYPE_GROUP_MESSAGE &&
            data[KEY_RECIPIENT] != FirebaseAuth.getInstance().currentUser?.uid
        ) return
        val (title, body) = if (data[KEY_TYPE] == TYPE_GROUP_MESSAGE) {
            val sender = data[KEY_SENDER].orEmpty().ifBlank { getString(R.string.studyhub_member) }
            val messageText = data[KEY_MESSAGE].orEmpty()
            getString(R.string.notification_new_message_title) to
                getString(R.string.notification_new_message_body, sender, messageText)
        } else {
            (data[KEY_TITLE] ?: message.notification?.title ?: getString(R.string.notification_default_title)) to
                (data[KEY_MESSAGE] ?: message.notification?.body ?: getString(R.string.notification_default_body))
        }

        serviceScope.launch {
            val enabled = runCatching { SettingsStore(applicationContext).notifications.first() }
                .getOrDefault(false)
            if (enabled) {
                val id = message.messageId?.hashCode() ?: System.currentTimeMillis().toInt()
                StudyHubNotifications.show(applicationContext, title, body, id)
            }
        }
    }

    companion object {
        private const val KEY_TYPE = "type"
        private const val TYPE_GROUP_MESSAGE = "group_message"
        private const val KEY_RECIPIENT = "recipientUid"
        private const val KEY_SENDER = "senderName"
        private const val KEY_MESSAGE = "messageText"
        private const val KEY_TITLE = "title"
    }
}
