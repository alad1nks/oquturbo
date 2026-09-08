package com.alad1nks.oquturbo.feature.numbertrail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.numbertrail.ui.NumberTrailRoute
import com.alad1nks.oquturbo.feature.numbertrail.ui.NumberTrailViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object NumberTrailRoute

fun NavController.navigateToNumberTrail(
    navOptions: NavOptionsBuilder.() -> Unit = {
    },
) = navigate(NumberTrailRoute, navOptions)

fun NavGraphBuilder.numberTrailScreen(onBackClick: (() -> Unit)? = null) {
    composable<NumberTrailRoute> { NumberTrailRoute(koinViewModel<NumberTrailViewModel>(), onBackClick) }
}
