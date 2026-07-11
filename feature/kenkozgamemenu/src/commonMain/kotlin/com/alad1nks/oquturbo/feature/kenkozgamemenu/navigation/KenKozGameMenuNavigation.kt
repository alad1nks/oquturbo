package com.alad1nks.oquturbo.feature.kenkozgamemenu.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import com.alad1nks.oquturbo.feature.kenkozgamemenu.ui.KenKozGameMenuScreen
import kotlinx.serialization.Serializable

@Serializable data object KenKozGameMenuRoute

fun NavGraphBuilder.kenKozGameMenuScreen(
    onModeClick: (KenKozGameMode) -> Unit,
) {
    composable<KenKozGameMenuRoute> {
        KenKozGameMenuScreen(onModeClick = onModeClick)
    }
}
