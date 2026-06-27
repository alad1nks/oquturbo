package com.alad1nks.oquturbo.feature.remembernumbermenu.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RememberNumberMenuContentColumn(
    onPlayClick: (Int, String) -> Unit,
    onCustomClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(AppResource.String.remember_number_menu_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(AppResource.String.remember_number_menu_subtitle),
            )

            Spacer(modifier = Modifier.height(24.dp))

            RememberNumberMenuItem(
                imageVector = Icons.Outlined.Timer,
                title = stringResource(AppResource.String.remember_number_menu_item_classic_title),
                subtitle = stringResource(AppResource.String.remember_number_menu_item_classic_subtitle),
                onClick = { onPlayClick(4, "0123456789") },
            )

            Spacer(modifier = Modifier.height(16.dp))

            RememberNumberMenuItem(
                imageVector = Icons.Outlined.Computer,
                title = stringResource(AppResource.String.remember_number_menu_item_binary_title),
                subtitle = stringResource(AppResource.String.remember_number_menu_item_binary_subtitle),
                onClick = { onPlayClick(4, "01") },
            )

            Spacer(modifier = Modifier.height(16.dp))

            RememberNumberMenuItem(
                imageVector = Icons.Outlined.Settings,
                title = stringResource(AppResource.String.remember_number_menu_item_custom_title),
                subtitle = stringResource(AppResource.String.remember_number_menu_item_custom_subtitle),
                onClick = onCustomClick,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
