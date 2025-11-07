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
import androidx.compose.ui.Modifier
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun RememberNumberMenuRoute(
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onPlayClick: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    RememberNumberMenuScreen(
        showBackButton = showBackButton,
        onBackClick = onBackClick,
        onPlayClick = onPlayClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RememberNumberMenuScreen(
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onPlayClick: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
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

        RememberNumberMenuContentColumn(onPlayClick = onPlayClick)
    }
}

@Composable
@Preview
private fun RememberNumberMenuScreenPreview() {
    Scaffold {
        RememberNumberMenuScreen(
            showBackButton = true,
            onBackClick = {},
            onPlayClick = { _, _ -> },
        )
    }
}

@Composable
@Preview(
    widthDp = 800,
    heightDp = 1280,
)
private fun RememberNumberMenuScreenPreviewTablet() {
    Scaffold {
        RememberNumberMenuScreen(
            showBackButton = true,
            onBackClick = {},
            onPlayClick = { _, _ -> },
        )
    }
}
