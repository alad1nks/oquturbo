package com.alad1nks.baspa.shared

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.alad1nks.oquturbo.feature.baspagame.navigation.baspaGameScreen
import com.alad1nks.oquturbo.feature.baspagame.navigation.navigateToBaspaGame
import com.alad1nks.oquturbo.feature.baspagamemenu.navigation.BaspaGameMenuRoute
import com.alad1nks.oquturbo.feature.baspagamemenu.navigation.baspaGameMenuScreen
import com.alad1nks.oquturbo.feature.main.ui.MainScreen

@Composable
fun App() {
    val navController = rememberNavController()
    MainScreen(
        commonModules = getCommonModules(),
        platformModules = getPlatformModules(),
        startDestination = BaspaGameMenuRoute,
        navController = navController,
    ) {
        baspaGameMenuScreen(navController::navigateToBaspaGame)
        baspaGameScreen(navController::popBackStack)
    }
}
