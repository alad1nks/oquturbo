package com.alad1nks.oquturbo.feature.wordflow.navigation

import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowContent
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowPrompt
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowTier
import com.alad1nks.oquturbo.feature.wordflow.model.normalizeWordFlowLocale
import com.alad1nks.oquturbo.feature.wordflow.ui.WordFlowRoute
import com.alad1nks.oquturbo.feature.wordflow.ui.WordFlowViewModel
import kotlinx.serialization.Serializable
import oquturbo.feature.wordflow.generated.resources.Res
import oquturbo.feature.wordflow.generated.resources.word_flow_easy_correct
import oquturbo.feature.wordflow.generated.resources.word_flow_easy_templates
import oquturbo.feature.wordflow.generated.resources.word_flow_easy_wrong_a
import oquturbo.feature.wordflow.generated.resources.word_flow_easy_wrong_b
import oquturbo.feature.wordflow.generated.resources.word_flow_hard_correct
import oquturbo.feature.wordflow.generated.resources.word_flow_hard_templates
import oquturbo.feature.wordflow.generated.resources.word_flow_hard_wrong_a
import oquturbo.feature.wordflow.generated.resources.word_flow_hard_wrong_b
import oquturbo.feature.wordflow.generated.resources.word_flow_medium_correct
import oquturbo.feature.wordflow.generated.resources.word_flow_medium_templates
import oquturbo.feature.wordflow.generated.resources.word_flow_medium_wrong_a
import oquturbo.feature.wordflow.generated.resources.word_flow_medium_wrong_b
import org.jetbrains.compose.resources.stringArrayResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data object WordFlowRoute

fun NavController.navigateToWordFlow(navOptions: NavOptionsBuilder.() -> Unit = {}) =
    navigate(WordFlowRoute, navOptions)

fun NavGraphBuilder.wordFlowScreen(onBackClick: (() -> Unit)? = null) {
    composable<WordFlowRoute> {
        val locale = normalizeWordFlowLocale(Locale.current.language)
        val easy = localizedTier(WordFlowTier.Easy)
        val medium = localizedTier(WordFlowTier.Medium)
        val hard = localizedTier(WordFlowTier.Hard)
        val content = remember(locale, easy, medium, hard) { WordFlowContent(easy + medium + hard) }
        val viewModel =
            koinViewModel<WordFlowViewModel>(
                parameters = { parametersOf(locale, content) },
            )
        WordFlowRoute(viewModel, onBackClick)
    }
}

@androidx.compose.runtime.Composable
private fun localizedTier(tier: WordFlowTier): List<WordFlowPrompt> {
    val templates =
        stringArrayResource(
            when (tier) {
                WordFlowTier.Easy -> Res.array.word_flow_easy_templates
                WordFlowTier.Medium -> Res.array.word_flow_medium_templates
                WordFlowTier.Hard -> Res.array.word_flow_hard_templates
            },
        )
    val correct =
        stringArrayResource(
            when (tier) {
                WordFlowTier.Easy -> Res.array.word_flow_easy_correct
                WordFlowTier.Medium -> Res.array.word_flow_medium_correct
                WordFlowTier.Hard -> Res.array.word_flow_hard_correct
            },
        )
    val wrongA =
        stringArrayResource(
            when (tier) {
                WordFlowTier.Easy -> Res.array.word_flow_easy_wrong_a
                WordFlowTier.Medium -> Res.array.word_flow_medium_wrong_a
                WordFlowTier.Hard -> Res.array.word_flow_hard_wrong_a
            },
        )
    val wrongB =
        stringArrayResource(
            when (tier) {
                WordFlowTier.Easy -> Res.array.word_flow_easy_wrong_b
                WordFlowTier.Medium -> Res.array.word_flow_medium_wrong_b
                WordFlowTier.Hard -> Res.array.word_flow_hard_wrong_b
            },
        )
    require(setOf(templates.size, correct.size, wrongA.size, wrongB.size) == setOf(6)) {
        "Word Flow ${tier.name} prompt arrays must contain six aligned entries"
    }
    return templates.indices.map { index ->
        WordFlowPrompt(
            id = "${tier.name.lowercase()}-${index + 1}",
            tier = tier,
            sentenceTemplate = templates[index],
            correctAnswer = correct[index],
            wrongAnswers = listOf(wrongA[index], wrongB[index]),
        )
    }
}
