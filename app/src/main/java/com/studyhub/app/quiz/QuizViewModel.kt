package com.studyhub.app.quiz

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studyhub.app.BuildConfig
import com.studyhub.app.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

data class QuizState(val saved: List<PaperQuiz> = emptyList(), val active: PaperQuiz? = null,
    val busy: Boolean = false, val error: Int? = null, val selected: Boolean = false)
class QuizViewModel(app: Application) : AndroidViewModel(app) {
    private val auth = FirebaseAuth.getInstance()
    private var uid = auth.currentUser?.uid
    private var pdf: ByteArray? = null
    private var operation: Job? = null
    var count = 5
    var consent = false
    private val mutable = MutableStateFlow(QuizState())
    val state = mutable.asStateFlow()
    private val listener = FirebaseAuth.AuthStateListener {
        if (it.currentUser?.uid != uid) {
            operation?.cancel(); uid = it.currentUser?.uid; pdf = null; consent = false
            mutable.value = QuizState(); load()
        }
    }
    init { auth.addAuthStateListener(listener); load() }
    private fun load() = runOperation { user ->
        val saved = withContext(Dispatchers.IO) { QuizStore(getApplication(), user).load() }
        mutable.value = mutable.value.copy(saved = saved)
    }
    private fun runOperation(block: suspend (String) -> Unit) {
        if (mutable.value.busy) return
        val user = uid ?: run { mutable.value = mutable.value.copy(error = R.string.quiz_sign_in); return }
        mutable.value = mutable.value.copy(busy = true, error = null)
        operation = viewModelScope.launch {
            try { block(user) }
            catch (e: CancellationException) { throw e }
            catch (e: QuizFailure) { if (uid == user) mutable.value = mutable.value.copy(error = e.messageId) }
            catch (_: Exception) { if (uid == user) mutable.value = mutable.value.copy(error = R.string.quiz_failed) }
            finally { if (uid == user) mutable.value = mutable.value.copy(busy = false) }
        }
    }
    fun select(uri: Uri) = runOperation {
        pdf = null; mutable.value = mutable.value.copy(selected = false)
        val bytes = withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                val out = ByteArrayOutputStream(); val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer); if (n < 0) break
                    if (out.size() + n > 5 * 1024 * 1024) throw QuizFailure(R.string.quiz_pdf_invalid)
                    out.write(buffer, 0, n)
                }
                out.toByteArray()
            } ?: throw QuizFailure(R.string.quiz_pdf_invalid)
        }
        if (bytes.size < 8 || bytes.take(5).toByteArray().toString(Charsets.US_ASCII) != "%PDF-") throw QuizFailure(R.string.quiz_pdf_invalid)
        pdf = bytes; mutable.value = mutable.value.copy(selected = true)
    }
    fun generate(language: String) = runOperation { user ->
        val bytes = pdf ?: throw QuizFailure(R.string.quiz_pdf_invalid)
        if (!consent) throw QuizFailure(R.string.quiz_consent_needed)
        if (mutable.value.saved.size >= 20) throw QuizFailure(R.string.quiz_full)
        if (!com.studyhub.app.util.ApiEndpointPolicy.allowed(BuildConfig.API_BASE_URL, BuildConfig.DEBUG)) throw QuizFailure(R.string.quiz_setup)
        val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: throw QuizFailure(R.string.quiz_sign_in)
        if (uid != user) throw CancellationException()
        val requestedCount = count
        val quiz = withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder().callTimeout(110, TimeUnit.SECONDS).readTimeout(100, TimeUnit.SECONDS).build()
            val lang = if (language in listOf("en", "af", "zu")) language else "en"
            val request = Request.Builder().url(BuildConfig.API_BASE_URL.trimEnd('/') + "/api/quizzes/generate?count=$requestedCount&lang=$lang")
                .header("Authorization", "Bearer $token").post(bytes.toRequestBody("application/pdf".toMediaType())).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw QuizFailure(when (response.code) {
                    401 -> R.string.quiz_sign_in; 413, 400 -> R.string.quiz_pdf_invalid
                    429 -> R.string.quiz_limit; 503 -> R.string.quiz_setup; else -> R.string.quiz_failed
                })
                val json = JSONObject(response.body?.string() ?: throw QuizFailure(R.string.quiz_failed))
                json.put("id", UUID.randomUUID().toString())
                QuizStore.decode(json).checked().also { if (it.questions.size != requestedCount) throw QuizFailure(R.string.quiz_failed) }
            }
        }
        ensureActiveAccount(user)
        persist(user, listOf(quiz) + mutable.value.saved, quiz)
        pdf = null; consent = false; mutable.value = mutable.value.copy(selected = false)
    }
    private fun ensureActiveAccount(user: String) { if (uid != user || auth.currentUser?.uid != user) throw CancellationException() }
    private suspend fun persist(user: String, saved: List<PaperQuiz>, active: PaperQuiz?) {
        ensureActiveAccount(user)
        withContext(Dispatchers.IO) { QuizStore(getApplication(), user).save(saved) }
        ensureActiveAccount(user)
        mutable.value = mutable.value.copy(saved = saved, active = active)
    }
    fun open(quiz: PaperQuiz) { if (!mutable.value.busy) mutable.value = mutable.value.copy(active = quiz, error = null) }
    fun closeQuiz() { if (!mutable.value.busy) mutable.value = mutable.value.copy(active = null) }
    fun answer(index: Int) = runOperation { user ->
        val q = mutable.value.active ?: return@runOperation
        if (q.complete || index !in 0..3) return@runOperation
        val updated = q.copy(answers = q.answers + index)
        persist(user, mutable.value.saved.map { if (it.id == q.id) updated else it }, updated)
    }
    fun retry() = runOperation { user ->
        val q = mutable.value.active ?: return@runOperation
        if (!q.complete || q.score == q.questions.size) return@runOperation
        if (mutable.value.saved.size >= 20) throw QuizFailure(R.string.quiz_full)
        val retry = q.retryMistakes(UUID.randomUUID().toString()).checked()
        persist(user, listOf(retry) + mutable.value.saved, retry)
    }
    fun delete(id: String) = runOperation { user -> persist(user, mutable.value.saved.filterNot { it.id == id }, null) }
    override fun onCleared() { auth.removeAuthStateListener(listener); super.onCleared() }
    private class QuizFailure(val messageId: Int) : Exception()
}
