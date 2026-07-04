package com.alad1nks.oquturbo.feature.remembernumbermenu.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class RememberNumberMenuViewModel(
    showThemeIcon: Boolean,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState: MutableStateFlow<RememberNumberMenuUiState> =
        MutableStateFlow(
            value =
                RememberNumberMenuUiState(
                    themeIcon = RememberNumberMenuUiState.ThemeIcon(show = showThemeIcon),
                ),
        )
    val uiState: StateFlow<RememberNumberMenuUiState> = _uiState.asStateFlow()

    init {
        if (showThemeIcon) {
            viewModelScope.launch {
                settingsRepository.getDarkTheme()
                    .collect { darkTheme ->
                        _uiState.update {
                            it.copy(
                                themeIcon = it.themeIcon.copy(darkTheme = darkTheme == true),
                            )
                        }
                    }
            }
        }
    }

    fun openItemCustomOptionsDialog() {
        _uiState.update {
            it.copy(
                customOptionsDialog = it.customOptionsDialog.copy(show = true),
            )
        }
    }

    fun closeItemCustomOptionsDialog() {
        _uiState.update {
            it.copy(
                customOptionsDialog = it.customOptionsDialog.copy(show = false),
            )
        }
    }

    fun changeCustomSettingsLength(value: Int) {
        _uiState.update {
            it.copy(
                customOptionsDialog = it.customOptionsDialog.copy(maxLength = value),
            )
        }
    }

    fun selectCustomSettingsDigit(index: Int, isAvailable: Boolean) {
        _uiState.update {
            val digitsAvailability =
                it.customOptionsDialog.digitsAvailability.toMutableList().apply {
                    this[index] = isAvailable
                }

            it.copy(
                customOptionsDialog = it.customOptionsDialog.copy(digitsAvailability = digitsAvailability),
            )
        }
    }

    fun changeDarkTheme(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(value)
        }
    }
}
