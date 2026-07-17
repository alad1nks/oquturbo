package com.alad1nks.oquturbo.feature.remembernumbermenu.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.AppTopBar
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RememberNumberMenuRoute(
    viewModel: RememberNumberMenuViewModel,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onPlayClick: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    RememberNumberMenuScreen(
        uiState = uiState,
        showBackButton = showBackButton,
        onBackClick = onBackClick,
        onPlayClick = onPlayClick,
        onCustomClick = viewModel::openItemCustomOptionsDialog,
        onCustomSettingsLengthChange = viewModel::changeCustomSettingsLength,
        onCustomSettingsDigitSelect = viewModel::selectCustomSettingsDigit,
        closeItemCustomOptionsDialog = viewModel::closeItemCustomOptionsDialog,
        onDarkThemeChange = viewModel::changeDarkTheme,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RememberNumberMenuScreen(
    uiState: RememberNumberMenuUiState,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onPlayClick: (Int, String) -> Unit,
    onCustomClick: () -> Unit,
    onCustomSettingsLengthChange: (Int) -> Unit,
    onCustomSettingsDigitSelect: (Int, Boolean) -> Unit,
    closeItemCustomOptionsDialog: () -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    if (uiState.customOptionsDialog.show) {
        RememberNumberMenuCustomOptionsDialog(
            length = uiState.customOptionsDialog.maxLength,
            onLengthChange = onCustomSettingsLengthChange,
            digitsAvailability = uiState.customOptionsDialog.digitsAvailability,
            onDigitSelect = onCustomSettingsDigitSelect,
            onDismissRequest = closeItemCustomOptionsDialog,
            onPlayClick = onPlayClick,
        )
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .appBackground()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = stringResource(AppResource.String.remember_number_title),
                onBackClick = onBackClick.takeIf { showBackButton },
                actions = {
                    if (uiState.themeIcon.show) {
                        RememberNumberMenuThemeIcon(
                            darkTheme = uiState.themeIcon.darkTheme,
                            onChange = onDarkThemeChange,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                RememberNumberMenuContentColumn(
                    onPlayClick = onPlayClick,
                    onCustomClick = onCustomClick,
                    modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun RememberNumberMenuScreenPreview() {
    OquTurboTheme {
        RememberNumberMenuScreen(
            uiState = RememberNumberMenuUiState(),
            showBackButton = true,
            onBackClick = {},
            onPlayClick = { _, _ -> },
            onCustomClick = {},
            onCustomSettingsLengthChange = {},
            onCustomSettingsDigitSelect = { _, _ -> },
            closeItemCustomOptionsDialog = {},
            onDarkThemeChange = {},
        )
    }
}

@Preview(
    widthDp = 800,
    heightDp = 1280,
)
@Composable
private fun RememberNumberMenuScreenPreviewTablet() {
    OquTurboTheme {
        RememberNumberMenuScreen(
            uiState = RememberNumberMenuUiState(),
            showBackButton = true,
            onBackClick = {},
            onPlayClick = { _, _ -> },
            onCustomClick = {},
            onCustomSettingsLengthChange = {},
            onCustomSettingsDigitSelect = { _, _ -> },
            closeItemCustomOptionsDialog = {},
            onDarkThemeChange = {},
        )
    }
}

@Preview
@Composable
private fun RememberNumberMenuScreenDialogPreview() {
    OquTurboTheme {
        RememberNumberMenuScreen(
            uiState =
                RememberNumberMenuUiState(
                    customOptionsDialog = RememberNumberMenuUiState.CustomOptionsDialog(show = true),
                ),
            showBackButton = true,
            onBackClick = {},
            onPlayClick = { _, _ -> },
            onCustomClick = {},
            onCustomSettingsLengthChange = {},
            onCustomSettingsDigitSelect = { _, _ -> },
            closeItemCustomOptionsDialog = {},
            onDarkThemeChange = {},
        )
    }
}

@Preview(
    widthDp = 800,
    heightDp = 1280,
)
@Composable
private fun RememberNumberMenuScreenDialogPreviewTablet() {
    OquTurboTheme {
        RememberNumberMenuScreen(
            uiState =
                RememberNumberMenuUiState(
                    customOptionsDialog = RememberNumberMenuUiState.CustomOptionsDialog(show = true),
                ),
            showBackButton = true,
            onBackClick = {},
            onPlayClick = { _, _ -> },
            onCustomClick = {},
            onCustomSettingsLengthChange = {},
            onCustomSettingsDigitSelect = { _, _ -> },
            closeItemCustomOptionsDialog = {},
            onDarkThemeChange = {},
        )
    }
}
