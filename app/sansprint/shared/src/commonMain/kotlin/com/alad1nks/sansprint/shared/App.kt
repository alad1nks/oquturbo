package com.alad1nks.sansprint.shared

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.alad1nks.oquturbo.feature.main.ui.MainScreen
import com.alad1nks.oquturbo.feature.remembernumber.navigation.navigateToRememberNumber
import com.alad1nks.oquturbo.feature.remembernumber.navigation.rememberNumberScreen
import com.alad1nks.oquturbo.feature.remembernumbermenu.navigation.RememberNumberMenuRoute
import com.alad1nks.oquturbo.feature.remembernumbermenu.navigation.rememberNumberMenuScreen

@Composable
fun App() {
    val navController = rememberNavController()

    MainScreen(
        commonModules = getCommonModules(),
        platformModules = getPlatformModules(),
        startDestination = RememberNumberMenuRoute,
        navController = navController,
    ) {
        rememberNumberMenuScreen(
            showBackButton = false,
            onBackClick = {},
            onPlayClick = navController::navigateToRememberNumber,
        )
        rememberNumberScreen {
            navController.popBackStack(route = RememberNumberMenuRoute, inclusive = false)
        }
    }
}
