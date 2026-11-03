package com.studyhub.app.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.studyhub.app.data.local.SettingsStore
import com.studyhub.app.data.local.StudyHubDatabase
import com.studyhub.app.data.local.TaskSyncCodec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/** Replays the durable Room outbox only while its owner is signed in and online. */
class PendingTaskSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uid = inputData.getString(TaskSyncScheduler.KEY_UID)?.takeIf { it.isNotBlank() }
            ?: return Result.failure()

        // Never replay one account's queue under another account's credentials.
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) return Result.success()
        if (!SettingsStore(applicationContext).offlineSync.first()) return Result.success()

        val dao = StudyHubDatabase.getDatabase(applicationContext).dao()
        val firestore = FirebaseFirestore.getInstance()
        return try {
            dao.getPendingTaskOperations(uid).forEach { operation ->
                if (FirebaseAuth.getInstance().currentUser?.uid != uid) return Result.success()
                if (!SettingsStore(applicationContext).offlineSync.first()) return Result.success()
                when (operation.operation) {
                    TaskSyncCodec.UPSERT -> {
                        val task = TaskSyncCodec.decode(operation.payload)
                        if (task.id != operation.taskId || task.ownerId != uid) {
                            Log.e(TAG, "Rejected malformed queued task ${operation.taskId}")
                            return Result.failure()
                        }
                        firestore.collection(TASKS).document(operation.taskId).set(task).await()
                    }
                    TaskSyncCodec.COMPLETE -> {
                        val task = TaskSyncCodec.decode(operation.payload)
                        require(task.id == operation.taskId)
                        firestore.collection(TASKS).document(operation.taskId)
                            .update("completed", task.completed).await()
                    }
                    TaskSyncCodec.DELETE -> {
                        firestore.collection(TASKS).document(operation.taskId).delete().await()
                    }
                    else -> {
                        Log.e(TAG, "Unknown queued task operation: ${operation.operation}")
                        return Result.failure()
                    }
                }
                dao.deletePendingTaskOperation(operation.operationId)
            }
            Result.success()
        } catch (error: Exception) {
            Log.w(TAG, "Task sync will retry when connectivity is available", error)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "PendingTaskSyncWorker"
        private const val TASKS = "tasks"
    }
}

object TaskSyncScheduler {
    const val KEY_UID = "owner_uid"
    private const val WORK_PREFIX = "studyhub-task-sync-"

    fun enqueue(context: Context, uid: String) {
        if (uid.isBlank()) return
        val request = OneTimeWorkRequestBuilder<PendingTaskSyncWorker>()
            .setInputData(workDataOf(KEY_UID to uid))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            WORK_PREFIX + uid,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }
}
