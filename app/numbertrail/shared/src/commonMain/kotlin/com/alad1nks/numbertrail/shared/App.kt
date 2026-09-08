package com.alad1nks.numbertrail.shared

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.alad1nks.oquturbo.feature.main.ui.MainScreen
import com.alad1nks.oquturbo.feature.numbertrail.navigation.NumberTrailRoute
import com.alad1nks.oquturbo.feature.numbertrail.navigation.numberTrailScreen

@Composable
fun App() {
    val navController = rememberNavController()
    MainScreen(
        commonModules = getCommonModules(),
        platformModules = getPlatformModules(),
        startDestination = NumberTrailRoute,
        navController = navController,
    ) {
        numberTrailScreen()
    }
}
