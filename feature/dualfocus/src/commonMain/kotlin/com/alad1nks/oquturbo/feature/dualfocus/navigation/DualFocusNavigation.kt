package com.alad1nks.oquturbo.feature.dualfocus.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.dualfocus.ui.DualFocusRoute
import com.alad1nks.oquturbo.feature.dualfocus.ui.DualFocusViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object DualFocusRoute

fun NavController.navigateToDualFocus(
    navOptions: NavOptionsBuilder.() -> Unit = {
    },
) = navigate(DualFocusRoute, navOptions)

fun NavGraphBuilder.dualFocusScreen(onBackClick: (() -> Unit)? = null) {
    composable<DualFocusRoute> { DualFocusRoute(koinViewModel<DualFocusViewModel>(), onBackClick) }
}
