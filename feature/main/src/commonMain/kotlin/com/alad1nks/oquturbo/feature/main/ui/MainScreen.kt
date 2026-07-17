package com.alad1nks.oquturbo.feature.main.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.appBackground
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinConfiguration

@Composable
fun MainScreen(
    commonModules: List<Module>,
    platformModules: List<Module>,
    startDestination: Any,
    navController: NavHostController,
    bottomBar: @Composable () -> Unit = {},
    navGraphBuilder: NavGraphBuilder.() -> Unit,
) {
    KoinApplication(
        configuration =
            koinConfiguration {
                modules(commonModules + platformModules)
            },
    ) {
        val viewModel =
            koinViewModel<MainViewModel>(
                parameters = { parametersOf() },
            )
        val darkTheme by viewModel.darkTheme.collectAsState(false)

        OquTurboTheme(darkTheme = darkTheme) {
            Scaffold(
                modifier = Modifier.fillMaxSize().appBackground(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = Color.Transparent,
                bottomBar = bottomBar,
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                    builder = navGraphBuilder,
                )
            }
        }
    }
}
