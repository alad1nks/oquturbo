package com.alad1nks.oquturbo.feature.baspagamemenu.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.AppTopBar
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameMode
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BaspaGameMenuScreen(
    onModeClick: (BaspaGameMode) -> Unit,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().appBackground()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = stringResource(AppResource.String.baspa_title),
                onBackClick = onBackClick.takeIf { showBackButton },
            )
            BaspaGameMenuContentColumn(
                onModeClick = onModeClick,
                modifier =
                    Modifier
                        .weight(1f)
                        .align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Preview
@Composable
private fun BaspaGameMenuScreenPreview() {
    OquTurboTheme {
        Scaffold {
            BaspaGameMenuScreen(onModeClick = {})
        }
    }
}
