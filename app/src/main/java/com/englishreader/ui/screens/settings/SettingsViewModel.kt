package com.englishreader.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishreader.data.remote.gemini.GeminiService
import com.englishreader.data.repository.SettingsRepository
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
    private val geminiService: GeminiService
) : ViewModel() {
    
    val apiKey: StateFlow<String> = settingsRepository.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    
    val darkMode: StateFlow<Boolean> = settingsRepository.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val fontSize: StateFlow<Int> = settingsRepository.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16)
    
    val readingFontSize: StateFlow<Int> = settingsRepository.readingFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 18)
    
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()
    
    private val _testState = MutableStateFlow<TestState>(TestState.Idle)
    val testState: StateFlow<TestState> = _testState.asStateFlow()
    
    fun saveApiKey(key: String) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            settingsRepository.saveApiKey(key)
            geminiService.resetModel()
            _saveState.value = SaveState.Saved
            
            // Reset state after delay
            kotlinx.coroutines.delay(2000)
            _saveState.value = SaveState.Idle
        }
    }
    
    fun testApiKey(key: String) {
        viewModelScope.launch {
            _testState.value = TestState.Testing
            
            // Temporarily save the key
            settingsRepository.saveApiKey(key)
            geminiService.resetModel()
            
            // Test with a simple translation
            val result = geminiService.translate("hello")
            
            _testState.value = result.fold(
                onSuccess = { TestState.Success },
                onFailure = { TestState.Error(it.message ?: "Connection failed") }
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
