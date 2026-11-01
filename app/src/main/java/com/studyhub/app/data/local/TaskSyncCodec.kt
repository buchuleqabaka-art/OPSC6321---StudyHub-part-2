package com.studyhub.app.data.local

import com.google.gson.Gson
import com.studyhub.app.data.model.Task

/** Stable queue payload encoding shared by the repository and WorkManager worker. */
object TaskSyncCodec {
    const val UPSERT = "UPSERT"
    const val DELETE = "DELETE"

    private val gson = Gson()

    fun encode(task: Task): String = gson.toJson(task)
    fun decode(payload: String): Task = gson.fromJson(payload, Task::class.java)
}
