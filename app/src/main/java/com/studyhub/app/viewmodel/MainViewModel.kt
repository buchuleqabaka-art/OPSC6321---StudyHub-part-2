package com.studyhub.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studyhub.app.R
import com.studyhub.app.data.local.SettingsStore
import com.studyhub.app.data.local.StudyHubDatabase
import com.studyhub.app.data.model.ChatMessage
import com.studyhub.app.data.model.QuoteDto
import com.studyhub.app.data.model.StudyGroup
import com.studyhub.app.data.model.StudySession
import com.studyhub.app.data.model.Task
import com.studyhub.app.data.model.UserProfile
import com.studyhub.app.data.repository.ApiRepository
import com.studyhub.app.data.repository.StudyHubRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class MainUiState(
    val profile: UserProfile? = null,
    val groups: List<StudyGroup> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val sessions: List<StudySession> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val chatComposers: Map<String, ChatComposer> = emptyMap(),
    val chatLoadFailed: Boolean = false,
    val quote: QuoteDto? = null,
    val planningGroupId: String? = null,
    val activeGroupId: String? = null,
    val pendingTaskSyncCount: Int = 0,
    val loading: Boolean = true,
    val error: String? = null
) {
    val upcomingTasks: List<Task> get() = tasks.filter { !it.completed }.take(3)
    val pendingTasks: List<Task> get() = tasks.filter { !it.completed }
    val completedTasks: List<Task> get() = tasks.filter { it.completed }
    val nextSession: StudySession? get() = sessions.firstOrNull { it.startTime >= System.currentTimeMillis() }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StudyHubDatabase.getDatabase(application)
    private val repo = StudyHubRepository(localDao = database.dao(), appContext = application)
    private val settingsStore = SettingsStore(application)
    private val apiRepo = ApiRepository()

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private var chatJob: Job? = null
    private val chatSendJobs = mutableListOf<Job>()
    private var chatUid = FirebaseAuth.getInstance().currentUser?.uid
    private var chatGeneration = 0L
    private val chatAuthListener = FirebaseAuth.AuthStateListener { auth ->
        if (auth.currentUser?.uid != chatUid) {
            chatUid = auth.currentUser?.uid
            chatGeneration++
            chatJob?.cancel()
            chatSendJobs.forEach { it.cancel() }
            chatSendJobs.clear()
            _state.update {
                it.copy(planningGroupId = null, activeGroupId = null, messages = emptyList(), chatComposers = emptyMap(), chatLoadFailed = false)
            }
        }
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(chatAuthListener)
        start()
    }

    override fun onCleared() {
        FirebaseAuth.getInstance().removeAuthStateListener(chatAuthListener)
        super.onCleared()
    }

    private fun start() {
        viewModelScope.launch {
            repo.observeProfile().collect { profile ->
                _state.update { it.copy(profile = profile, loading = false) }
            }
        }
        viewModelScope.launch {
            repo.observeMyGroups().catch { _state.update { it.copy(error = getApplication<Application>().getString(R.string.planning_load_failed)) }; emit(emptyList()) }.collect { groups -> _state.update { it.copy(groups = groups) } }
        }
        viewModelScope.launch {
            repo.observeTasks().catch { _state.update { it.copy(error = getApplication<Application>().getString(R.string.planning_load_failed)) }; emit(emptyList()) }.collect { tasks -> _state.update { it.copy(tasks = tasks) } }
        }
        viewModelScope.launch {
            repo.observePendingTaskCount().collect { count ->
                _state.update { it.copy(pendingTaskSyncCount = count) }
            }
        }
        viewModelScope.launch {
            settingsStore.offlineSync.collect { enabled ->
                if (enabled) {
                    FirebaseAuth.getInstance().currentUser?.uid?.let(repo::enqueuePendingTaskSync)
                }
            }
        }
        viewModelScope.launch {
            settingsStore.language.collect { language -> loadQuote(language) }
        }
        viewModelScope.launch {
            repo.observeSessions().catch { _state.update { it.copy(error = getApplication<Application>().getString(R.string.planning_load_failed)) }; emit(emptyList()) }.collect { s -> _state.update { it.copy(sessions = s) } }
        }
    }

    fun updateProfile(updates: Map<String, Any>) = viewModelScope.launch {
        repo.updateProfile(updates).onFailure { e ->
            _state.update { it.copy(error = e.localizedMessage) }
        }
    }

    // Pulls the daily quote from our hosted REST API
    fun loadQuote(languageTag: String = "en") = viewModelScope.launch {
        _state.update { it.copy(quote = null) }
        apiRepo.dailyQuote(languageTag).onSuccess { quote ->
            _state.update { it.copy(quote = quote) }
        }
    }

    fun openGroupChat(groupId: String) {
        if (groupId.isBlank()) return
        chatJob?.cancel()
        val generation = chatGeneration
        _state.update { it.copy(activeGroupId = groupId, messages = emptyList(), chatLoadFailed = false) }
        chatJob = viewModelScope.launch {
            repo.observeMessages(groupId)
                .catch {
                    if (generation == chatGeneration && _state.value.activeGroupId == groupId) {
                        _state.update { it.copy(chatLoadFailed = true) }
                    }
                }
                .collect { msgs ->
                    if (generation == chatGeneration && _state.value.activeGroupId == groupId) {
                        _state.update { it.copy(messages = msgs) }
                    }
                }
        }
    }

    fun editChatDraft(text: String) {
        val groupId = _state.value.activeGroupId ?: return
        _state.update { state ->
            val composer = state.chatComposers[groupId] ?: ChatComposer()
            state.copy(chatComposers = state.chatComposers + (groupId to composer.edit(text)))
        }
    }

    fun sendMessage() {
        val groupId = _state.value.activeGroupId ?: return
        val current = _state.value.chatComposers[groupId] ?: return
        if (current.sending || current.text.isBlank() || chatUid == null) return
        val attempt = current.begin(UUID.randomUUID().toString())
        val messageId = attempt.messageId ?: return
        val generation = chatGeneration
        val studentFallback = getApplication<Application>().getString(R.string.student)
        val name = _state.value.profile?.name?.ifBlank { studentFallback } ?: studentFallback
        _state.update { it.copy(chatComposers = it.chatComposers + (groupId to attempt)) }
        chatSendJobs.removeAll { it.isCompleted }
        chatSendJobs += viewModelScope.launch {
            val result = repo.sendMessage(groupId, name, attempt.text, messageId)
            if (generation != chatGeneration) return@launch
            _state.update { state ->
                val composer = state.chatComposers[groupId] ?: return@update state
                state.copy(chatComposers = state.chatComposers + (groupId to composer.finish(messageId, result.isSuccess)))
            }
            if (result.isFailure) return@launch
            val idToken = runCatching {
                FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
            }.getOrNull()
            if (generation == chatGeneration && !idToken.isNullOrBlank()) {
                // Push failure does not change the successfully sent message.
                apiRepo.notifyGroupMessage(idToken, groupId, messageId)
            }
        }
    }

    fun selectPlanningGroup(groupId: String?) {
        _state.update { it.copy(planningGroupId = groupId) }
    }

    fun joinGroup(groupId: String, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val result = repo.joinGroup(groupId.trim())
        onResult(result.isSuccess)
    }

    fun createGroup(name: String, subject: String, description: String, onResult: (Boolean) -> Unit = {}) = viewModelScope.launch {
        repo.createGroup(name, subject, description)
            .also { onResult(it.isSuccess) }
            .onFailure { e -> _state.update { it.copy(error = e.localizedMessage) } }
    }

    fun addTask(task: Task, onResult: (Boolean) -> Unit = {}) = viewModelScope.launch {
        repo.addTask(task)
            .also { onResult(it.isSuccess) }
            .onFailure { e -> _state.update { it.copy(error = e.localizedMessage) } }
    }

    fun updateProfile(name: String, course: String, email: String) = viewModelScope.launch {
        repo.updateProfile(
            mapOf(
                "name" to name,
                "course" to course,
                "email" to email
            )
        ).onFailure { e -> _state.update { it.copy(error = e.localizedMessage) } }
    }
    fun toggleTask(task: Task) = viewModelScope.launch {
        repo.setTaskCompleted(task).onFailure { _state.update { state -> state.copy(error = getApplication<Application>().getString(R.string.planning_save_failed)) } }
    }

    fun deleteTask(taskId: String) = viewModelScope.launch { repo.deleteTask(taskId) }

    fun addSession(session: StudySession, onResult: (Boolean) -> Unit = {}) = viewModelScope.launch {
        repo.addSession(session).also { onResult(it.isSuccess) }.onFailure { e -> _state.update { it.copy(error = e.localizedMessage) } }
    }

    fun toggleAttendance(sessionId: String) = viewModelScope.launch {
        repo.toggleAttendance(sessionId)
    }

    fun clearError() = _state.update { it.copy(error = null) }

    fun uploadProfilePhoto(uri: Uri, onResult: (Result<String>) -> Unit) = viewModelScope.launch {
        val result = repo.uploadProfilePhoto(uri)
        result.onSuccess { url ->
            _state.update { state -> state.copy(profile = state.profile?.copy(photoUrl = url)) }
        }.onFailure { error ->
            _state.update { it.copy(error = error.localizedMessage) }
        }
        onResult(result)
    }
}