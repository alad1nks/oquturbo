package com.alad1nks.oquturbo.feature.memorygrid.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.alad1nks.oquturbo.core.ui.navigation.enumNavType
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridGameMode
import com.alad1nks.oquturbo.feature.memorygrid.ui.MemoryGridRoute
import com.alad1nks.oquturbo.feature.memorygrid.ui.MemoryGridViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.reflect.typeOf

@Serializable
data class MemoryGridRoute(val mode: MemoryGridGameMode)

private val memoryGridTypeMap = mapOf(typeOf<MemoryGridGameMode>() to enumNavType<MemoryGridGameMode>())

fun NavController.navigateToMemoryGrid(
    mode: MemoryGridGameMode,
    navOptions: NavOptionsBuilder.() -> Unit = {},
) = navigate(MemoryGridRoute(mode), navOptions)

fun NavGraphBuilder.memoryGridScreen(onBackClick: () -> Unit) {
    composable<MemoryGridRoute>(typeMap = memoryGridTypeMap) { entry ->
        val route = entry.toRoute<MemoryGridRoute>()
        val viewModel = koinViewModel<MemoryGridViewModel>(parameters = { parametersOf(route.mode) })
        MemoryGridRoute(viewModel = viewModel, onBackClick = onBackClick)
    }
}
