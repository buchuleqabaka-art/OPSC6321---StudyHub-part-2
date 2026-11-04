package com.studyhub.app.quiz

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Private, account-scoped device storage. Excluded from cloud/device backup. */
class QuizStore(context: Context, uid: String) {
    private val key = MessageDigest.getInstance("SHA-256").digest(uid.toByteArray()).joinToString("") { "%02x".format(it) }
    private val file = AtomicFile(File(context.noBackupFilesDir, "quiz-$key.json"))
    fun load(): List<PaperQuiz> {
        if (!file.baseFile.exists()) return emptyList()
        val array = JSONArray(file.openRead().use { it.bufferedReader().readText() })
        return (0 until array.length()).map { decode(array.getJSONObject(it)).checked() }
    }
    fun save(quizzes: List<PaperQuiz>) {
        val out = file.startWrite()
        try {
            out.write(JSONArray().apply { quizzes.forEach { put(encode(it.checked())) } }.toString().toByteArray())
            file.finishWrite(out)
        } catch (e: Exception) { file.failWrite(out); throw e }
    }
    companion object {
        fun decode(o: JSONObject): PaperQuiz {
            val qs = o.getJSONArray("questions")
            val answers = o.optJSONArray("answers") ?: JSONArray()
            return PaperQuiz(o.getString("id"), o.getString("title"), (0 until qs.length()).map { i ->
                val q = qs.getJSONObject(i); val opts = q.getJSONArray("options")
                QuizQuestion(q.getString("prompt"), (0 until opts.length()).map { opts.getString(it) }, q.getInt("answer"), q.getString("explanation"), q.getString("source"))
            }, (0 until answers.length()).map { answers.getInt(it) })
        }
        fun encode(q: PaperQuiz): JSONObject = JSONObject().put("id", q.id).put("title", q.title)
            .put("answers", JSONArray(q.answers)).put("questions", JSONArray().apply {
                q.questions.forEach { put(JSONObject().put("prompt", it.prompt).put("options", JSONArray(it.options))
                    .put("answer", it.answer).put("explanation", it.explanation).put("source", it.source)) }
            })
    }
}
