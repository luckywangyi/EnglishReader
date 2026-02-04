package com.englishreader.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val FONT_SIZE = intPreferencesKey("font_size")
        private val AUTO_REFRESH = booleanPreferencesKey("auto_refresh")
        private val READING_FONT_SIZE = intPreferencesKey("reading_font_size")
    }
    
    val geminiApiKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[GEMINI_API_KEY] ?: ""
    }
    
    val darkMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DARK_MODE] ?: false
    }
    
    val fontSize: Flow<Int> = dataStore.data.map { preferences ->
        preferences[FONT_SIZE] ?: 16
    }
    
    val readingFontSize: Flow<Int> = dataStore.data.map { preferences ->
        preferences[READING_FONT_SIZE] ?: 18
    }
    
    val autoRefresh: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AUTO_REFRESH] ?: true
    }
    
    suspend fun getApiKey(): String {
        return dataStore.data.first()[GEMINI_API_KEY] ?: ""
    }
    
    suspend fun saveApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[GEMINI_API_KEY] = apiKey
        }
    }
    
    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE] = enabled
        }
    }
    
    suspend fun setFontSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[FONT_SIZE] = size
        }
    }
    
    suspend fun setReadingFontSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[READING_FONT_SIZE] = size
        }
    }
    
    suspend fun setAutoRefresh(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_REFRESH] = enabled
        }
    }
    
    fun hasApiKey(): Flow<Boolean> = geminiApiKey.map { it.isNotEmpty() }
}
