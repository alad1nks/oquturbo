package com.alad1nks.oquturbo.feature.remembernumber.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry
import com.alad1nks.oquturbo.feature.remembernumber.ui.RememberNumberRoute
import com.alad1nks.oquturbo.feature.remembernumber.ui.RememberNumberViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class RememberNumberRoute(
    val maxLength: Int,
    val availableDigits: String,
    val trainingEntryId: String? = null,
    val trainingRequiredScore: Int? = null,
)

fun NavController.navigateToRememberNumber(
    maxLength: Int,
    availableDigits: String,
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(
        route =
            RememberNumberRoute(
                maxLength = maxLength,
                availableDigits = availableDigits,
            ),
        builder = navOptions,
    )
}

fun NavController.navigateToRememberNumberTraining(
    maxLength: Int,
    availableDigits: String,
    trainingEntryId: String,
    trainingRequiredScore: Int,
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    require(trainingEntryId.isNotBlank()) { "Training entry id must not be blank" }
    require(trainingRequiredScore > 0) { "Training required score must be positive" }
    navigate(
        route =
            RememberNumberRoute(
                maxLength = maxLength,
                availableDigits = availableDigits,
                trainingEntryId = trainingEntryId,
                trainingRequiredScore = trainingRequiredScore,
            ),
        builder = navOptions,
    )
}

fun NavGraphBuilder.rememberNumberScreen(
    onBackClick: () -> Unit,
) {
    rememberNumberScreen(
        onBackClick = onBackClick,
        onTrainingBackClick = onBackClick,
        onTrainingContinue = { onBackClick() },
    )
}

fun NavGraphBuilder.rememberNumberScreen(
    onBackClick: () -> Unit,
    onTrainingBackClick: () -> Unit,
    onTrainingContinue: (DailyTrainingEntry?) -> Unit,
) {
    composable<RememberNumberRoute> { entry ->
        val route = entry.toRoute<RememberNumberRoute>()
        require((route.trainingEntryId == null) == (route.trainingRequiredScore == null)) {
            "Number Sprint training id and required score must be provided together"
        }

        val viewModel =
            koinViewModel<RememberNumberViewModel>(
                parameters = {
                    parametersOf(
                        route.maxLength,
                        route.availableDigits,
                        route.trainingEntryId,
                        route.trainingRequiredScore,
                    )
                },
            )

        RememberNumberRoute(
            viewModel = viewModel,
            onBackClick = if (route.trainingEntryId == null) onBackClick else onTrainingBackClick,
            onTrainingContinue = onTrainingContinue,
        )
    }
}
