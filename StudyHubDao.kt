package com.studyhub.app.data.local

import androidx.room.*
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

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
