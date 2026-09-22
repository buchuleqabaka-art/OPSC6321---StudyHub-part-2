package com.studyhub.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "studyhub_settings")

/** Backs the Appearance / Offline / Data-usage rows on the Profile screen. */
class SettingsStore(private val context: Context) {

    companion object {
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val OFFLINE_SYNC = booleanPreferencesKey("offline_sync")
        private val DATA_SAVER = booleanPreferencesKey("data_saver")
        private val NOTIFICATIONS = booleanPreferencesKey("notifications")
        private val LANGUAGE = stringPreferencesKey("language")
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE] ?: false }
    val offlineSync: Flow<Boolean> = context.dataStore.data.map { it[OFFLINE_SYNC] ?: true }
    val dataSaver: Flow<Boolean> = context.dataStore.data.map { it[DATA_SAVER] ?: false }
    val notifications: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS] ?: true }
    val language: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "English" }

    suspend fun setDarkMode(value: Boolean) = context.dataStore.edit { it[DARK_MODE] = value }
    suspend fun setOfflineSync(value: Boolean) = context.dataStore.edit { it[OFFLINE_SYNC] = value }
    suspend fun setDataSaver(value: Boolean) = context.dataStore.edit { it[DATA_SAVER] = value }
    suspend fun setNotifications(value: Boolean) = context.dataStore.edit { it[NOTIFICATIONS] = value }
    suspend fun setLanguage(value: String) = context.dataStore.edit { it[LANGUAGE] = value }
}