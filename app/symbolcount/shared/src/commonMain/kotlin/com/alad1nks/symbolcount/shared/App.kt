package com.alad1nks.symbolcount.shared

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.alad1nks.oquturbo.feature.main.ui.MainScreen
import com.alad1nks.oquturbo.feature.symbolcount.navigation.SymbolCountRoute
import com.alad1nks.oquturbo.feature.symbolcount.navigation.symbolCountScreen

@Composable
fun App() {
    val navController = rememberNavController()
    MainScreen(
        commonModules = getCommonModules(),
        platformModules = getPlatformModules(),
        startDestination = SymbolCountRoute,
        navController = navController,
    ) {
        symbolCountScreen()
    }
}
