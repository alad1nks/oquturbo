package com.alad1nks.oquturbo.feature.kenkozgamemenu.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alad1nks.oquturbo.core.ui.component.GameMenuItem
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun KenKozGameMenuContentColumn(
    onModeClick: (KenKozGameMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(AppResource.String.kenkoz_game_menu_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(text = stringResource(AppResource.String.kenkoz_game_menu_subtitle))

            Spacer(modifier = Modifier.height(24.dp))

            GameMenuItem(
                imageVector = Icons.Outlined.TextFields,
                title = stringResource(AppResource.String.kenkoz_game_menu_item_characters_title),
                subtitle = stringResource(AppResource.String.kenkoz_game_menu_item_characters_subtitle),
                onClick = { onModeClick(KenKozGameMode.Characters) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            GameMenuItem(
                imageVector = Icons.AutoMirrored.Outlined.Article,
                title = stringResource(AppResource.String.kenkoz_game_menu_item_words_title),
                subtitle = stringResource(AppResource.String.kenkoz_game_menu_item_words_subtitle),
                onClick = { onModeClick(KenKozGameMode.Words) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            GameMenuItem(
                imageVector = Icons.Outlined.Difference,
                title = stringResource(AppResource.String.kenkoz_game_menu_item_find_difference_title),
                subtitle = stringResource(AppResource.String.kenkoz_game_menu_item_find_difference_subtitle),
                onClick = { onModeClick(KenKozGameMode.FindDifference) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            GameMenuItem(
                imageVector = Icons.Outlined.ViewWeek,
                title = stringResource(AppResource.String.kenkoz_game_menu_item_wide_line_title),
                subtitle = stringResource(AppResource.String.kenkoz_game_menu_item_wide_line_subtitle),
                onClick = { onModeClick(KenKozGameMode.WideLine) },
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
