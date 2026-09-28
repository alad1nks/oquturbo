package com.alad1nks.oquturbo.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboLayout

enum class GameStage { Ready, Paused }

/** Inline game stage. The feature owns width constraints, scroll and live-region semantics. */
@Composable
fun GameStagePanel(
    stage: GameStage,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = if (stage == GameStage.Ready) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth().padding(
                    horizontal = OquTurboLayout.stageHorizontalInset,
                    vertical = OquTurboLayout.stageVerticalInset,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OquTurboLayout.gap),
            content = content,
        )
    }
}
