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
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

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
    modifier: Modifier = Modifier,
) {
    if (uiState is RememberNumberMenuUiState.CustomOptionsDialog) {
        RememberNumberMenuCustomOptionsDialog(
            length = uiState.maxLength,
            onLengthChange = onCustomSettingsLengthChange,
            digitsAvailability = uiState.digitsAvailability,
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
            uiState = RememberNumberMenuUiState.Default,
            showBackButton = true,
            onBackClick = {},
            onPlayClick = { _, _ -> },
            onCustomClick = {},
            onCustomSettingsLengthChange = {},
            onCustomSettingsDigitSelect = { _, _ -> },
            closeItemCustomOptionsDialog = {},
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
            uiState = RememberNumberMenuUiState.Default,
            showBackButton = true,
            onBackClick = {},
            onPlayClick = { _, _ -> },
            onCustomClick = {},
            onCustomSettingsLengthChange = {},
            onCustomSettingsDigitSelect = { _, _ -> },
            closeItemCustomOptionsDialog = {},
        )
    }
}

@Preview
@Composable
private fun RememberNumberMenuScreenDialogPreview() {
    Scaffold {
        RememberNumberMenuScreen(
            uiState = RememberNumberMenuUiState.CustomOptionsDialog(),
            showBackButton = true,
            onBackClick = {},
            onPlayClick = { _, _ -> },
            onCustomClick = {},
            onCustomSettingsLengthChange = {},
            onCustomSettingsDigitSelect = { _, _ -> },
            closeItemCustomOptionsDialog = {},
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
            uiState = RememberNumberMenuUiState.CustomOptionsDialog(),
            showBackButton = true,
            onBackClick = {},
            onPlayClick = { _, _ -> },
            onCustomClick = {},
            onCustomSettingsLengthChange = {},
            onCustomSettingsDigitSelect = { _, _ -> },
            closeItemCustomOptionsDialog = {},
        )
    }
}
