package com.alad1nks.oquturbo.feature.kenkozgamemenu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.ui.component.GameMenuItem
import com.alad1nks.oquturbo.core.ui.component.PageHeader
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun KenKozGameMenuContentColumn(
    onModeClick: (KenKozGameMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageHeader(
                title = stringResource(AppResource.String.kenkoz_game_menu_title),
                subtitle = stringResource(AppResource.String.kenkoz_game_menu_subtitle),
            )
        }

        item {
            GameMenuItem(
                imageVector = Icons.Outlined.TextFields,
                title = stringResource(AppResource.String.kenkoz_game_menu_item_characters_title),
                subtitle = stringResource(AppResource.String.kenkoz_game_menu_item_characters_subtitle),
                onClick = { onModeClick(KenKozGameMode.Characters) },
            )
        }

        item {
            GameMenuItem(
                imageVector = Icons.AutoMirrored.Outlined.Article,
                title = stringResource(AppResource.String.kenkoz_game_menu_item_words_title),
                subtitle = stringResource(AppResource.String.kenkoz_game_menu_item_words_subtitle),
                onClick = { onModeClick(KenKozGameMode.Words) },
            )
        }

        item {
            GameMenuItem(
                imageVector = Icons.Outlined.Difference,
                title = stringResource(AppResource.String.kenkoz_game_menu_item_find_difference_title),
                subtitle = stringResource(AppResource.String.kenkoz_game_menu_item_find_difference_subtitle),
                onClick = { onModeClick(KenKozGameMode.FindDifference) },
            )
        }

        item {
            GameMenuItem(
                imageVector = Icons.Outlined.ViewWeek,
                title = stringResource(AppResource.String.kenkoz_game_menu_item_wide_line_title),
                subtitle = stringResource(AppResource.String.kenkoz_game_menu_item_wide_line_subtitle),
                onClick = { onModeClick(KenKozGameMode.WideLine) },
            )
        }
    }
}
