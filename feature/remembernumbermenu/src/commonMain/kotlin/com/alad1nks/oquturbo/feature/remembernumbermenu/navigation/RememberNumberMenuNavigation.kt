package com.alad1nks.oquturbo.feature.remembernumbermenu.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.remembernumbermenu.ui.RememberNumberMenuRoute
import com.alad1nks.oquturbo.feature.remembernumbermenu.ui.RememberNumberMenuViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

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
    composable<RememberNumberMenuRoute> { _ ->
        val viewModel = koinViewModel<RememberNumberMenuViewModel>()

        RememberNumberMenuRoute(
            viewModel = viewModel,
            showBackButton = showBackButton,
            onBackClick = onBackClick,
            onPlayClick = onPlayClick,
        )
    }
}
