package com.alad1nks.oquturbo.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance

@Composable
fun Modifier.appBackground(): Modifier {
    val colorScheme = MaterialTheme.colorScheme
    return if (colorScheme.background.luminance() < 0.5f) {
        background(colorScheme.background)
    } else {
        background(
            brush =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            colorScheme.primaryContainer.copy(alpha = 0.18f),
                            colorScheme.surfaceContainerLowest,
                            colorScheme.secondaryContainer.copy(alpha = 0.12f),
                        ),
                ),
        )
    }
}
