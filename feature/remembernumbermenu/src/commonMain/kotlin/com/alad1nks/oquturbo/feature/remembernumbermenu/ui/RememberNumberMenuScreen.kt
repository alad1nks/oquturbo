package com.alad1nks.oquturbo.feature.remembernumbermenu.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(text = stringResource(AppResource.String.remember_number_title))
            },
            navigationIcon = {
                if (showBackButton) {
                    IconButton(
                        onClick = onBackClick,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                }
            },
            actions = {
                if (uiState.themeIcon.show) {
                    RememberNumberMenuThemeIcon(
                        darkTheme = uiState.themeIcon.darkTheme,
                        onChange = onDarkThemeChange,
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
        )

        RememberNumberMenuContentColumn(
            onPlayClick = onPlayClick,
            onCustomClick = onCustomClick,
        )
    }
}

@Preview
@Composable
private fun RememberNumberMenuScreenPreview() {
    Scaffold {
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
    Scaffold {
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
    Scaffold {
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
    Scaffold {
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
