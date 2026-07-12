package com.alad1nks.oquturbo.feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.home.ui.HomeRoute
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute

fun NavController.navigateToHome(
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(
        route = HomeRoute,
        builder = navOptions,
    )
}

fun NavGraphBuilder.homeScreen(onStartTrainingClick: () -> Unit) {
    composable<HomeRoute> {
        HomeRoute(onStartTrainingClick = onStartTrainingClick)
    }
}
