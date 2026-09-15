package com.alad1nks.oquturbo.feature.symbolcount.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.symbolcount.ui.SymbolCountRoute
import com.alad1nks.oquturbo.feature.symbolcount.ui.SymbolCountViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object SymbolCountRoute

fun NavController.navigateToSymbolCount(
    navOptions: NavOptionsBuilder.() -> Unit = {
    },
) = navigate(SymbolCountRoute, navOptions)

fun NavGraphBuilder.symbolCountScreen(onBackClick: (() -> Unit)? = null) {
    composable<SymbolCountRoute> { SymbolCountRoute(koinViewModel<SymbolCountViewModel>(), onBackClick) }
}
