package com.alad1nks.oquturbo.feature.remembernumbermenu.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.remembernumbermenu.ui.RememberNumberMenuRoute
import kotlinx.serialization.Serializable

@Serializable data object RememberNumberMenuRoute

fun NavController.navigateToRememberNumberMenu(
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(
        route = RememberNumberMenuRoute,
        builder = navOptions,
    )
}

fun NavGraphBuilder.rememberNumberMenuScreen(
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onPlayClick: (Int, String) -> Unit,
) {
    composable<RememberNumberMenuRoute> { entry ->
        RememberNumberMenuRoute(
            showBackButton = showBackButton,
            onBackClick = onBackClick,
            onPlayClick = onPlayClick,
        )
    }
}
