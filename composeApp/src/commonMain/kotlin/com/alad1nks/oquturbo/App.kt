package com.alad1nks.oquturbo

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.alad1nks.oquturbo.feature.remembernumber.navigation.RememberNumberRoute
import com.alad1nks.oquturbo.feature.remembernumber.navigation.rememberNumberScreen
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
        NavHost(
            navController = navController,
            startDestination = RememberNumberRoute(4),
        ) {
            rememberNumberScreen()
        }
    }
}
