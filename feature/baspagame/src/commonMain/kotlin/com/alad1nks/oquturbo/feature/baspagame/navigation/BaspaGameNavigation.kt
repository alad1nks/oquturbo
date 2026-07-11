package com.alad1nks.oquturbo.feature.baspagame.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameContent
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameMode
import com.alad1nks.oquturbo.feature.baspagame.ui.BaspaGameRoute
import com.alad1nks.oquturbo.feature.baspagame.ui.BaspaGameViewModel
import com.alad1nks.oquturbo.resources.AppResource
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.stringArrayResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable data class BaspaGameRoute(val mode: BaspaGameMode)

fun NavController.navigateToBaspaGame(mode: BaspaGameMode) {
    navigate(BaspaGameRoute(mode))
}

fun NavGraphBuilder.baspaGameScreen(onBackClick: () -> Unit) {
    composable<BaspaGameRoute> { entry ->
        val mode = entry.toRoute<BaspaGameRoute>().mode
        val content = baspaGameContent(mode)
        val viewModel =
            koinViewModel<BaspaGameViewModel>(
                parameters = { parametersOf(mode, content) },
            )
        BaspaGameRoute(viewModel, onBackClick)
    }
}

@androidx.compose.runtime.Composable
private fun baspaGameContent(mode: BaspaGameMode): BaspaGameContent {
    val matchingResource: StringArrayResource
    val otherResource: StringArrayResource
    when (mode) {
        BaspaGameMode.Letter -> {
            matchingResource = AppResource.Array.baspa_letter_matching
            otherResource = AppResource.Array.baspa_letter_other
        }
        BaspaGameMode.WordLength -> {
            matchingResource = AppResource.Array.baspa_length_matching
            otherResource = AppResource.Array.baspa_length_other
        }
        else -> {
            matchingResource = AppResource.Array.baspa_categories_matching
            otherResource = AppResource.Array.baspa_categories_other
        }
    }
    return BaspaGameContent(
        matchingWords = stringArrayResource(matchingResource),
        otherWords = stringArrayResource(otherResource),
        statements = stringArrayResource(AppResource.Array.baspa_statements).map(String::toBooleanPair),
        equations = stringArrayResource(AppResource.Array.baspa_equations).map(String::toBooleanPair),
    )
}

private fun String.toBooleanPair(): Pair<String, Boolean> {
    val parts = split('|')
    require(parts.size == 2) { "Invalid Baspa boolean item: $this" }
    return parts[0] to parts[1].toBooleanStrict()
}
