package com.alad1nks.oquturbo.feature.rotationmatch.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.rotationmatch.ui.RotationMatchRoute
import com.alad1nks.oquturbo.feature.rotationmatch.ui.RotationMatchViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object RotationMatchRoute

fun NavController.navigateToRotationMatch(
    navOptions: NavOptionsBuilder.() -> Unit = {
    },
) = navigate(RotationMatchRoute, navOptions)

fun NavGraphBuilder.rotationMatchScreen(onBackClick: (() -> Unit)? = null) {
    composable<RotationMatchRoute> { RotationMatchRoute(koinViewModel<RotationMatchViewModel>(), onBackClick) }
}
