package com.alad1nks.oquturbo.shared

import androidx.compose.runtime.Composable
import com.alad1nks.oquturbo.feature.home.navigation.HomeRoute
import com.alad1nks.oquturbo.feature.home.navigation.homeScreen
import com.alad1nks.oquturbo.feature.main.ui.MainScreen
import com.alad1nks.oquturbo.feature.remembernumber.navigation.navigateToRememberNumber
import com.alad1nks.oquturbo.feature.remembernumber.navigation.rememberNumberScreen
import com.alad1nks.oquturbo.feature.remembernumbermenu.navigation.RememberNumberMenuRoute
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
        rememberNumberMenuScreen(
            showBackButton = false,
            onBackClick = {},
            onPlayClick = appState.navController::navigateToRememberNumber,
        )
        statsScreen()
        profileScreen()
        rememberNumberScreen {
            appState.navController.popBackStack(route = RememberNumberMenuRoute, inclusive = false)
        }
    }
}
