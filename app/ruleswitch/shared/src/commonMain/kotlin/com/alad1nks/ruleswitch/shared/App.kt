package com.alad1nks.ruleswitch.shared

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.alad1nks.oquturbo.feature.main.ui.MainScreen
import com.alad1nks.oquturbo.feature.ruleswitch.navigation.RuleSwitchRoute
import com.alad1nks.oquturbo.feature.ruleswitch.navigation.ruleSwitchScreen

@Composable
fun App() {
    val navController = rememberNavController()
    MainScreen(
        commonModules = getCommonModules(),
        platformModules = getPlatformModules(),
        startDestination = RuleSwitchRoute,
        navController = navController,
    ) {
        ruleSwitchScreen()
    }
}
