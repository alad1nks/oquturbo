package com.alad1nks.wordflow.shared

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.alad1nks.oquturbo.feature.main.ui.MainScreen
import com.alad1nks.oquturbo.feature.wordflow.navigation.WordFlowRoute
import com.alad1nks.oquturbo.feature.wordflow.navigation.wordFlowScreen

@Composable
fun App() {
    val navController = rememberNavController()
    MainScreen(
        commonModules = getCommonModules(),
        platformModules = getPlatformModules(),
        startDestination = WordFlowRoute,
        navController = navController,
    ) {
        wordFlowScreen()
    }
}
