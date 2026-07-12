package com.alad1nks.oquturbo.shared

import androidx.compose.runtime.Composable
import com.alad1nks.oquturbo.feature.baspagame.navigation.baspaGameScreen
import com.alad1nks.oquturbo.feature.baspagame.navigation.navigateToBaspaGame
import com.alad1nks.oquturbo.feature.baspagamemenu.navigation.BaspaGameMenuRoute
import com.alad1nks.oquturbo.feature.baspagamemenu.navigation.baspaGameMenuScreen
import com.alad1nks.oquturbo.feature.baspagamemenu.navigation.navigateToBaspaGameMenu
import com.alad1nks.oquturbo.feature.games.model.TrainingGame
import com.alad1nks.oquturbo.feature.games.navigation.GamesRoute
import com.alad1nks.oquturbo.feature.games.navigation.gamesScreen
import com.alad1nks.oquturbo.feature.home.navigation.HomeRoute
import com.alad1nks.oquturbo.feature.home.navigation.homeScreen
import com.alad1nks.oquturbo.feature.kenkozgame.navigation.kenKozGameScreen
import com.alad1nks.oquturbo.feature.kenkozgame.navigation.navigateToKenKozGame
import com.alad1nks.oquturbo.feature.kenkozgamemenu.navigation.KenKozGameMenuRoute
import com.alad1nks.oquturbo.feature.kenkozgamemenu.navigation.kenKozGameMenuScreen
import com.alad1nks.oquturbo.feature.kenkozgamemenu.navigation.navigateToKenKozGameMenu
import com.alad1nks.oquturbo.feature.main.ui.MainScreen
import com.alad1nks.oquturbo.feature.remembernumber.navigation.navigateToRememberNumber
import com.alad1nks.oquturbo.feature.remembernumber.navigation.rememberNumberScreen
import com.alad1nks.oquturbo.feature.remembernumbermenu.navigation.RememberNumberMenuRoute
import com.alad1nks.oquturbo.feature.remembernumbermenu.navigation.navigateToRememberNumberMenu
import com.alad1nks.oquturbo.feature.remembernumbermenu.navigation.rememberNumberMenuScreen
import com.alad1nks.oquturbo.shared.navigation.OquTurboTopLevelDestination
import com.alad1nks.oquturbo.shared.navigation.profileScreen
import com.alad1nks.oquturbo.shared.navigation.statsScreen
import com.alad1nks.oquturbo.shared.ui.OquTurboNavigationBar
import com.alad1nks.oquturbo.shared.ui.rememberOquTurboAppState

@Composable
fun App() {
    val appState = rememberOquTurboAppState()

    MainScreen(
        commonModules = getCommonModules(),
        platformModules = getPlatformModules(),
        startDestination = HomeRoute,
        navController = appState.navController,
        bottomBar = {
            if (appState.shouldShowNavigationBar) {
                OquTurboNavigationBar(
                    destinations = appState.topLevelDestinations,
                    currentDestination = appState.currentDestination,
                    onNavigateToDestination = appState::navigateToTopLevelDestination,
                )
            }
        },
    ) {
        homeScreen(
            onStartTrainingClick = {
                appState.navigateToTopLevelDestination(OquTurboTopLevelDestination.GAMES)
            },
        )
        gamesScreen(
            onGameClick = { game ->
                when (game) {
                    TrainingGame.NumberSprint -> appState.navController.navigateToRememberNumberMenu()
                    TrainingGame.WideEye -> appState.navController.navigateToKenKozGameMenu()
                    TrainingGame.DontTap -> appState.navController.navigateToBaspaGameMenu()
                }
            },
            onSettingsClick = {
                appState.navigateToTopLevelDestination(OquTurboTopLevelDestination.PROFILE)
            },
        )
        rememberNumberMenuScreen(
            showBackButton = true,
            onBackClick = {
                appState.navController.popBackStack(route = GamesRoute, inclusive = false)
            },
            onPlayClick = appState.navController::navigateToRememberNumber,
        )
        kenKozGameMenuScreen(
            onModeClick = appState.navController::navigateToKenKozGame,
            showBackButton = true,
            onBackClick = {
                appState.navController.popBackStack(route = GamesRoute, inclusive = false)
            },
        )
        baspaGameMenuScreen(
            onModeClick = appState.navController::navigateToBaspaGame,
            showBackButton = true,
            onBackClick = {
                appState.navController.popBackStack(route = GamesRoute, inclusive = false)
            },
        )
        statsScreen()
        profileScreen()
        rememberNumberScreen {
            appState.navController.popBackStack(route = RememberNumberMenuRoute, inclusive = false)
        }
        kenKozGameScreen {
            appState.navController.popBackStack(route = KenKozGameMenuRoute, inclusive = false)
        }
        baspaGameScreen {
            appState.navController.popBackStack(route = BaspaGameMenuRoute, inclusive = false)
        }
    }
}
