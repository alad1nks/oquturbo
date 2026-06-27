package com.alad1nks.oquturbo.feature.remembernumbermenu.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class RememberNumberMenuViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<RememberNumberMenuUiState> =
        MutableStateFlow(RememberNumberMenuUiState.Default)
    val uiState: StateFlow<RememberNumberMenuUiState> = _uiState.asStateFlow()

    fun openItemCustomOptionsDialog() {
        _uiState.value = RememberNumberMenuUiState.CustomOptionsDialog()
    }

    fun closeItemCustomOptionsDialog() {
        _uiState.value = RememberNumberMenuUiState.Default
    }

    fun changeCustomSettingsLength(value: Int) {
        val uiStateValue = _uiState.value
        if (uiStateValue is RememberNumberMenuUiState.CustomOptionsDialog) {
            _uiState.value = uiStateValue.copy(maxLength = value)
        }
    }

    fun selectCustomSettingsDigit(index: Int, isAvailable: Boolean) {
        val uiStateValue = _uiState.value
        if (uiStateValue is RememberNumberMenuUiState.CustomOptionsDialog) {
            val digitsAvailability =
                uiStateValue.digitsAvailability.toMutableList().apply {
                    this[index] = isAvailable
                }
            _uiState.value = uiStateValue.copy(digitsAvailability = digitsAvailability)
        }
    }
}
