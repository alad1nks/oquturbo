package com.alad1nks.oquturbo.feature.numbertrail.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailFailure
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailState
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

private enum class ReviewRole { Completed, Expected, Wrong, Remaining }

@Composable
internal fun NumberTrailBoardReview(game: NumberTrailState) {
    val board = game.board ?: return
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(AppResource.String.number_trail_review_title),
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(AppResource.String.number_trail_grid_size, board.size),
            Modifier.padding(top = 8.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val style =
                if (board.size >= 5 && maxWidth < 320.dp) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleLarge
                }.copy(fontWeight = FontWeight.Bold)
            val measure = rememberTextMeasurer()
            val density = LocalDensity.current
            val textWidth = with(density) { measure.measure("25", style).size.width.toDp() }
            val minimumWidth = (textWidth + 8.dp) * board.size + 8.dp * (board.size - 1)
            val boardWidth = maxWidth.coerceAtMost(400.dp).coerceAtLeast(minimumWidth)
            val scroll = rememberScrollState()
            Box(if (boardWidth > maxWidth) Modifier.horizontalScroll(scroll) else Modifier) {
                Column(Modifier.width(boardWidth), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    board.numbers.chunked(board.size).forEach { numbers ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            numbers.forEach { number ->
                                val role =
                                    when {
                                        number < board.target -> ReviewRole.Completed
                                        number == board.target -> ReviewRole.Expected
                                        game.failure == NumberTrailFailure.Wrong && number == game.selectedNumber ->
                                            ReviewRole.Wrong
                                        else -> ReviewRole.Remaining
                                    }
                                val colors = MaterialTheme.colorScheme
                                val background =
                                    when (role) {
                                        ReviewRole.Completed -> colors.primaryContainer
                                        ReviewRole.Expected -> colors.tertiaryContainer
                                        ReviewRole.Wrong -> colors.errorContainer
                                        ReviewRole.Remaining -> colors.surface
                                    }
                                val foreground =
                                    when (role) {
                                        ReviewRole.Completed -> colors.onPrimaryContainer
                                        ReviewRole.Expected -> colors.onTertiaryContainer
                                        ReviewRole.Wrong -> colors.onErrorContainer
                                        ReviewRole.Remaining -> colors.onSurface
                                    }
                                val outline =
                                    when (role) {
                                        ReviewRole.Completed -> colors.primary
                                        ReviewRole.Expected -> colors.tertiary
                                        ReviewRole.Wrong -> colors.error
                                        ReviewRole.Remaining -> colors.outlineVariant
                                    }
                                val description =
                                    stringResource(
                                        when (role) {
                                            ReviewRole.Completed ->
                                                AppResource.String.number_trail_review_cell_completed
                                            ReviewRole.Expected -> AppResource.String.number_trail_review_cell_expected
                                            ReviewRole.Wrong -> AppResource.String.number_trail_review_cell_wrong
                                            ReviewRole.Remaining -> AppResource.String.number_trail_tile_description
                                        },
                                        number,
                                    )
                                Surface(
                                    Modifier.weight(1f).clearAndSetSemantics { contentDescription = description },
                                    shape = MaterialTheme.shapes.medium,
                                    color = background,
                                    contentColor = foreground,
                                    border =
                                        BorderStroke(
                                            if (role == ReviewRole.Expected || role == ReviewRole.Wrong) 2.dp else 1.dp,
                                            outline,
                                        ),
                                ) {
                                    Column(
                                        Modifier.heightIn(min = 64.dp).padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Box(
                                            Modifier.height(16.dp),
                                            contentAlignment = Alignment.Center,
                                        ) { ReviewMarker(role) }
                                        Text(number.toString(), style = style)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReviewLegend(ReviewRole.Expected, stringResource(AppResource.String.number_trail_review_legend_expected))
            if (game.failure == NumberTrailFailure.Wrong) {
                ReviewLegend(ReviewRole.Wrong, stringResource(AppResource.String.number_trail_review_legend_wrong))
            }
            ReviewLegend(ReviewRole.Completed, stringResource(AppResource.String.number_trail_review_legend_completed))
        }
    }
}

@Composable
private fun ReviewMarker(role: ReviewRole) {
    when (role) {
        ReviewRole.Expected ->
            Box(
                Modifier.size(12.dp).border(2.dp, androidx.compose.material3.LocalContentColor.current, CircleShape),
            )
        ReviewRole.Wrong -> Icon(Icons.Default.Close, null, Modifier.size(14.dp))
        ReviewRole.Completed -> Icon(Icons.Default.Check, null, Modifier.size(14.dp))
        ReviewRole.Remaining -> Unit
    }
}

@Composable
private fun ReviewLegend(role: ReviewRole, text: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) { ReviewMarker(role) }
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}
