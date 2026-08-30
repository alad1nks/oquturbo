package com.alad1nks.dualfocus.shared

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.alad1nks.oquturbo.feature.dualfocus.navigation.DualFocusRoute
import com.alad1nks.oquturbo.feature.dualfocus.navigation.dualFocusScreen
import com.alad1nks.oquturbo.feature.main.ui.MainScreen

@Composable
fun App() {
    val navController = rememberNavController()
    MainScreen(
        commonModules = getCommonModules(),
        platformModules = getPlatformModules(),
        startDestination = DualFocusRoute,
        navController = navController,
    ) {
        dualFocusScreen()
    }
}
