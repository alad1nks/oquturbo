package com.alad1nks.oquturbo.feature.remembernumbermenu.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PlusOne
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(AppResource.String.remember_number_menu_title),
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(AppResource.String.remember_number_menu_subtitle),
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        RememberNumberMenuItem(
            imageVector = Icons.Outlined.Timer,
            title = stringResource(AppResource.String.remember_number_menu_item_classic_title),
            subtitle = stringResource(AppResource.String.remember_number_menu_item_classic_subtitle),
            onClick = { onPlayClick(4, "0123456789") },
            onDetailsClick = {},
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        RememberNumberMenuItem(
            imageVector = Icons.Outlined.PlusOne,
            title = stringResource(AppResource.String.remember_number_menu_item_classic_title),
            subtitle = stringResource(AppResource.String.remember_number_menu_item_classic_subtitle),
            onClick = { onPlayClick(4, "01") },
            onDetailsClick = {},
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        RememberNumberMenuItem(
            imageVector = Icons.Outlined.Settings,
            title = stringResource(AppResource.String.remember_number_menu_item_classic_title),
            subtitle = stringResource(AppResource.String.remember_number_menu_item_classic_subtitle),
            onClick = {},
            onDetailsClick = {},
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
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
