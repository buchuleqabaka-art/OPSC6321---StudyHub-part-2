package com.studyhub.app.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyhub.app.data.local.SettingsStore
import com.studyhub.app.util.AppLanguages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val darkMode: Boolean = false,
    val offlineSync: Boolean = true,
    val dataSaver: Boolean = false,
    val notifications: Boolean = false,
    val language: String = AppLanguages.ENGLISH,
    val loaded: Boolean = false
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SettingsStore(app)
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { store.darkMode.collect { value -> _state.update { it.copy(darkMode = value) } } }
        viewModelScope.launch { store.offlineSync.collect { value -> _state.update { it.copy(offlineSync = value) } } }
        viewModelScope.launch { store.dataSaver.collect { value -> _state.update { it.copy(dataSaver = value) } } }
        viewModelScope.launch { store.notifications.collect { value -> _state.update { it.copy(notifications = value) } } }
        viewModelScope.launch {
            store.language.collect { storedLanguage ->
                val tag = AppLanguages.normalize(storedLanguage)
                _state.update { it.copy(language = tag, loaded = true) }
                applyApplicationLanguage(tag)
            }
        }
    }

    fun setDarkMode(value: Boolean) = viewModelScope.launch { store.setDarkMode(value) }
    fun setOfflineSync(value: Boolean) = viewModelScope.launch { store.setOfflineSync(value) }
    fun setDataSaver(value: Boolean) = viewModelScope.launch { store.setDataSaver(value) }
    fun setNotifications(value: Boolean) = viewModelScope.launch { store.setNotifications(value) }

    fun setLanguage(value: String) = viewModelScope.launch {
        val tag = AppLanguages.normalize(value)
        store.setLanguage(tag)
        applyApplicationLanguage(tag)
    }

    private fun applyApplicationLanguage(tag: String) {
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (current != tag) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }
}
