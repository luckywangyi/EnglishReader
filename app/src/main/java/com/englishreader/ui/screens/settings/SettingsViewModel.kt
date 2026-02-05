package com.englishreader.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishreader.data.remote.ai.AiService
import com.englishreader.data.repository.SettingsRepository
import com.englishreader.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val aiService: AiService,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    
    val apiKey: StateFlow<String> = settingsRepository.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    
    val darkMode: StateFlow<Boolean> = settingsRepository.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val fontSize: StateFlow<Int> = settingsRepository.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16)
    
    val readingFontSize: StateFlow<Int> = settingsRepository.readingFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 18)
    
    val reminderEnabled: StateFlow<Boolean> = settingsRepository.reminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val morningReminderHour: StateFlow<Int> = settingsRepository.morningReminderHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)
    
    val eveningReminderHour: StateFlow<Int> = settingsRepository.eveningReminderHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 21)
    
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()
    
    private val _testState = MutableStateFlow<TestState>(TestState.Idle)
    val testState: StateFlow<TestState> = _testState.asStateFlow()
    
    fun saveApiKey(key: String) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            settingsRepository.saveApiKey(key)
            aiService.resetModel()
            _saveState.value = SaveState.Saved
            
            // Reset state after delay
            kotlinx.coroutines.delay(2000)
            _saveState.value = SaveState.Idle
        }
    }
    
    fun testApiKey(key: String) {
        viewModelScope.launch {
            _testState.value = TestState.Testing
            
            // 直接使用传入的 key 测试，不保存
            val result = aiService.testConnectionWithKey(key)
            
            _testState.value = result.fold(
                onSuccess = { TestState.Success },
                onFailure = { TestState.Error(it.message ?: "连接失败") }
            )
            
            // Reset state after delay
            kotlinx.coroutines.delay(3000)
            _testState.value = TestState.Idle
        }
    }
    
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(enabled)
        }
    }
    
    fun setFontSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.setFontSize(size)
        }
    }
    
    fun setReadingFontSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.setReadingFontSize(size)
        }
    }
    
    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(enabled)
            if (enabled) {
                val morningHour = settingsRepository.getMorningReminderHour()
                val eveningHour = settingsRepository.getEveningReminderHour()
                reminderScheduler.scheduleReminders(morningHour, eveningHour)
            } else {
                reminderScheduler.cancelAllReminders()
            }
        }
    }
    
    fun setMorningReminderHour(hour: Int) {
        viewModelScope.launch {
            settingsRepository.setMorningReminderHour(hour)
            if (settingsRepository.isReminderEnabled()) {
                val eveningHour = settingsRepository.getEveningReminderHour()
                reminderScheduler.scheduleReminders(hour, eveningHour)
            }
        }
    }
    
    fun setEveningReminderHour(hour: Int) {
        viewModelScope.launch {
            settingsRepository.setEveningReminderHour(hour)
            if (settingsRepository.isReminderEnabled()) {
                val morningHour = settingsRepository.getMorningReminderHour()
                reminderScheduler.scheduleReminders(morningHour, hour)
            }
        }
    }
}

sealed class SaveState {
    data object Idle : SaveState()
    data object Saving : SaveState()
    data object Saved : SaveState()
}

sealed class TestState {
    data object Idle : TestState()
    data object Testing : TestState()
    data object Success : TestState()
    data class Error(val message: String) : TestState()
}
