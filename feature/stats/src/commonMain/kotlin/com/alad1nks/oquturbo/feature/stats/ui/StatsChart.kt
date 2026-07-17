package com.alad1nks.oquturbo.feature.stats.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StatsLineChart(
    scores: List<Int>,
    modifier: Modifier = Modifier,
) {
    if (scores.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val pointColor = MaterialTheme.colorScheme.surface
    val chartDescription = stringResource(AppResource.String.stats_chart_content_description)

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(180.dp)
                .semantics { contentDescription = chartDescription },
    ) {
        val horizontalPadding = 12.dp.toPx()
        val verticalPadding = 16.dp.toPx()
        val chartWidth = size.width - horizontalPadding * 2
        val chartHeight = size.height - verticalPadding * 2
        val minValue = scores.minOrNull() ?: 0
        val maxValue = scores.maxOrNull() ?: minValue
        val valueRange = (maxValue - minValue).coerceAtLeast(1)

        repeat(4) { index ->
            val y = verticalPadding + chartHeight * index / 3f
            drawLine(
                color = gridColor,
                start = Offset(horizontalPadding, y),
                end = Offset(size.width - horizontalPadding, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val points =
            scores.mapIndexed { index, value ->
                Offset(
                    x = horizontalPadding + chartWidth * index / (scores.size - 1),
                    y = verticalPadding + chartHeight * (maxValue - value) / valueRange,
                )
            }
        val path =
            Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { point -> lineTo(point.x, point.y) }
            }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        points.forEach { point ->
            drawCircle(color = pointColor, radius = 5.dp.toPx(), center = point)
            drawCircle(color = lineColor, radius = 3.dp.toPx(), center = point)
        }
    }
}

@Composable
internal fun StatsScoreHistory(
    scores: List<Int>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    if (scores.isEmpty()) return

    val chartDescription = stringResource(AppResource.String.stats_chart_content_description)
    val scoreHistoryDescription = listOf(chartDescription, scores.joinToString()).joinToString()
    LazyRow(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = scoreHistoryDescription },
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(scores) { score ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Text(
                    text = score.toString(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
