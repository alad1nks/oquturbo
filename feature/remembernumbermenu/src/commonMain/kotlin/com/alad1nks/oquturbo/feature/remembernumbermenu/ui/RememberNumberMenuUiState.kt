package com.alad1nks.oquturbo.feature.remembernumbermenu.ui

internal interface RememberNumberMenuUiState {
    data object Default : RememberNumberMenuUiState

    data class CustomOptionsDialog(
        val maxLength: Int = 4,
        val digitsAvailability: List<Boolean> = List(10) { true },
    ) : RememberNumberMenuUiState
}
