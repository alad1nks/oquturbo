package com.alad1nks.oquturbo.feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry
import com.alad1nks.oquturbo.core.data.model.GameSeriesKey
import com.alad1nks.oquturbo.feature.home.ui.DailyTrainingCompleteScreen
import com.alad1nks.oquturbo.feature.home.ui.HomeRoute
import com.alad1nks.oquturbo.feature.home.ui.HomeViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object HomeRoute

@Serializable data object DailyTrainingCompleteRoute

fun NavController.navigateToHome(
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(
        route = HomeRoute,
        builder = navOptions,
    )
}

fun NavController.navigateToDailyTrainingComplete(
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(route = DailyTrainingCompleteRoute, builder = navOptions)
}

fun NavGraphBuilder.homeScreen(
    onStartTrainingClick: (DailyTrainingEntry) -> Unit,
    onViewProgressClick: () -> Unit,
    onModeStatisticsClick: (GameSeriesKey) -> Unit,
    onWeeklyReviewClick: () -> Unit,
) {
    composable<HomeRoute> {
        val viewModel = koinViewModel<HomeViewModel>()
        HomeRoute(
            viewModel = viewModel,
            onStartTrainingClick = onStartTrainingClick,
            onViewProgressClick = onViewProgressClick,
            onModeStatisticsClick = onModeStatisticsClick,
            onWeeklyReviewClick = onWeeklyReviewClick,
        )
    }
}

fun NavGraphBuilder.dailyTrainingCompleteScreen(onHomeClick: () -> Unit) {
    composable<DailyTrainingCompleteRoute> {
        DailyTrainingCompleteScreen(onHomeClick = onHomeClick)
    }
}
