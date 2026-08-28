package com.alad1nks.oquturbo.feature.kenkozgame.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun AnswerFeedbackCard(
    selectedAnswerLabel: String,
    selectedAnswer: String?,
    correctAnswerLabel: String,
    correctAnswer: String?,
    modifier: Modifier = Modifier,
) {
    if (selectedAnswer == null && correctAnswer == null) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            selectedAnswer?.let { answer ->
                AnswerFeedbackRow(
                    label = selectedAnswerLabel,
                    answer = answer,
                )
            }
            correctAnswer?.let { answer ->
                AnswerFeedbackRow(
                    label = correctAnswerLabel,
                    answer = answer,
                )
            }
        }
    }
}

@Composable
private fun AnswerFeedbackRow(
    label: String,
    answer: String,
) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = answer,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
