package com.studyhub.app.data.model

import com.google.firebase.firestore.DocumentId

// Every model has a no-arg default so Firestore's toObject() can build it

data class UserProfile(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val dateOfBirth: String = "",
    val course: String = "Information Technology",
    val yearOfStudy: String = "3rd Year",
    val photoUrl: String = "",
    val points: Int = 0,
    val tasksCompleted: Int = 0,
    val provider: String = "password", // password | google.com | apple.com
    val createdAt: Long = System.currentTimeMillis()
)

data class StudyGroup(
    @DocumentId val id: String = "",
    val name: String = "",
    val subject: String = "",
    val description: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val iconKey: String = "database",
    val createdAt: Long = System.currentTimeMillis()
) {
    val memberCount: Int get() = memberIds.size
}

enum class Priority { HIGH, MEDIUM, LOW }

data class Task(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: Long = 0L,
    val assignedToId: String = "",
    val assignedToName: String = "",
    val groupId: String = "",
    val priority: String = "MEDIUM",
    val completed: Boolean = false,
    val ownerId: String = ""
) {
    // Falls back to MEDIUM if Firestore ever holds an unexpected value
    val priorityEnum: Priority
        get() = runCatching { Priority.valueOf(priority.uppercase()) }.getOrDefault(Priority.MEDIUM)
}

data class StudySession(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val groupId: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val location: String = "",
    val attendeeIds: List<String> = emptyList(),
    val ownerId: String = ""
) {
    val goingCount: Int get() = attendeeIds.size
}

data class ChatMessage(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val sentAt: Long = System.currentTimeMillis()
)

// Returned by the StudyHub REST API - the daily motivational quote on Home
data class QuoteDto(
    val id: String = "",
    val content: String = "",
    val author: String = ""
)