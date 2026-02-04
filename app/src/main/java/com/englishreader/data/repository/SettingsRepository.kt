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
        
        // 今日推荐缓存
        private val TODAY_RECOMMENDED_ID = stringPreferencesKey("today_recommended_id")
        private val TODAY_RECOMMENDED_DATE = stringPreferencesKey("today_recommended_date")
        
        // 阅读提醒设置
        private val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val MORNING_REMINDER_HOUR = intPreferencesKey("morning_reminder_hour")
        private val EVENING_REMINDER_HOUR = intPreferencesKey("evening_reminder_hour")
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
    
    // 今日推荐相关
    suspend fun getTodayRecommendedId(): String? {
        return dataStore.data.first()[TODAY_RECOMMENDED_ID]
    }
    
    suspend fun getTodayRecommendedDate(): String? {
        return dataStore.data.first()[TODAY_RECOMMENDED_DATE]
    }
    
    suspend fun saveTodayRecommendation(articleId: String, date: String) {
        dataStore.edit { preferences ->
            preferences[TODAY_RECOMMENDED_ID] = articleId
            preferences[TODAY_RECOMMENDED_DATE] = date
        }
    }
    
    // 阅读提醒相关
    val reminderEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[REMINDER_ENABLED] ?: false
    }
    
    val morningReminderHour: Flow<Int> = dataStore.data.map { preferences ->
        preferences[MORNING_REMINDER_HOUR] ?: 10
    }
    
    val eveningReminderHour: Flow<Int> = dataStore.data.map { preferences ->
        preferences[EVENING_REMINDER_HOUR] ?: 21
    }
    
    suspend fun isReminderEnabled(): Boolean {
        return dataStore.data.first()[REMINDER_ENABLED] ?: false
    }
    
    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[REMINDER_ENABLED] = enabled
        }
    }
    
    suspend fun getMorningReminderHour(): Int {
        return dataStore.data.first()[MORNING_REMINDER_HOUR] ?: 10
    }
    
    suspend fun getEveningReminderHour(): Int {
        return dataStore.data.first()[EVENING_REMINDER_HOUR] ?: 21
    }
    
    suspend fun setMorningReminderHour(hour: Int) {
        dataStore.edit { preferences ->
            preferences[MORNING_REMINDER_HOUR] = hour
        }
    }
    
    suspend fun setEveningReminderHour(hour: Int) {
        dataStore.edit { preferences ->
            preferences[EVENING_REMINDER_HOUR] = hour
        }
    }
}
