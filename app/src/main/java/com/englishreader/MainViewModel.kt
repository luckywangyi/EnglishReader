package com.englishreader

import androidx.lifecycle.ViewModel
import com.englishreader.data.repository.SettingsRepository
import com.englishreader.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {

    val themeMode: Flow<ThemeMode> = settingsRepository.darkMode.map { isDark ->
        if (isDark) ThemeMode.DARK else ThemeMode.LIGHT
    }
}
