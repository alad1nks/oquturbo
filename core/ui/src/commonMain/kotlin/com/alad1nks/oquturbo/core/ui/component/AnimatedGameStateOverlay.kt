package com.alad1nks.oquturbo.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun <T : Any> AnimatedGameStateOverlay(
    state: T?,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    var retainedState by remember { mutableStateOf(state) }
    val displayedState = state ?: retainedState

    SideEffect {
        if (state != null) {
            retainedState = state
        }
    }

    AnimatedVisibility(
        visible = state != null,
        modifier = modifier,
        enter =
            fadeIn(
                animationSpec =
                    tween(
                        durationMillis = GAME_STATE_OVERLAY_ANIMATION_DURATION_MILLIS,
                        easing = LinearEasing,
                    ),
            ),
        exit =
            fadeOut(
                animationSpec =
                    tween(
                        durationMillis = GAME_STATE_OVERLAY_ANIMATION_DURATION_MILLIS,
                        easing = LinearEasing,
                    ),
            ),
        label = "Game state overlay",
    ) {
        if (displayedState != null) {
            content(displayedState)
        }
    }
}

private const val GAME_STATE_OVERLAY_ANIMATION_DURATION_MILLIS = 300
