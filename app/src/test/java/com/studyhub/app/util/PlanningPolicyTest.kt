package com.studyhub.app.util

import com.studyhub.app.data.model.Task
import com.google.firebase.firestore.Exclude
import org.junit.Assert.*
import org.junit.Test

class PlanningPolicyTest {
    @Test fun `accept valid session and reject blank title past start and reversed times`() {
        assertTrue(PlanningPolicy.validSession("Revision", 200, 300, 100))
        assertFalse(PlanningPolicy.validSession("  ", 200, 300, 100))
        assertFalse(PlanningPolicy.validSession("Revision", 100, 300, 100))
        assertFalse(PlanningPolicy.validSession("Revision", 300, 200, 100))
        assertFalse(PlanningPolicy.validSession("Revision", 200, 200, 100))
    }
    @Test fun `title length boundaries`() {
        assertTrue(PlanningPolicy.validTitle("x".repeat(160)))
        assertFalse(PlanningPolicy.validTitle("x".repeat(161)))
        assertFalse(PlanningPolicy.validTitle(""))
    }
    @Test fun `only owner or assignee can complete a task`() {
        val task = Task(ownerId = "alice", assignedToId = "bob")
        assertTrue(PlanningPolicy.canComplete(task, "alice"))
        assertTrue(PlanningPolicy.canComplete(task, "bob"))
        assertFalse(PlanningPolicy.canComplete(task, "eve"))
        assertFalse(PlanningPolicy.canComplete(Task(), ""))
    }
    @Test fun `points belong to assignee and duplicate records do not multiply points`() {
        val task = Task(id = "one", ownerId = "alice", assignedToId = "bob", completed = true)
        assertEquals(10, PlanningPolicy.points(listOf(task, task), "bob"))
        assertEquals(0, PlanningPolicy.points(listOf(task), "alice"))
        assertEquals(0, PlanningPolicy.points(listOf(task.copy(completed = false)), "bob"))
        assertEquals(0, PlanningPolicy.points(emptyList(), "bob"))
    }
    @Test fun `legacy personal tasks count towards their owner`() {
        assertEquals(10, PlanningPolicy.points(listOf(Task(id = "old", ownerId = "alice", completed = true)), "alice"))
    }
    @Test fun `reminders include next hour but exclude expired and distant events`() {
        assertTrue(PlanningPolicy.reminderDue(3600100, 100))
        assertTrue(PlanningPolicy.reminderDue(101, 100))
        assertFalse(PlanningPolicy.reminderDue(100, 100))
        assertFalse(PlanningPolicy.reminderDue(99, 100))
        assertFalse(PlanningPolicy.reminderDue(3600101, 100))
    }
    @Test fun `computed task priority is not serialized into Firestore`() {
        assertTrue(Task::class.java.getMethod("getPriorityEnum").isAnnotationPresent(Exclude::class.java))
    }
}
