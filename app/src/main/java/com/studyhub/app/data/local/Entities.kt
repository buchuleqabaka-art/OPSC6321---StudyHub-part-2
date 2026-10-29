package com.studyhub.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.studyhub.app.data.model.ChatMessage
import com.studyhub.app.data.model.StudyGroup
import com.studyhub.app.data.model.StudySession
import com.studyhub.app.data.model.Task
import com.studyhub.app.data.model.UserProfile

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val email: String,
    val dateOfBirth: String,
    val course: String,
    val yearOfStudy: String,
    val photoUrl: String,
    val points: Int,
    val tasksCompleted: Int,
    val provider: String,
    val createdAt: Long
)

fun UserProfileEntity.toModel() = UserProfile(
    uid = uid, name = name, email = email, dateOfBirth = dateOfBirth,
    course = course, yearOfStudy = yearOfStudy, photoUrl = photoUrl,
    points = points, tasksCompleted = tasksCompleted, provider = provider,
    createdAt = createdAt
)

fun UserProfile.toEntity() = UserProfileEntity(
    uid = uid, name = name, email = email, dateOfBirth = dateOfBirth,
    course = course, yearOfStudy = yearOfStudy, photoUrl = photoUrl,
    points = points, tasksCompleted = tasksCompleted, provider = provider,
    createdAt = createdAt
)

@Entity(tableName = "study_groups")
data class StudyGroupEntity(
    @PrimaryKey val id: String,
    val localUserId: String,
    val name: String,
    val subject: String,
    val description: String,
    val ownerId: String,
    val memberIds: List<String>,
    val iconKey: String,
    val createdAt: Long
)

fun StudyGroupEntity.toModel() = StudyGroup(
    id = id, name = name, subject = subject, description = description,
    ownerId = ownerId, memberIds = memberIds, iconKey = iconKey, createdAt = createdAt
)

fun StudyGroup.toEntity(uid: String) = StudyGroupEntity(
    id = id, localUserId = uid, name = name, subject = subject, description = description,
    ownerId = ownerId, memberIds = memberIds, iconKey = iconKey, createdAt = createdAt
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val localUserId: String,
    val title: String,
    val description: String,
    val dueDate: Long,
    val assignedToId: String,
    val assignedToName: String,
    val groupId: String,
    val priority: String,
    val completed: Boolean,
    val ownerId: String
)

fun TaskEntity.toModel() = Task(
    id = id, title = title, description = description, dueDate = dueDate,
    assignedToId = assignedToId, assignedToName = assignedToName,
    groupId = groupId, priority = priority, completed = completed, ownerId = ownerId
)

fun Task.toEntity(uid: String) = TaskEntity(
    id = id, localUserId = uid, title = title, description = description, dueDate = dueDate,
    assignedToId = assignedToId, assignedToName = assignedToName,
    groupId = groupId, priority = priority, completed = completed, ownerId = ownerId
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey val id: String,
    val localUserId: String,
    val title: String,
    val description: String,
    val groupId: String,
    val startTime: Long,
    val endTime: Long,
    val location: String,
    val attendeeIds: List<String>,
    val ownerId: String
)

fun StudySessionEntity.toModel() = StudySession(
    id = id, title = title, description = description, groupId = groupId,
    startTime = startTime, endTime = endTime, location = location,
    attendeeIds = attendeeIds, ownerId = ownerId
)

fun StudySession.toEntity(uid: String) = StudySessionEntity(
    id = id, localUserId = uid, title = title, description = description, groupId = groupId,
    startTime = startTime, endTime = endTime, location = location,
    attendeeIds = attendeeIds, ownerId = ownerId
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val localUserId: String,
    val groupId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val sentAt: Long
)

fun ChatMessageEntity.toModel() = ChatMessage(
    id = id, groupId = groupId, senderId = senderId, senderName = senderName,
    text = text, sentAt = sentAt
)

fun ChatMessage.toEntity(uid: String) = ChatMessageEntity(
    id = id, localUserId = uid, groupId = groupId, senderId = senderId, senderName = senderName,
    text = text, sentAt = sentAt
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String = gson.toJson(value ?: emptyList<String>())

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}
