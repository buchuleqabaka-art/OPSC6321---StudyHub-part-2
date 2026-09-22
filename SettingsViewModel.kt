package com.studyhub.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyhub.app.data.local.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val darkMode: Boolean = false,
    val offlineSync: Boolean = true,
    val dataSaver: Boolean = false,
    val notifications: Boolean = true,
    val language: String = "English"
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SettingsStore(app)
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { store.darkMode.collect { v -> _state.update { it.copy(darkMode = v) } } }
        viewModelScope.launch { store.offlineSync.collect { v -> _state.update { it.copy(offlineSync = v) } } }
        viewModelScope.launch { store.dataSaver.collect { v -> _state.update { it.copy(dataSaver = v) } } }
        viewModelScope.launch { store.notifications.collect { v -> _state.update { it.copy(notifications = v) } } }
        viewModelScope.launch { store.language.collect { v -> _state.update { it.copy(language = v) } } }
    }

    fun setDarkMode(value: Boolean) = viewModelScope.launch { store.setDarkMode(value) }
    fun setOfflineSync(value: Boolean) = viewModelScope.launch { store.setOfflineSync(value) }
    fun setDataSaver(value: Boolean) = viewModelScope.launch { store.setDataSaver(value) }
    fun setNotifications(value: Boolean) = viewModelScope.launch { store.setNotifications(value) }
    fun setLanguage(value: String) = viewModelScope.launch { store.setLanguage(value) }
}