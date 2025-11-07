package com.alad1nks.sansprint

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.alad1nks.oquturbo.feature.remembernumber.navigation.navigateToRememberNumber
import com.alad1nks.oquturbo.feature.remembernumber.navigation.rememberNumberScreen
import com.alad1nks.oquturbo.feature.remembernumbermenu.navigation.RememberNumberMenuRoute
import com.alad1nks.oquturbo.feature.remembernumbermenu.navigation.rememberNumberMenuScreen
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication

@Composable
fun KoinApp() {
    KoinApplication(
        application = {
            modules(getPlatformModules() + getCommonModules())
        },
    ) {
        App()
    }
}

@Composable
@Preview
fun App() {
    val navController = rememberNavController()

    MaterialTheme {
        Scaffold {
            NavHost(
                navController = navController,
                startDestination = RememberNumberMenuRoute,
            ) {
                rememberNumberScreen(onBackClick = navController::popBackStack)
                rememberNumberMenuScreen(
                    showBackButton = false,
                    onBackClick = navController::popBackStack,
                    onPlayClick = navController::navigateToRememberNumber,
                )
            }
        }
    }
}
