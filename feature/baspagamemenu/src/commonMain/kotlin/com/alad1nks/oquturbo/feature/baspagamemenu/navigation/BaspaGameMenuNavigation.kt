package com.alad1nks.oquturbo.feature.baspagamemenu.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameMode
import com.alad1nks.oquturbo.feature.baspagamemenu.ui.BaspaGameMenuScreen
import kotlinx.serialization.Serializable

@Serializable data object BaspaGameMenuRoute

fun NavController.navigateToBaspaGameMenu(
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(
        route = BaspaGameMenuRoute,
        builder = navOptions,
    )
}

fun NavGraphBuilder.baspaGameMenuScreen(
    onModeClick: (BaspaGameMode) -> Unit,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
) {
    composable<BaspaGameMenuRoute> {
        BaspaGameMenuScreen(
            onModeClick = onModeClick,
            showBackButton = showBackButton,
            onBackClick = onBackClick,
        )
    }
}
