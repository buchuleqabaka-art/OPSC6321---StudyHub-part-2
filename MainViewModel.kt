package com.studyhub.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

data class MainUiState(
    val profile: UserProfile? = null,
    val groups: List<StudyGroup> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val sessions: List<StudySession> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val quote: QuoteDto? = null,
    val activeGroupId: String? = null,
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
    private val repo = StudyHubRepository(localDao = database.dao())
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
            repo.observeSessions().collect { s -> _state.update { it.copy(sessions = s) } }
        }
        loadQuote()
    }

    fun updateProfile(updates: Map<String, Any>) = viewModelScope.launch {
        repo.updateProfile(updates).onFailure { e ->
            _state.update { it.copy(error = e.localizedMessage) }
        }
    }

    // Pulls the daily quote from our hosted REST API
    fun loadQuote() = viewModelScope.launch {
        apiRepo.dailyQuote().onSuccess { q -> _state.update { it.copy(quote = q) } }
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
        val name = _state.value.profile?.name ?: "Student"
        if (text.isBlank()) return
        viewModelScope.launch { repo.sendMessage(groupId, name, text) }
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
        repo.setTaskCompleted(task.id, !task.completed)
    }

    fun deleteTask(taskId: String) = viewModelScope.launch { repo.deleteTask(taskId) }

    fun addSession(session: StudySession) = viewModelScope.launch {
        repo.addSession(session).onFailure { e -> _state.update { it.copy(error = e.localizedMessage) } }
    }

    fun toggleAttendance(sessionId: String) = viewModelScope.launch {
        repo.toggleAttendance(sessionId)
    }

    fun clearError() = _state.update { it.copy(error = null) }

    fun updatePhoto(base64: String) = viewModelScope.launch {
        repo.updateProfile(mapOf("photoUrl" to base64))
            .onFailure { e -> _state.update { it.copy(error = e.localizedMessage) } }
    }
}