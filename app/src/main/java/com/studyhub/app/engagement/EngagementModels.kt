package com.studyhub.app.engagement

import com.studyhub.app.data.model.StudySession
import kotlin.math.roundToInt

data class AttendanceSummary(val attended: Int, val absent: Int, val unrecorded: Int) {
    val recorded get() = attended + absent
    val percent: Int? get() = if (recorded == 0) null else (attended * 100.0 / recorded).roundToInt()
}
object AttendancePolicy {
    fun summarize(sessions: List<StudySession>, records: Map<String, String>, now: Long): AttendanceSummary {
        val ended = sessions.distinctBy { it.id }.filter { it.endTime <= now }
        val attended = ended.count { records[it.id] == "attended" }
        val absent = ended.count { records[it.id] == "absent" }
        return AttendanceSummary(attended, absent, ended.size - attended - absent)
    }
}
data class GroupPoll(val id: String, val question: String, val options: List<String>, val ownerId: String,
    val closed: Boolean, val votes: Map<String, Int> = emptyMap()) {
    val counts get() = options.indices.map { choice -> votes.values.count { it == choice } }
}
object PollPolicy {
    fun valid(question: String, options: List<String>): Boolean = question.trim().length in 1..200 &&
        options.size in 2..4 && options.all { it.trim().length in 1..100 } &&
        options.map { it.trim().lowercase() }.distinct().size == options.size
}
