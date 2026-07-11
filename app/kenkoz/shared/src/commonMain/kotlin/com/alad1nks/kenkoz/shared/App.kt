package com.alad1nks.kenkoz.shared

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.alad1nks.oquturbo.feature.kenkozgamemenu.navigation.KenKozGameMenuRoute
import com.alad1nks.oquturbo.feature.kenkozgamemenu.navigation.kenKozGameMenuScreen
import com.alad1nks.oquturbo.feature.main.ui.MainScreen

@Composable
fun App() {
    val navController = rememberNavController()

    MainScreen(
        commonModules = getCommonModules(),
        platformModules = getPlatformModules(),
        startDestination = KenKozGameMenuRoute,
        navController = navController,
    ) {
        kenKozGameMenuScreen()
    }
}
