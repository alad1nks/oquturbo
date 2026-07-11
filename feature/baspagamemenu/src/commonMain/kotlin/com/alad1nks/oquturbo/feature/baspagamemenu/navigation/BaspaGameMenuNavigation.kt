package com.alad1nks.oquturbo.feature.baspagamemenu.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.baspagamemenu.ui.BaspaGameMenuScreen
import kotlinx.serialization.Serializable

@Serializable data object BaspaGameMenuRoute

fun NavGraphBuilder.baspaGameMenuScreen() {
    composable<BaspaGameMenuRoute> {
        BaspaGameMenuScreen()
    }
}
