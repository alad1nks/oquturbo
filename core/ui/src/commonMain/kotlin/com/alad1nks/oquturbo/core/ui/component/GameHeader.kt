package com.alad1nks.oquturbo.core.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboLayout

@Composable
fun GameHeader(
    scoreLabel: String,
    score: String,
    recordLabel: String,
    record: String,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val measurer = rememberTextMeasurer()
        val width = with(LocalDensity.current) { maxWidth.roundToPx() }
        val gap = with(LocalDensity.current) { OquTurboLayout.textGap.roundToPx() }
        val actionWidth = with(LocalDensity.current) { OquTurboLayout.headerActionSize.roundToPx() }
        val scoreStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
        val recordStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)

        fun textWidth(text: String, style: TextStyle): Int =
            measurer.measure(AnnotatedString(text), style, softWrap = false, maxLines = 1).size.width
        val scoreWidth = maxOf(textWidth(score, scoreStyle), textWidth(scoreLabel, MaterialTheme.typography.labelLarge))
        val recordWidth =
            maxOf(textWidth(record, recordStyle), textWidth(recordLabel, MaterialTheme.typography.labelMedium)) +
                gap + actionWidth
        val fitsRow = actionWidth + scoreWidth + recordWidth + 2 * gap <= width
        if (fitsRow) {
            Layout(
                modifier = Modifier.fillMaxWidth(),
                content = {
                    HeaderLeading(leadingContent)
                    HeaderValue(scoreLabel, score, scoreStyle, true)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OquTurboLayout.textGap),
                    ) {
                        HeaderValue(recordLabel, record, recordStyle, false, Alignment.End)
                        HeaderTrophy()
                    }
                },
            ) { measurables, constraints ->
                val loose = constraints.copy(minWidth = 0, minHeight = 0)
                val leadingPlaceable = measurables[0].measure(loose)
                val scorePlaceable = measurables[1].measure(loose)
                val recordPlaceable = measurables[2].measure(loose)
                val height = maxOf(leadingPlaceable.height, scorePlaceable.height, recordPlaceable.height)
                val fitsCentered =
                    scorePlaceable.width + 2 * (
                        maxOf(
                            leadingPlaceable.width,
                            recordPlaceable.width,
                        ) + gap
                    ) <= width
                layout(width, height) {
                    leadingPlaceable.placeRelative(0, (height - leadingPlaceable.height) / 2)
                    scorePlaceable.placeRelative(
                        if (fitsCentered) (width - scorePlaceable.width) / 2 else leadingPlaceable.width + gap,
                        (height - scorePlaceable.height) / 2,
                    )
                    recordPlaceable.placeRelative(width - recordPlaceable.width, (height - recordPlaceable.height) / 2)
                }
            }
        } else {
            // Give the values the full width before choosing a smaller semantic heading style.
            // Never split digits or suppress the user's font scale to preserve the old row.
            val scoreStyles =
                listOf(
                    scoreStyle,
                    MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                )
            val fittingScoreStyle = scoreStyles.firstOrNull { textWidth(score, it) <= width } ?: scoreStyles.last()
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(OquTurboLayout.textGap),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HeaderLeading(leadingContent)
                    HeaderTrophy()
                }
                HeaderValue(scoreLabel, score, fittingScoreStyle, true)
                HeaderValue(recordLabel, record, recordStyle, false)
            }
        }
    }
}

@Composable
private fun HeaderLeading(content: (@Composable () -> Unit)?) {
    Box(Modifier.size(OquTurboLayout.headerActionSize), contentAlignment = Alignment.CenterStart) {
        content?.invoke()
    }
}

@Composable
private fun HeaderValue(
    label: String,
    value: String,
    valueStyle: TextStyle,
    isScore: Boolean,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
    Column(horizontalAlignment = alignment) {
        Text(
            label,
            style = if (isScore) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            style = valueStyle,
            color = if (isScore) MaterialTheme.colorScheme.primary else Color.Unspecified,
            softWrap = false,
            maxLines = 1,
        )
    }
}

@Composable
private fun HeaderTrophy() {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
        Icon(
            Icons.Filled.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.padding(13.dp).size(OquTurboLayout.headerIconSize),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun GameHeaderActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(OquTurboLayout.headerActionSize),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(OquTurboLayout.headerIconSize),
            )
        }
    }
}
