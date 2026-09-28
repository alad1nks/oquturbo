package com.alad1nks.oquturbo.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp

enum class AppCardTone { Neutral, Primary, Secondary, Subdued }

/** Page and subordinate surfaces; callers own content padding and scrolling. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    tone: AppCardTone = AppCardTone.Neutral,
    containerColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = if (compact) MaterialTheme.shapes.medium else MaterialTheme.shapes.large,
        colors = appCardColors(tone, containerColor),
        border = appCardBorder(tone),
        elevation = CardDefaults.cardElevation(defaultElevation = if (compact || tone.isAccent) 0.dp else 1.dp),
        content = content,
    )
}

@Composable
fun AppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
    tone: AppCardTone = AppCardTone.Neutral,
    containerColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = if (compact) MaterialTheme.shapes.medium else MaterialTheme.shapes.large,
        colors = appCardColors(tone, containerColor),
        border = appCardBorder(tone),
        elevation = CardDefaults.cardElevation(defaultElevation = if (compact || tone.isAccent) 0.dp else 1.dp),
        content = content,
    )
}

private val AppCardTone.isAccent get() = this == AppCardTone.Primary || this == AppCardTone.Secondary

@Composable
private fun appCardColors(tone: AppCardTone, containerColor: Color) =
    CardDefaults.cardColors(
        containerColor =
            containerColor.takeOrElse {
                when (tone) {
                    AppCardTone.Neutral -> MaterialTheme.colorScheme.surface
                    AppCardTone.Primary -> MaterialTheme.colorScheme.primaryContainer
                    AppCardTone.Secondary -> MaterialTheme.colorScheme.secondaryContainer
                    AppCardTone.Subdued -> MaterialTheme.colorScheme.surfaceContainerLow
                }
            },
        contentColor =
            when (tone) {
                AppCardTone.Primary -> MaterialTheme.colorScheme.onPrimaryContainer
                AppCardTone.Secondary -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
    )

@Composable
private fun appCardBorder(tone: AppCardTone): BorderStroke? =
    if (tone.isAccent) {
        null
    } else {
        BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (tone == AppCardTone.Subdued) 0.42f else 0.55f),
        )
    }
