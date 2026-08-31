package com.alad1nks.oquturbo.feature.remembernumber.ui

internal data class RememberNumberAnswerComparison(
    val submitted: RememberNumberAnswerGroup,
    val correct: RememberNumberAnswerGroup,
)

internal data class RememberNumberAnswerGroup(
    val digits: List<RememberNumberAnswerDigit>,
) {
    val value: String = digits.joinToString(separator = "") { it.value.toString() }
    val digitSpacedValue: String = digits.joinToString(separator = " ") { it.value.toString() }
}

internal data class RememberNumberAnswerDigit(
    val value: Char,
    val isMatching: Boolean,
)

internal fun RememberNumberUiState.answerComparisonOrNull(): RememberNumberAnswerComparison? {
    val mistake = this as? RememberNumberUiState.Mistake ?: return null

    return RememberNumberAnswerComparison(
        submitted =
            RememberNumberAnswerGroup(
                digits =
                    mistake.text.mapIndexed { index, digit ->
                        RememberNumberAnswerDigit(
                            value = digit,
                            isMatching = digit == mistake.correctText.getOrNull(index),
                        )
                    },
            ),
        correct =
            RememberNumberAnswerGroup(
                digits =
                    mistake.correctText.map { digit ->
                        RememberNumberAnswerDigit(value = digit, isMatching = true)
                    },
            ),
    )
}
