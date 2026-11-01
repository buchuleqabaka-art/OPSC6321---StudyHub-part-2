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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class MainUiState(
    val profile: UserProfile? = null,
    val groups: List<StudyGroup> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val sessions: List<StudySession> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val quote: QuoteDto? = null,
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

    init { start() }

    private fun start() {
        viewModelScope.launch {
            repo.observeProfile().collect { profile ->
                _state.update { it.copy(profile = profile, loading = false) }
            }
        }
        viewModelScope.launch {
            repo.observeMyGroups().collect { groups -> _state.update { it.copy(groups = groups) } }
        }
        viewModelScope.launch {
            repo.observeTasks().collect { tasks -> _state.update { it.copy(tasks = tasks) } }
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
            repo.observeSessions().collect { s -> _state.update { it.copy(sessions = s) } }
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
        _state.update { it.copy(activeGroupId = groupId, messages = emptyList()) }
        viewModelScope.launch {
            repo.observeMessages(groupId).collect { msgs ->
                _state.update { it.copy(messages = msgs) }
            }
        }
    }

    fun sendMessage(text: String) {
        val groupId = _state.value.activeGroupId ?: return
        val studentFallback = getApplication<Application>().getString(R.string.student)
        val name = _state.value.profile?.name?.ifBlank { studentFallback } ?: studentFallback
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        viewModelScope.launch {
            val messageId = repo.sendMessage(groupId, name, cleanText).getOrNull() ?: return@launch
            val idToken = runCatching {
                FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
            }.getOrNull()
            if (!idToken.isNullOrBlank()) {
                // Push is best-effort; the message remains sent if the API is not deployed.
                apiRepo.notifyGroupMessage(idToken, groupId, messageId)
            }
        }
    }

    fun createGroup(name: String, subject: String, description: String) = viewModelScope.launch {
        repo.createGroup(name, subject, description)
            .onFailure { e -> _state.update { it.copy(error = e.localizedMessage) } }
    }

    fun addTask(task: Task) = viewModelScope.launch {
        repo.addTask(task)
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
        repo.setTaskCompleted(task)
    }

    fun deleteTask(taskId: String) = viewModelScope.launch { repo.deleteTask(taskId) }

    fun addSession(session: StudySession) = viewModelScope.launch {
        repo.addSession(session).onFailure { e -> _state.update { it.copy(error = e.localizedMessage) } }
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