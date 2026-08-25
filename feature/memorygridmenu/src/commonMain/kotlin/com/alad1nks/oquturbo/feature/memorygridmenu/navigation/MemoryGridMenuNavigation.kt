package com.alad1nks.oquturbo.feature.memorygridmenu.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridGameMode
import com.alad1nks.oquturbo.feature.memorygridmenu.ui.MemoryGridMenuScreen
import kotlinx.serialization.Serializable

@Serializable data object MemoryGridMenuRoute

fun NavController.navigateToMemoryGridMenu(navOptions: NavOptionsBuilder.() -> Unit = {}) =
    navigate(MemoryGridMenuRoute, navOptions)

fun NavGraphBuilder.memoryGridMenuScreen(
    onModeClick: (MemoryGridGameMode) -> Unit,
    onBackClick: () -> Unit,
) {
    composable<MemoryGridMenuRoute> {
        MemoryGridMenuScreen(onModeClick = onModeClick, onBackClick = onBackClick)
    }
}
