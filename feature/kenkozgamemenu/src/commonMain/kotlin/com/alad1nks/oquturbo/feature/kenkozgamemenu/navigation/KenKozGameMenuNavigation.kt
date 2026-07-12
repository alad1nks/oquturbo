package com.alad1nks.oquturbo.feature.kenkozgamemenu.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import com.alad1nks.oquturbo.feature.kenkozgamemenu.ui.KenKozGameMenuScreen
import kotlinx.serialization.Serializable

@Serializable data object KenKozGameMenuRoute

fun NavController.navigateToKenKozGameMenu(
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(
        route = KenKozGameMenuRoute,
        builder = navOptions,
    )
}

fun NavGraphBuilder.kenKozGameMenuScreen(
    onModeClick: (KenKozGameMode) -> Unit,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
) {
    composable<KenKozGameMenuRoute> {
        KenKozGameMenuScreen(
            onModeClick = onModeClick,
            showBackButton = showBackButton,
            onBackClick = onBackClick,
        )
    }
}
