package com.alad1nks.oquturbo.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

@Composable
fun Modifier.appBackground(): Modifier =
    background(
        brush =
            Brush.verticalGradient(
                colors =
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f),
                    ),
            ),
    )
