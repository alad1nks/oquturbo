package com.alad1nks.rotationmatch.shared

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.alad1nks.oquturbo.feature.main.ui.MainScreen
import com.alad1nks.oquturbo.feature.rotationmatch.navigation.RotationMatchRoute
import com.alad1nks.oquturbo.feature.rotationmatch.navigation.rotationMatchScreen

@Composable
fun App() {
    val navController = rememberNavController()
    MainScreen(
        commonModules = getCommonModules(),
        platformModules = getPlatformModules(),
        startDestination = RotationMatchRoute,
        navController = navController,
    ) {
        rotationMatchScreen()
    }
}
