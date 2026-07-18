package com.alad1nks.oquturbo.feature.games.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.games.model.TrainingGame
import com.alad1nks.oquturbo.feature.games.ui.GamesRoute
import kotlinx.serialization.Serializable

@Serializable data object GamesRoute

fun NavController.navigateToGames(
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(
        route = GamesRoute,
        builder = navOptions,
    )
}

fun NavGraphBuilder.gamesScreen(
    onGameClick: (TrainingGame) -> Unit,
) {
    composable<GamesRoute> {
        GamesRoute(
            onGameClick = onGameClick,
        )
    }
}
