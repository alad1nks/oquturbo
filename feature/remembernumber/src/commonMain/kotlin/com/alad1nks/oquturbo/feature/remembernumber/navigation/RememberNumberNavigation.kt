package com.alad1nks.oquturbo.feature.remembernumber.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.alad1nks.oquturbo.feature.remembernumber.RememberNumberRoute
import com.alad1nks.oquturbo.feature.remembernumber.RememberNumberViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable data class RememberNumberRoute(
    val maxLength: Int,
)

fun NavController.navigateToRememberNumber(
    maxLength: Int,
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(
        route = RememberNumberRoute(maxLength),
        builder = navOptions,
    )
}

fun NavGraphBuilder.rememberNumberScreen() {
    composable<RememberNumberRoute> { entry ->
        val maxLength = entry.toRoute<RememberNumberRoute>().maxLength

        val viewModel =
            koinViewModel<RememberNumberViewModel>(
                parameters = { parametersOf(maxLength) },
            )

        RememberNumberRoute(
            viewModel = viewModel,
        )
    }
}
