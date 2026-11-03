package com.studyhub.app.util

import com.studyhub.app.data.model.Task

object PlanningPolicy {
    fun validTitle(title: String) = title.trim().length in 1..160
    fun validSession(title: String, start: Long, end: Long, now: Long) =
        validTitle(title) && start > now && end > start
    fun canComplete(task: Task, uid: String) = uid.isNotBlank() &&
        (task.ownerId == uid || task.assignedToId == uid)
    fun completedCount(tasks: List<Task>, uid: String): Int = tasks.distinctBy { it.id }.count {
        it.completed && (it.assignedToId.ifBlank { it.ownerId } == uid)
    }
    fun points(tasks: List<Task>, uid: String) = completedCount(tasks, uid) * 10
    fun reminderDue(eventAt: Long, now: Long) = eventAt > now && eventAt - now <= 60 * 60 * 1000L
}
