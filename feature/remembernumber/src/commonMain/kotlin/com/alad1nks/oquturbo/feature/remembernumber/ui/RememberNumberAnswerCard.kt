package com.alad1nks.oquturbo.feature.remembernumber.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun RememberNumberAnswerCard(
    comparison: RememberNumberAnswerComparison,
    submittedLabel: String,
    submittedDescription: String,
    correctLabel: String,
    correctDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics { isTraversalGroup = true },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RememberNumberAnswerGroup(
                label = submittedLabel,
                description = submittedDescription,
                answer = comparison.submitted,
                traversalIndex = 0f,
            )
            RememberNumberAnswerGroup(
                label = correctLabel,
                description = correctDescription,
                answer = comparison.correct,
                traversalIndex = 1f,
            )
        }
    }
}

@Composable
private fun RememberNumberAnswerGroup(
    label: String,
    description: String,
    answer: RememberNumberAnswerGroup,
    traversalIndex: Float,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    this.traversalIndex = traversalIndex
                }
                .clearAndSetSemantics {
                    contentDescription = description
                },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val itemsPerRow = minOf(answer.digits.size.coerceAtLeast(1), MAX_TILES_PER_ROW)
            val tileSize =
                minOf(
                    (maxWidth - TILE_SPACING * (itemsPerRow - 1)) / itemsPerRow,
                    MAX_TILE_SIZE,
                )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        space = TILE_SPACING,
                        alignment = Alignment.CenterHorizontally,
                    ),
                verticalArrangement = Arrangement.spacedBy(TILE_SPACING),
                maxItemsInEachRow = MAX_TILES_PER_ROW,
            ) {
                answer.digits.forEach { digit ->
                    val containerColor: Color
                    val borderColor: Color
                    if (digit.isMatching) {
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        borderColor = MaterialTheme.colorScheme.tertiary
                    } else {
                        containerColor = MaterialTheme.colorScheme.errorContainer
                        borderColor = MaterialTheme.colorScheme.error
                    }

                    Surface(
                        modifier =
                            Modifier
                                .size(tileSize)
                                .border(
                                    width = 2.dp,
                                    color = borderColor,
                                    shape = TILE_SHAPE,
                                ),
                        shape = TILE_SHAPE,
                        color = containerColor,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = digit.value.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val TILE_SHAPE = RoundedCornerShape(12.dp)
private val TILE_SPACING = 6.dp
private val MAX_TILE_SIZE = 48.dp
private const val MAX_TILES_PER_ROW = 5
