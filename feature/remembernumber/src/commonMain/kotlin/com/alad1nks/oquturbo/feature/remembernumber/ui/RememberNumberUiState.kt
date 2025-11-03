package com.alad1nks.oquturbo.feature.remembernumber.ui

internal sealed interface RememberNumberUiState {
    val text: String
    val score: Int

    data class Initial(
        override val text: String = "",
        override val score: Int = 0,
    ) : RememberNumberUiState

    data class Reading(
        override val text: String,
        override val score: Int,
    ) : RememberNumberUiState

    data class Writing(
        override val text: String,
        override val score: Int,
    ) : RememberNumberUiState

    data class Mistake(
        override val text: String,
        override val score: Int,
        val correctText: String,
        val record: Int,
    ) : RememberNumberUiState
}
