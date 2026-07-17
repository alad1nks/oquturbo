package com.alad1nks.oquturbo.feature.stats.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.alad1nks.oquturbo.core.ui.navigation.enumNavType
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.ui.StatsGameDetailRouteContent
import com.alad1nks.oquturbo.feature.stats.ui.StatsGameDetailViewModel
import com.alad1nks.oquturbo.feature.stats.ui.StatsModeDetailRouteContent
import com.alad1nks.oquturbo.feature.stats.ui.StatsModeDetailViewModel
import com.alad1nks.oquturbo.feature.stats.ui.StatsRouteContent
import com.alad1nks.oquturbo.feature.stats.ui.StatsViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.reflect.typeOf

@Serializable
data object StatsRoute

@Serializable
data class StatsGameDetailRoute(
    val game: StatsGame,
    val period: StatsPeriod,
)

@Serializable
data class StatsModeDetailRoute(
    val game: StatsGame,
    val mode: StatsMode,
    val period: StatsPeriod,
)

private val statsGameDetailTypeMap =
    mapOf(
        typeOf<StatsGame>() to enumNavType<StatsGame>(),
        typeOf<StatsPeriod>() to enumNavType<StatsPeriod>(),
    )

private val statsModeDetailTypeMap =
    statsGameDetailTypeMap +
        (typeOf<StatsMode>() to enumNavType<StatsMode>())

fun NavController.navigateToStats(navOptions: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route = StatsRoute, builder = navOptions)
}

fun NavController.navigateToStatsGame(
    game: StatsGame,
    period: StatsPeriod,
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(route = StatsGameDetailRoute(game, period), builder = navOptions)
}

fun NavController.navigateToStatsMode(
    game: StatsGame,
    mode: StatsMode,
    period: StatsPeriod,
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(route = StatsModeDetailRoute(game, mode, period), builder = navOptions)
}

fun NavGraphBuilder.statsScreen(
    onGamesClick: () -> Unit,
    onGameClick: (StatsGame, StatsPeriod) -> Unit,
    onActivityClick: (StatsGame, StatsMode?, StatsPeriod) -> Unit = { _, _, _ -> },
) {
    composable<StatsRoute> {
        StatsRouteContent(
            viewModel = koinViewModel<StatsViewModel>(),
            onGamesClick = onGamesClick,
            onGameClick = onGameClick,
            onActivityClick = onActivityClick,
        )
    }
}

fun NavGraphBuilder.statsGameDetailScreen(
    onBackClick: () -> Unit,
    onModeClick: (StatsGame, StatsMode, StatsPeriod) -> Unit,
) {
    composable<StatsGameDetailRoute>(typeMap = statsGameDetailTypeMap) { entry ->
        val route = entry.toRoute<StatsGameDetailRoute>()
        StatsGameDetailRouteContent(
            viewModel =
                koinViewModel<StatsGameDetailViewModel>(
                    parameters = { parametersOf(route.game, route.period) },
                ),
            onBackClick = onBackClick,
            onModeClick = onModeClick,
        )
    }
}

fun NavGraphBuilder.statsModeDetailScreen(onBackClick: () -> Unit) {
    composable<StatsModeDetailRoute>(typeMap = statsModeDetailTypeMap) { entry ->
        val route = entry.toRoute<StatsModeDetailRoute>()
        StatsModeDetailRouteContent(
            viewModel =
                koinViewModel<StatsModeDetailViewModel>(
                    parameters = { parametersOf(route.game, route.mode, route.period) },
                ),
            onBackClick = onBackClick,
        )
    }
}
