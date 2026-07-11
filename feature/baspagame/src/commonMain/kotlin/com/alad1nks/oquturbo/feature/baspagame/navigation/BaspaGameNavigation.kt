package com.alad1nks.oquturbo.feature.baspagame.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameContent
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameMode
import com.alad1nks.oquturbo.feature.baspagame.model.Category
import com.alad1nks.oquturbo.feature.baspagame.model.LetterChallenge
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
    val categories = stringArrayResource(AppResource.Array.baspa_categories).map(String::toCategory)
    val matchingResource: StringArrayResource?
    val otherResource: StringArrayResource?
    when (mode) {
        BaspaGameMode.WordLength -> {
            matchingResource = AppResource.Array.baspa_length_matching
            otherResource = AppResource.Array.baspa_length_other
        }
        else -> {
            matchingResource = null
            otherResource = null
        }
    }
    return BaspaGameContent(
        categories = categories,
        letters = stringArrayResource(AppResource.Array.baspa_letters).map(String::toLetterChallenge),
        matchingWords = matchingResource?.let { stringArrayResource(it) } ?: categories.flatMap { it.words },
        otherWords = otherResource?.let { stringArrayResource(it) }.orEmpty(),
        statements = stringArrayResource(AppResource.Array.baspa_statements).map(String::toBooleanPair),
        equations = stringArrayResource(AppResource.Array.baspa_equations).map(String::toBooleanPair),
    )
}

private fun String.toLetterChallenge(): LetterChallenge {
    val parts = split('|', limit = 3)
    require(parts.size == 3) { "Invalid Baspa letter item: $this" }
    return LetterChallenge(
        letter = parts[0],
        matchingWords = parts[1].split(',').map(String::trim).filter(String::isNotEmpty),
        otherWords = parts[2].split(',').map(String::trim).filter(String::isNotEmpty),
    )
}

private fun String.toCategory(): Category {
    val parts = split('|', limit = 3)
    require(parts.size == 3) { "Invalid Baspa category item: $this" }
    return Category(parts[0], parts[1], parts[2].split(',').map(String::trim).filter(String::isNotEmpty))
}

private fun String.toBooleanPair(): Pair<String, Boolean> {
    val parts = split('|')
    require(parts.size == 2) { "Invalid Baspa boolean item: $this" }
    return parts[0] to parts[1].toBooleanStrict()
}
