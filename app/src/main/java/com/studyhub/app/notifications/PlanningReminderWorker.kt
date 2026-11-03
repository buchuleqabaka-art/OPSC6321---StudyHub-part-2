package com.studyhub.app.notifications

import android.content.Context
import android.content.res.Configuration
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.studyhub.app.R
import com.studyhub.app.data.local.SettingsStore
import com.studyhub.app.data.local.StudyHubDatabase
import com.studyhub.app.util.PlanningPolicy
import kotlinx.coroutines.flow.first
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Local reminders work from the account's cached planning data, even offline.
 * WorkManager is approximate (battery/Doze can delay delivery), not an exact alarm.
 */
class PlanningReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return Result.success()
        val settings = SettingsStore(applicationContext)
        if (!settings.notifications.first()) return Result.success()
        val dao = StudyHubDatabase.getDatabase(applicationContext).dao()
        val groups = dao.getGroups(uid).map { it.id }.toSet()
        val now = System.currentTimeMillis()
        val events = dao.getTasks(uid).filter {
            !it.completed && it.assignedToId.ifBlank { it.ownerId } == uid &&
                (it.ownerId == uid || it.groupId in groups)
        }.map { Reminder("task", it.id, it.title, it.dueDate) } +
            dao.getSessions(uid).filter {
                uid in it.attendeeIds && (it.ownerId == uid || it.groupId in groups)
            }.map { Reminder("session", it.id, it.title, it.startTime) }
        val config = Configuration(applicationContext.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(settings.language.first()))
        }
        val context = applicationContext.createConfigurationContext(config)
        val delivered = applicationContext.getSharedPreferences("planning_reminders", Context.MODE_PRIVATE)
        for (event in events.filter { PlanningPolicy.reminderDue(it.at, now) }) {
            if (auth.currentUser?.uid != uid || !settings.notifications.first()) return Result.success()
            val key = "$uid:${event.kind}:${event.id}:${event.at}"
            if (delivered.getBoolean(key, false)) continue
            val title = context.getString(if (event.kind == "task") R.string.planning_task_reminder else R.string.planning_session_reminder)
            val body = context.getString(R.string.planning_reminder_body, event.title)
            if (StudyHubNotifications.show(context, title, body, key.hashCode())) {
                delivered.edit().putBoolean(key, true).commit()
            }
        }
        return Result.success()
    }

    private data class Reminder(val kind: String, val id: String, val title: String, val at: Long)

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "studyhub-planning-reminders", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PlanningReminderWorker>(15, TimeUnit.MINUTES).build()
            )
        }
    }
}
