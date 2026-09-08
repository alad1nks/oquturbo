package com.alad1nks.oquturbo.feature.kenkozgame.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class KenKozDurationSemanticsTest {
    @Test
    fun allModesShowZeroDurationAndPreserveFeedback() {
        KenKozGameMode.entries.forEach { mode ->
            runComposeUiTest {
                setContent {
                    OquTurboTheme {
                        KenKozGameScreen(
                            result(mode),
                            {},
                            {},
                            {},
                            {},
                        )
                    }
                }
                onNodeWithText("Duration: less than 1 second", useUnmergedTree = true).assertExists()
                onNodeWithText("Score: 0", useUnmergedTree = true).assertExists()
                onNodeWithText(
                    if (mode == KenKozGameMode.FindDifference) "on top" else "mountain",
                    useUnmergedTree = true,
                )
                    .assertExists()
                onNodeWithText(
                    if (mode == KenKozGameMode.FindDifference) "on the left" else "morning",
                    useUnmergedTree = true,
                )
                    .assertExists()
            }
        }
    }

    @Test
    fun activeAndMissingDurationStatesHaveNoInventedValue() {
        KenKozGameUiState.Phase.entries.forEach { phase ->
            runComposeUiTest {
                setContent {
                    OquTurboTheme {
                        KenKozGameScreen(
                            result().copy(phase = phase, completedDurationMillis = null),
                            {},
                            {},
                            {},
                            {},
                        )
                    }
                }
                onNodeWithText("Duration:", substring = true, useUnmergedTree = true).assertDoesNotExist()
            }
        }
    }

    @Test
    fun trainingReadinessPreservesRetryAndContinueActions() {
        listOf(false, true).forEach { ready ->
            listOf(0, 5).forEach { score ->
                runComposeUiTest {
                    var retries = 0
                    var continuations = 0
                    setContent {
                        OquTurboTheme {
                            KenKozGameScreen(
                                result().copy(
                                    score = score,
                                    trainingRequiredScore = 5,
                                    isTrainingCompletionReady = ready,
                                    completedDurationMillis = 1_999,
                                ),
                                onBackClick = {},
                                onStartClick = { retries++ },
                                onTrainingContinueClick = { continuations++ },
                                onAnswerClick = {},
                            )
                        }
                    }
                    onNodeWithText("Duration: 1 second", useUnmergedTree = true).assertExists()
                    onNodeWithText(
                        if (score == 5) "Goal reached" else "Score at least 5 points to continue",
                        useUnmergedTree = true,
                    ).performClick()
                    assertEquals(if (ready && score == 0) 1 else 0, retries)
                    assertEquals(if (ready && score == 5) 1 else 0, continuations)
                }
            }
        }
    }

    @Test
    fun narrowLocalizedDurationScrollsWithoutRetryAndActionsRemainReachable() {
        withLocales(listOf("en", "ru", "kk")) { locale ->
            runDesktopComposeUiTest(width = 320, height = 640) {
                var retries = 0
                var backs = 0
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        OquTurboTheme {
                            KenKozGameScreen(
                                result().copy(
                                    score = 8,
                                    record = 8,
                                    isNewRecord = true,
                                    completedDurationMillis = Long.MAX_VALUE,
                                ),
                                onBackClick = { backs++ },
                                onStartClick = { retries++ },
                                onTrainingContinueClick = {},
                                onAnswerClick = {},
                            )
                        }
                    }
                }
                val duration =
                    when (locale) {
                        "ru" -> "Время: 153722867280912 минут 55 секунд"
                        "kk" -> "Уақыт: 153722867280912 минут 55 секунд"
                        else -> "Duration: 153722867280912 minutes 55 seconds"
                    }
                val node = onNodeWithText(duration, useUnmergedTree = true)
                node.performScrollTo().assertIsDisplayed()
                val layouts = mutableListOf<TextLayoutResult>()
                node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertFalse(layouts.single().hasVisualOverflow)
                assertEquals(0, retries)
                onNodeWithText("mountain", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
                onNodeWithText("morning", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
                val retry =
                    when (locale) {
                        "ru" -> "Попробовать снова?"
                        "kk" -> "Қайта байқап көресіз бе?"
                        else -> "Try Again?"
                    }
                onNodeWithText(retry, useUnmergedTree = true).performScrollTo().performClick()
                assertEquals(1, retries)
                onNodeWithContentDescription(
                    when (locale) {
                        "ru" -> "Назад"
                        "kk" -> "Артқа"
                        else -> "Back"
                    },
                ).performClick()
                assertEquals(1, backs)
            }
        }
    }

    @Test
    fun localizedSecondsAndMinutesPreserveAllIntegerPluralCategories() {
        withLocales(listOf("en", "ru", "kk")) { locale ->
            val values = listOf(1L, 2L, 5L, 11L, 21L, 22L)
            val seconds =
                when (locale) {
                    "ru" -> listOf("секунда", "секунды", "секунд", "секунд", "секунда", "секунды")
                    "kk" -> List(6) { "секунд" }
                    else -> listOf("second") + List(5) { "seconds" }
                }
            val minutes =
                when (locale) {
                    "ru" -> listOf("минута", "минуты", "минут", "минут", "минута", "минуты")
                    "kk" -> List(6) { "минут" }
                    else -> listOf("minute") + List(5) { "minutes" }
                }
            val zeroSeconds = if (locale == "en") "seconds" else "секунд"
            val examples =
                values.mapIndexed { i, n -> n * 1_000 to "$n ${seconds[i]}" } +
                    values.mapIndexed { i, n -> n * 60_000 to "$n ${minutes[i]} 0 $zeroSeconds" } +
                    listOf(
                        180_000_001_260_000L to
                            when (locale) {
                                "ru" -> "3000000021 минута 0 секунд"
                                "kk" -> "3000000021 минут 0 секунд"
                                else -> "3000000021 minutes 0 seconds"
                            },
                    )
            examples.forEach { (millis, expected) ->
                runComposeUiTest {
                    setContent { Text(kenKozDurationText(millis)) }
                    onNodeWithText(expected).assertExists()
                }
            }
        }
    }

    private fun result(mode: KenKozGameMode = KenKozGameMode.Words) =
        KenKozGameUiState(
            mode = mode,
            phase = KenKozGameUiState.Phase.Mistake,
            completedDurationMillis = 0,
            questionDirection = KenKozGameUiState.Direction.Top,
            correctAnswer = if (mode == KenKozGameMode.FindDifference) "Top" else "mountain",
            selectedAnswer = if (mode == KenKozGameMode.FindDifference) "Left" else "morning",
        )

    private fun withLocales(locales: List<String>, block: (String) -> Unit) {
        val original = Locale.getDefault()
        try {
            locales.forEach { locale ->
                Locale.setDefault(Locale.forLanguageTag(locale))
                block(locale)
            }
        } finally {
            Locale.setDefault(original)
        }
    }
}
