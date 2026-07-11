package com.alad1nks.oquturbo.feature.baspagamemenu.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alad1nks.oquturbo.core.ui.component.GameMenuItem
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BaspaGameMenuContentColumn(
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(AppResource.String.baspa_game_menu_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(AppResource.String.baspa_game_menu_subtitle))
            Spacer(modifier = Modifier.height(24.dp))

            BaspaGameMenuItem(
                Icons.Outlined.Category,
                AppResource.String.baspa_game_menu_categories_title,
                AppResource.String.baspa_game_menu_categories_subtitle,
            )
            BaspaGameMenuItem(
                Icons.Outlined.TextFields,
                AppResource.String.baspa_game_menu_letter_title,
                AppResource.String.baspa_game_menu_letter_subtitle,
            )
            BaspaGameMenuItem(
                Icons.Outlined.FormatSize,
                AppResource.String.baspa_game_menu_word_length_title,
                AppResource.String.baspa_game_menu_word_length_subtitle,
            )
            BaspaGameMenuItem(
                Icons.Outlined.Palette,
                AppResource.String.baspa_game_menu_text_color_title,
                AppResource.String.baspa_game_menu_text_color_subtitle,
            )
            BaspaGameMenuItem(
                Icons.AutoMirrored.Outlined.FactCheck,
                AppResource.String.baspa_game_menu_true_false_title,
                AppResource.String.baspa_game_menu_true_false_subtitle,
            )
            BaspaGameMenuItem(
                Icons.Outlined.Calculate,
                AppResource.String.baspa_game_menu_math_title,
                AppResource.String.baspa_game_menu_math_subtitle,
            )
            BaspaGameMenuItem(
                Icons.Outlined.Speed,
                AppResource.String.baspa_game_menu_speed_reading_title,
                AppResource.String.baspa_game_menu_speed_reading_subtitle,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BaspaGameMenuItem(
    imageVector: ImageVector,
    title: StringResource,
    subtitle: StringResource,
) {
    GameMenuItem(
        imageVector = imageVector,
        title = stringResource(title),
        subtitle = stringResource(subtitle),
        onClick = {},
    )
    Spacer(modifier = Modifier.height(16.dp))
}
