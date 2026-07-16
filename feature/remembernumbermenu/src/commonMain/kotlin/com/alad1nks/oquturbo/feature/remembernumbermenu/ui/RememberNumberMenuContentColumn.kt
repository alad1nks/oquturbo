package com.alad1nks.oquturbo.feature.remembernumbermenu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.ui.component.GameMenuItem
import com.alad1nks.oquturbo.core.ui.component.PageHeader
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RememberNumberMenuContentColumn(
    onPlayClick: (Int, String) -> Unit,
    onCustomClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageHeader(
                title = stringResource(AppResource.String.remember_number_menu_title),
                subtitle = stringResource(AppResource.String.remember_number_menu_subtitle),
            )
        }
        item {
            GameMenuItem(
                imageVector = Icons.Outlined.Timer,
                title = stringResource(AppResource.String.remember_number_menu_item_classic_title),
                subtitle = stringResource(AppResource.String.remember_number_menu_item_classic_subtitle),
                onClick = { onPlayClick(4, "0123456789") },
            )
        }
        item {
            GameMenuItem(
                imageVector = Icons.Outlined.Computer,
                title = stringResource(AppResource.String.remember_number_menu_item_binary_title),
                subtitle = stringResource(AppResource.String.remember_number_menu_item_binary_subtitle),
                onClick = { onPlayClick(4, "01") },
            )
        }
        item {
            GameMenuItem(
                imageVector = Icons.Outlined.Settings,
                title = stringResource(AppResource.String.remember_number_menu_item_custom_title),
                subtitle = stringResource(AppResource.String.remember_number_menu_item_custom_subtitle),
                onClick = onCustomClick,
            )
        }
    }
}
