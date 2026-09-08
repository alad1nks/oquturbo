package com.alad1nks.oquturbo.feature.remembernumber.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class RememberNumberDurationSemanticsTest {
    @Test
    fun durationIsAbsentInActiveAndMissingStatesButKnownZeroIsShown() {
        listOf(
            RememberNumberUiState.Initial(),
            RememberNumberUiState.Reading("1234", 0),
            RememberNumberUiState.Writing("", 0),
            RememberNumberUiState.Mistake("1234", 0, "1334", 0),
            RememberNumberUiState.Mistake("1234", 0, "1334", 0, completedDurationMillis = 0),
        ).forEach { state ->
            runComposeUiTest {
                setContent {
                    OquTurboTheme {
                        RememberNumberScreen(state, null, 4, writeText = {}, onStartClick = {}, onBackClick = {})
                    }
                }
                if (state is RememberNumberUiState.Mistake && state.completedDurationMillis != null) {
                    onNodeWithText("Duration: less than 1 second", useUnmergedTree = true).assertExists()
                } else {
                    onNodeWithText("Duration:", substring = true, useUnmergedTree = true).assertDoesNotExist()
                }
            }
        }
    }

    @Test
    fun compactLocalizedAnswersDurationSummaryAndActionsRemainReachable() {
        val original = Locale.getDefault()
        try {
            listOf("ru", "kk").forEach { locale ->
                Locale.setDefault(Locale.forLanguageTag(locale))
                runDesktopComposeUiTest(width = 320, height = 844) {
                    var retries = 0
                    var continues = 0
                    var backs = 0
                    val kazakh = locale == "kk"
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(1f)) {
                            OquTurboTheme {
                                Box(Modifier.requiredSize(320.dp, 844.dp)) {
                                    RememberNumberScreen(
                                        uiState =
                                            RememberNumberUiState.Mistake(
                                                "0123456789",
                                                8,
                                                "9123456780",
                                                12,
                                                completedDurationMillis = if (kazakh) Long.MAX_VALUE else 740_725_000,
                                            ),
                                        focusEvent = null,
                                        maxLength = 10,
                                        record = 12,
                                        trainingRequiredScore = if (kazakh) 5 else null,
                                        writeText = {},
                                        onStartClick = { retries++ },
                                        onTrainingContinueClick = { continues++ },
                                        onBackClick = { backs++ },
                                    )
                                }
                            }
                        }
                    }
                    val duration =
                        if (kazakh) "Уақыт: 153722867280912 минут 55 секунд" else "Время: 12345 минут 25 секунд"
                    val total = onNodeWithText(duration, useUnmergedTree = true)
                    total.performScrollTo().assertIsDisplayed()
                    val layouts = mutableListOf<TextLayoutResult>()
                    total.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    assertFalse(layouts.single().hasVisualOverflow)
                    listOf(
                        if (kazakh) "Сіздің жауабыңыз: 0 1 2 3 4 5 6 7 8 9" else "Ваш ответ: 0 1 2 3 4 5 6 7 8 9",
                        if (kazakh) "Дұрыс жауап: 9 1 2 3 4 5 6 7 8 0" else "Правильный ответ: 9 1 2 3 4 5 6 7 8 0",
                    ).forEach {
                        onNodeWithContentDescription(
                            it,
                            useUnmergedTree = true,
                        ).performScrollTo().assertIsDisplayed()
                    }
                    onNodeWithText(
                        if (kazakh) "Ұпай: 8" else "Счёт: 8",
                        useUnmergedTree = true,
                    ).performScrollTo().assertIsDisplayed()
                    onNodeWithText("Рекорд: 12", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
                    onNodeWithText(if (kazakh) "Мақсат орындалды" else "Попробовать снова?", useUnmergedTree = true)
                        .performScrollTo().assertIsDisplayed().performClick()
                    assertEquals(if (kazakh) 0 else 1, retries)
                    assertEquals(if (kazakh) 1 else 0, continues)
                    onNodeWithContentDescription(if (kazakh) "Артқа" else "Назад").performClick()
                    assertEquals(1, backs)
                }
            }
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun pendingTrainingShowsDurationAndBlocksActionUntilReady() {
        listOf(false, true).forEach { ready ->
            runComposeUiTest {
                var retries = 0
                setContent {
                    OquTurboTheme {
                        RememberNumberScreen(
                            RememberNumberUiState.Mistake(
                                "1010",
                                0,
                                "1110",
                                3,
                                isTrainingResultReady = ready,
                                completedDurationMillis = 1_999,
                            ),
                            null,
                            4,
                            trainingRequiredScore = 5,
                            writeText = {},
                            onStartClick = { retries++ },
                            onBackClick = {},
                        )
                    }
                }
                onNodeWithText("Duration: 1 second", useUnmergedTree = true).assertExists()
                onNodeWithText("Score at least 5 points to continue", useUnmergedTree = true).performClick()
                assertEquals(if (ready) 1 else 0, retries)
            }
        }
    }

    @Test
    fun localizedFormatterPreservesPluralCategoriesAndExactMinuteSeconds() {
        val original = Locale.getDefault()
        try {
            mapOf(
                "en" to
                    listOf(
                        0L to "less than 1 second",
                        65_999L to "1 minute 5 seconds",
                        120_000L to "2 minutes 0 seconds",
                    ),
                "ru" to
                    listOf(
                        60_000L to "1 минута 0 секунд",
                        120_000L to "2 минуты 0 секунд",
                        660_000L to "11 минут 0 секунд",
                        180_000_001_260_000L to "3000000021 минута 0 секунд",
                    ),
                "kk" to listOf(999L to "1 секундтан аз", 60_000L to "1 минут 0 секунд"),
            ).forEach { (locale, examples) ->
                Locale.setDefault(Locale.forLanguageTag(locale))
                examples.forEach { (millis, text) ->
                    runComposeUiTest {
                        setContent { Text(rememberNumberDurationText(millis)) }
                        onNodeWithText(text).assertExists()
                    }
                }
            }
        } finally {
            Locale.setDefault(original)
        }
    }
}
