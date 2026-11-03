package com.studyhub.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.studyhub.app.data.local.PendingTaskOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyHubDao {

    // --- Profile ---
    @Query("SELECT * FROM user_profiles WHERE uid = :uid")
    fun observeProfile(uid: String): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    // --- Groups ---
    @Query("SELECT * FROM study_groups WHERE localUserId = :uid")
    fun observeAllGroups(uid: String): Flow<List<StudyGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<StudyGroupEntity>)

    @Query("DELETE FROM study_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    // --- Tasks ---
    @Query("SELECT * FROM tasks WHERE localUserId = :uid ORDER BY dueDate ASC")
    fun observeAllTasks(uid: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId AND localUserId = :uid LIMIT 1")
    suspend fun getTask(taskId: String, uid: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId AND localUserId = :uid")
    suspend fun deleteTask(taskId: String, uid: String)

    @Query("SELECT COUNT(*) FROM pending_task_operations WHERE localUserId = :uid")
    fun observePendingTaskOperationCount(uid: String): Flow<Int>

    @Query("SELECT taskId FROM pending_task_operations WHERE localUserId = :uid")
    suspend fun getPendingTaskIds(uid: String): List<String>

    @Query("SELECT * FROM pending_task_operations WHERE localUserId = :uid ORDER BY createdAt ASC, operationId ASC")
    suspend fun getPendingTaskOperations(uid: String): List<PendingTaskOperationEntity>

    @Query("SELECT MAX(createdAt) FROM pending_task_operations WHERE localUserId = :uid")
    suspend fun latestPendingTime(uid: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingTaskOperation(operation: PendingTaskOperationEntity)

    @Query("DELETE FROM pending_task_operations WHERE operationId = :operationId")
    suspend fun deletePendingTaskOperation(operationId: String)

    @Transaction
    suspend fun saveTaskAndQueue(task: TaskEntity, operation: PendingTaskOperationEntity) {
        insertTask(task)
        insertPendingTaskOperation(operation.copy(createdAt = maxOf(operation.createdAt, (latestPendingTime(operation.localUserId) ?: 0L) + 1)))
    }

    @Transaction
    suspend fun deleteTaskAndQueue(taskId: String, uid: String, operation: PendingTaskOperationEntity) {
        deleteTask(taskId, uid)
        insertPendingTaskOperation(operation.copy(createdAt = maxOf(operation.createdAt, (latestPendingTime(operation.localUserId) ?: 0L) + 1)))
    }

    // --- Sessions ---
    @Query("SELECT * FROM study_sessions WHERE localUserId = :uid ORDER BY startTime ASC")
    fun observeSessions(uid: String): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<StudySessionEntity>)

    @Query("DELETE FROM study_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    // --- Messages ---
    @Query("SELECT * FROM chat_messages WHERE groupId = :groupId AND localUserId = :uid ORDER BY sentAt ASC")
    fun observeMessages(groupId: String, uid: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("SELECT * FROM tasks WHERE localUserId = :uid")
    suspend fun getTasks(uid: String): List<TaskEntity>
    @Query("SELECT * FROM study_sessions WHERE localUserId = :uid")
    suspend fun getSessions(uid: String): List<StudySessionEntity>
    @Query("SELECT * FROM study_groups WHERE localUserId = :uid")
    suspend fun getGroups(uid: String): List<StudyGroupEntity>
    @Query("DELETE FROM study_groups WHERE localUserId = :uid")
    suspend fun clearGroups(uid: String)
    @Transaction
    suspend fun replaceGroups(uid: String, groups: List<StudyGroupEntity>) {
        clearGroups(uid)
        insertGroups(groups)
    }
    @Query("DELETE FROM study_sessions WHERE localUserId = :uid")
    suspend fun clearSessions(uid: String)
    @Transaction
    suspend fun replaceSessions(uid: String, sessions: List<StudySessionEntity>) {
        clearSessions(uid)
        insertSessions(sessions)
    }
    @Query("DELETE FROM tasks WHERE localUserId = :uid AND id NOT IN (SELECT taskId FROM pending_task_operations WHERE localUserId = :uid)")
    suspend fun clearSyncedTasks(uid: String)
    @Transaction
    suspend fun reconcileTasks(uid: String, tasks: List<TaskEntity>, authoritative: Boolean) {
        val pending = getPendingTaskIds(uid).toSet()
        if (authoritative) clearSyncedTasks(uid)
        insertTasks(tasks.filterNot { it.id in pending })
    }
}
