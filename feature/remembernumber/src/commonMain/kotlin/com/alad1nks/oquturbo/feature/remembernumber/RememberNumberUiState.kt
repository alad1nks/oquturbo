package com.alad1nks.oquturbo.feature.remembernumber

internal sealed interface RememberNumberUiState {
    val text: String
    val score: String

    data class Initial(
        override val text: String = "",
        override val score: String = "0",
    ) : RememberNumberUiState

    data class Reading(
        override val text: String,
        override val score: String,
    ) : RememberNumberUiState

    data class Writing(
        override val text: String,
        override val score: String,
    ) : RememberNumberUiState

    data class Mistake(
        override val text: String,
        override val score: String,
        val correctText: String,
    ) : RememberNumberUiState
}
