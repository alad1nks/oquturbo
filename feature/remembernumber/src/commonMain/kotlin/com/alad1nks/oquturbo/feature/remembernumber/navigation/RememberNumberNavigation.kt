package com.alad1nks.oquturbo.feature.remembernumber.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.alad1nks.oquturbo.feature.remembernumber.ui.RememberNumberRoute
import com.alad1nks.oquturbo.feature.remembernumber.ui.RememberNumberViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class RememberNumberRoute(
    val maxLength: Int,
    val availableDigits: String,
)

fun NavController.navigateToRememberNumber(
    maxLength: Int,
    availableDigits: String,
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(
        route = RememberNumberRoute(maxLength, availableDigits),
        builder = navOptions,
    )
}

fun NavGraphBuilder.rememberNumberScreen() {
    composable<RememberNumberRoute> { entry ->
        val maxLength = entry.toRoute<RememberNumberRoute>().maxLength
        val availableDigits = entry.toRoute<RememberNumberRoute>().availableDigits

        val viewModel =
            koinViewModel<RememberNumberViewModel>(
                parameters = { parametersOf(maxLength, availableDigits) },
            )

        RememberNumberRoute(
            viewModel = viewModel,
        )
    }
}
