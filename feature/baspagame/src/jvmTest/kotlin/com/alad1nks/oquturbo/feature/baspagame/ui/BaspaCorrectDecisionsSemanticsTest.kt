package com.alad1nks.oquturbo.feature.baspagame.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameMode
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class BaspaCorrectDecisionsSemanticsTest {
    @Test
    fun compactLocalizedSummaryAndActionRemainReachableWhileTrainingReadinessChanges() {
        val originalLocale = Locale.getDefault()
        try {
            listOf(
                Triple("en", "Correct decisions: 123", "Includes correct taps and correctly skipped stimuli."),
                Triple(
                    "ru",
                    "Правильные решения: 123",
                    "Включает правильные нажатия и элементы, которые вы правильно пропустили.",
                ),
                Triple(
                    "kk",
                    "Дұрыс шешімдер: 123",
                    "Дұрыс басулар мен дұрыс өткізіп жіберілген элементтер есептеледі.",
                ),
            ).forEach { (locale, label, explanation) ->
                Locale.setDefault(Locale.forLanguageTag(locale))
                runDesktopComposeUiTest(width = 320, height = 640) {
                    val state =
                        mutableStateOf(
                            BaspaGameUiState(
                                mode = BaspaGameMode.TextColor,
                                phase = BaspaGameUiState.Phase.Mistake,
                                mistakeReason = BaspaMistakeReason.MissedMatch,
                                stimulus = "ҚЫЗЫЛ",
                                targetColorName = "қызыл",
                                stimulusColorName = "қызыл",
                                stimulusColorId = "red",
                                shouldTap = true,
                                score = 24,
                                record = 24,
                                completedCorrectAnswers = 123,
                                isNewRecord = true,
                                trainingRequiredScore = 20,
                            ),
                        )
                    var continues = 0
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(1f)) {
                            OquTurboTheme {
                                Box(Modifier.requiredSize(320.dp, 640.dp)) {
                                    BaspaGameScreen(
                                        uiState = state.value,
                                        onBackClick = {},
                                        onPauseClick = {},
                                        onTap = {},
                                        onRestart = {},
                                        onTrainingContinue = { continues++ },
                                    )
                                }
                            }
                        }
                    }
                    listOf(label, explanation).forEach { text ->
                        val node = onNodeWithText(text, useUnmergedTree = true)
                        node.performScrollTo().assertIsDisplayed()
                        val layouts = mutableListOf<TextLayoutResult>()
                        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                        assertFalse(layouts.single().hasVisualOverflow)
                    }
                    val action =
                        when (locale) {
                            "ru" -> "Цель достигнута"
                            "kk" -> "Мақсат орындалды"
                            else -> "Goal reached"
                        }
                    onNodeWithText(action, useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
                    assertEquals(0, continues)
                    runOnIdle { state.value = state.value.copy(isTrainingCompletionReady = true) }
                    onNodeWithText(label, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
                    onNodeWithText(action, useUnmergedTree = true).performScrollTo().performClick()
                    assertEquals(1, continues)
                }
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun completedZeroIsExposedOnlyInResult() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            runDesktopComposeUiTest(width = 320, height = 640) {
                val state =
                    mutableStateOf(
                        BaspaGameUiState(
                            mode = BaspaGameMode.Categories,
                            categoryName = "animals",
                            stimulus = "CAR",
                            isRecordLoaded = true,
                        ),
                    )
                setContent {
                    OquTurboTheme {
                        BaspaGameScreen(
                            uiState = state.value,
                            onBackClick = {},
                            onPauseClick = {},
                            onTap = {},
                            onRestart = {},
                            onTrainingContinue = {},
                        )
                    }
                }
                listOf(
                    BaspaGameUiState.Phase.Initial,
                    BaspaGameUiState.Phase.Playing,
                    BaspaGameUiState.Phase.Paused,
                ).forEach { phase ->
                    runOnIdle { state.value = state.value.copy(phase = phase) }
                    onNodeWithText("Correct decisions:", substring = true).assertDoesNotExist()
                }
                runOnIdle {
                    state.value =
                        state.value.copy(
                            phase = BaspaGameUiState.Phase.Mistake,
                            mistakeReason = BaspaMistakeReason.IncorrectTap,
                            completedCorrectAnswers = 0,
                        )
                }
                onNodeWithText("Correct decisions: 0", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
                onNodeWithText("Includes correct taps and correctly skipped stimuli.", useUnmergedTree = true)
                    .performScrollTo()
                    .assertIsDisplayed()
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
