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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingTaskOperation(operation: PendingTaskOperationEntity)

    @Query("DELETE FROM pending_task_operations WHERE operationId = :operationId")
    suspend fun deletePendingTaskOperation(operationId: String)

    @Transaction
    suspend fun saveTaskAndQueue(task: TaskEntity, operation: PendingTaskOperationEntity) {
        insertTask(task)
        insertPendingTaskOperation(operation)
    }

    @Transaction
    suspend fun deleteTaskAndQueue(taskId: String, uid: String, operation: PendingTaskOperationEntity) {
        deleteTask(taskId, uid)
        insertPendingTaskOperation(operation)
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
}
