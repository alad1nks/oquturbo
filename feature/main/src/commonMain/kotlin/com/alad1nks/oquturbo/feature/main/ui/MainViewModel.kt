package com.alad1nks.oquturbo.feature.main.ui

import androidx.lifecycle.ViewModel
import com.alad1nks.oquturbo.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.map

internal class MainViewModel(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val darkTheme = settingsRepository.getDarkTheme().map { it == true }
}
