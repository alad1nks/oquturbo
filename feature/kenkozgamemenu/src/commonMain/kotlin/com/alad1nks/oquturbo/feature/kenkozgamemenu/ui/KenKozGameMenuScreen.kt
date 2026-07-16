package com.alad1nks.oquturbo.feature.kenkozgamemenu.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.AppTopBar
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun KenKozGameMenuScreen(
    onModeClick: (KenKozGameMode) -> Unit,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().appBackground()) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxSize(),
        ) {
            AppTopBar(
                title = stringResource(AppResource.String.kenkoz_title),
                onBackClick = if (showBackButton) onBackClick else null,
            )

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                KenKozGameMenuContentColumn(
                    onModeClick = onModeClick,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .widthIn(max = 760.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun KenKozGameMenuScreenPreview() {
    OquTurboTheme {
        KenKozGameMenuScreen(onModeClick = {})
    }
}
