package com.alad1nks.oquturbo.feature.baspagame.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry
import com.alad1nks.oquturbo.core.ui.navigation.enumNavType
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameContent
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameMode
import com.alad1nks.oquturbo.feature.baspagame.model.Category
import com.alad1nks.oquturbo.feature.baspagame.model.GameColor
import com.alad1nks.oquturbo.feature.baspagame.ui.BaspaGameRoute
import com.alad1nks.oquturbo.feature.baspagame.ui.BaspaGameViewModel
import com.alad1nks.oquturbo.resources.AppResource
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringArrayResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.reflect.typeOf

@Serializable
data class BaspaGameRoute(
    val mode: BaspaGameMode,
    val trainingEntryId: String? = null,
    val trainingRequiredScore: Int? = null,
)

private val baspaGameTypeMap =
    mapOf(typeOf<BaspaGameMode>() to enumNavType<BaspaGameMode>())

fun NavController.navigateToBaspaGame(mode: BaspaGameMode) {
    navigate(BaspaGameRoute(mode))
}

fun NavController.navigateToBaspaTraining(
    mode: BaspaGameMode,
    trainingEntryId: String,
    trainingRequiredScore: Int,
) {
    require(trainingEntryId.isNotBlank()) { "Training entry id must not be blank" }
    require(trainingRequiredScore > 0) { "Training required score must be positive" }
    navigate(
        BaspaGameRoute(
            mode = mode,
            trainingEntryId = trainingEntryId,
            trainingRequiredScore = trainingRequiredScore,
        ),
    )
}

fun NavGraphBuilder.baspaGameScreen(onBackClick: () -> Unit) {
    baspaGameScreen(
        onBackClick = onBackClick,
        onTrainingBackClick = onBackClick,
        onTrainingContinue = { onBackClick() },
    )
}

fun NavGraphBuilder.baspaGameScreen(
    onBackClick: () -> Unit,
    onTrainingBackClick: () -> Unit,
    onTrainingContinue: (DailyTrainingEntry?) -> Unit,
) {
    composable<BaspaGameRoute>(typeMap = baspaGameTypeMap) { entry ->
        val route = entry.toRoute<BaspaGameRoute>()
        require((route.trainingEntryId == null) == (route.trainingRequiredScore == null)) {
            "Baspa training id and required score must be provided together"
        }
        val isTraining = route.trainingEntryId != null
        val content = baspaGameContent()
        val viewModel =
            koinViewModel<BaspaGameViewModel>(
                parameters = {
                    parametersOf(
                        route.mode,
                        content,
                        route.trainingEntryId,
                        route.trainingRequiredScore,
                    )
                },
            )
        BaspaGameRoute(
            viewModel = viewModel,
            onBackClick = if (isTraining) onTrainingBackClick else onBackClick,
            onTrainingContinue = onTrainingContinue,
        )
    }
}

@androidx.compose.runtime.Composable
private fun baspaGameContent(): BaspaGameContent {
    val categories = stringArrayResource(AppResource.Array.baspa_categories).map(String::toCategory)
    return BaspaGameContent(
        categories = categories,
        letters = stringArrayResource(AppResource.Array.baspa_letters),
        wordLengths = stringArrayResource(AppResource.Array.baspa_word_lengths).map(String::toInt),
        colors = stringArrayResource(AppResource.Array.baspa_colors).map(String::toGameColor),
        allWords =
            (stringArrayResource(AppResource.Array.baspa_all_words) + categories.flatMap { it.words }).distinct(),
        statements = stringArrayResource(AppResource.Array.baspa_statements).map(String::toBooleanPair),
        equations = stringArrayResource(AppResource.Array.baspa_equations).map(String::toBooleanPair),
    )
}

private fun String.toGameColor(): GameColor {
    val parts = split('|', limit = 3)
    require(parts.size == 3) { "Invalid Baspa color item: $this" }
    return GameColor(id = parts[0], name = parts[1], word = parts[2])
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
