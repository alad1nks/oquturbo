package com.alad1nks.oquturbo.feature.remembernumbermenu.ui

internal data class RememberNumberMenuUiState(
    val customOptionsDialog: CustomOptionsDialog = CustomOptionsDialog(),
    val themeIcon: ThemeIcon = ThemeIcon(),
) {
    data class CustomOptionsDialog(
        val maxLength: Int = 4,
        val digitsAvailability: List<Boolean> = List(10) { true },
        val show: Boolean = false,
    )

    data class ThemeIcon(
        val darkTheme: Boolean = false,
        val show: Boolean = false,
    )
}
