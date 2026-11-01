package com.studyhub.app.data.local

import com.studyhub.app.data.model.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskSyncCodecTest {
    @Test
    fun queuedTaskPayloadRoundTripsWithoutLosingTaskState() {
        val original = Task(
            id = "task-123",
            title = "Prepare isiZulu presentation",
            description = "Review the group notes and examples.",
            dueDate = 1_800_000_000_000L,
            assignedToId = "user-1",
            assignedToName = "Lerato",
            groupId = "group-9",
            priority = "HIGH",
            completed = true,
            ownerId = "user-1"
        )

        assertEquals(original, TaskSyncCodec.decode(TaskSyncCodec.encode(original)))
    }

    @Test
    fun operationKindsStayStableForTheWorker() {
        assertEquals("UPSERT", TaskSyncCodec.UPSERT)
        assertEquals("DELETE", TaskSyncCodec.DELETE)
    }
}
