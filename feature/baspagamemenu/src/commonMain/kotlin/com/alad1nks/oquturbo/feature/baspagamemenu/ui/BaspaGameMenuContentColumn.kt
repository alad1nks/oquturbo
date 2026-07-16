package com.alad1nks.oquturbo.feature.baspagamemenu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.ui.component.GameMenuItem
import com.alad1nks.oquturbo.core.ui.component.PageHeader
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameMode
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BaspaGameMenuContentColumn(
    onModeClick: (BaspaGameMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.widthIn(max = 760.dp).fillMaxWidth(),
        contentPadding = PaddingValues(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PageHeader(
                title = stringResource(AppResource.String.baspa_game_menu_title),
                subtitle = stringResource(AppResource.String.baspa_game_menu_subtitle),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
        item {
            GameMenuItem(
                imageVector = Icons.Outlined.Category,
                title = stringResource(AppResource.String.baspa_game_menu_categories_title),
                subtitle = stringResource(AppResource.String.baspa_game_menu_categories_subtitle),
                onClick = { onModeClick(BaspaGameMode.Categories) },
            )
        }
        item {
            GameMenuItem(
                imageVector = Icons.Outlined.SortByAlpha,
                title = stringResource(AppResource.String.baspa_game_menu_letter_title),
                subtitle = stringResource(AppResource.String.baspa_game_menu_letter_subtitle),
                onClick = { onModeClick(BaspaGameMode.Letter) },
            )
        }
        item {
            GameMenuItem(
                imageVector = Icons.Outlined.Straighten,
                title = stringResource(AppResource.String.baspa_game_menu_word_length_title),
                subtitle = stringResource(AppResource.String.baspa_game_menu_word_length_subtitle),
                onClick = { onModeClick(BaspaGameMode.WordLength) },
            )
        }
        item {
            GameMenuItem(
                imageVector = Icons.Outlined.Palette,
                title = stringResource(AppResource.String.baspa_game_menu_text_color_title),
                subtitle = stringResource(AppResource.String.baspa_game_menu_text_color_subtitle),
                onClick = { onModeClick(BaspaGameMode.TextColor) },
            )
        }
        item {
            GameMenuItem(
                imageVector = Icons.AutoMirrored.Outlined.FactCheck,
                title = stringResource(AppResource.String.baspa_game_menu_true_false_title),
                subtitle = stringResource(AppResource.String.baspa_game_menu_true_false_subtitle),
                onClick = { onModeClick(BaspaGameMode.TrueFalse) },
            )
        }
        item {
            GameMenuItem(
                imageVector = Icons.Outlined.Calculate,
                title = stringResource(AppResource.String.baspa_game_menu_math_title),
                subtitle = stringResource(AppResource.String.baspa_game_menu_math_subtitle),
                onClick = { onModeClick(BaspaGameMode.Math) },
            )
        }
        item {
            GameMenuItem(
                imageVector = Icons.Outlined.Speed,
                title = stringResource(AppResource.String.baspa_game_menu_speed_reading_title),
                subtitle = stringResource(AppResource.String.baspa_game_menu_speed_reading_subtitle),
                onClick = { onModeClick(BaspaGameMode.SpeedReading) },
            )
        }
    }
}
