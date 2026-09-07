package com.alad1nks.oquturbo.feature.memorygrid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
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
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridGameMode
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridPhase
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridState
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class MemoryGridDurationSemanticsTest {
    @Test
    fun everyModeShowsDurationOnlyInGameOver() {
        MemoryGridGameMode.entries.forEach { mode ->
            MemoryGridPhase.entries.forEach { phase ->
                runComposeUiTest {
                    setContent {
                        OquTurboTheme {
                            MemoryGridScreen(
                                state =
                                    MemoryGridState(
                                        phase = phase,
                                        sequence = listOf(0, 1, 2, 3),
                                        presentationIndex = 0,
                                        score = 3,
                                        input = listOf(0, 1),
                                        correctCellCount = 5,
                                        completedDurationMillis = 725_300,
                                    ),
                                mode = mode,
                                onStartClick = {},
                                onCellClick = {},
                                onBackClick = {},
                            )
                        }
                    }
                    val total = onNodeWithText("Duration: 12 minutes 5 seconds")
                    if (phase == MemoryGridPhase.GameOver) {
                        total.performScrollTo().assertIsDisplayed()
                    } else {
                        total.assertDoesNotExist()
                    }
                    if (phase == MemoryGridPhase.AwaitingInput) {
                        onNodeWithContentDescription("Accepted taps: 2").assertExists()
                    }
                }
            }
        }
    }

    @Test
    fun compactLocalizedDurationWrapsAndRetryAndBackRemainReachable() {
        val originalLocale = Locale.getDefault()
        try {
            listOf(
                Triple("en", "Duration: 12345 minutes 25 seconds", "Try again"),
                Triple("ru", "Время: 12345 минут 25 секунд", "Попробовать снова"),
                Triple("kk", "Уақыт: 12345 минут 25 секунд", "Қайта көру"),
            ).forEach { (locale, label, retry) ->
                Locale.setDefault(Locale.forLanguageTag(locale))
                runDesktopComposeUiTest(width = 320, height = 844) {
                    var retries = 0
                    var backs = 0
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(1f)) {
                            OquTurboTheme {
                                Box(Modifier.requiredSize(320.dp, 844.dp)) {
                                    MemoryGridScreen(
                                        state =
                                            MemoryGridState(
                                                phase = MemoryGridPhase.GameOver,
                                                correctCellCount = Int.MAX_VALUE,
                                                completedDurationMillis = 740_725_000,
                                            ),
                                        mode = MemoryGridGameMode.Route,
                                        onStartClick = { retries++ },
                                        onCellClick = {},
                                        onBackClick = { backs++ },
                                    )
                                }
                            }
                        }
                    }
                    val total = onNodeWithText(label)
                    total.performScrollTo().assertIsDisplayed()
                    val layouts = mutableListOf<TextLayoutResult>()
                    total.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    assertFalse(layouts.single().hasVisualOverflow)
                    onNodeWithText(retry).performScrollTo().assertIsDisplayed().performClick()
                    assertEquals(1, retries)
                    val back =
                        when (locale) {
                            "ru" -> "Назад"
                            "kk" -> "Артқа"
                            else -> "Back"
                        }
                    onNodeWithContentDescription(back).performClick()
                    assertEquals(1, backs)
                }
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun russianPluralCategoriesAndLargeLongAreLocalized() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ru"))
            listOf(
                1L to "минута",
                2L to "минуты",
                5L to "минут",
                11L to "минут",
                21L to "минута",
                3_000_000_021L to "минута",
            ).forEach {
                (minutes, unit) ->
                runComposeUiTest {
                    setContent {
                        androidx.compose.material3.Text(memoryGridDurationText(minutes * 60_000))
                    }
                    onNodeWithText("$minutes $unit").assertExists()
                }
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun missingMeasurementDoesNotInventDuration() =
        runComposeUiTest {
            setContent {
                OquTurboTheme {
                    MemoryGridScreen(
                        state = MemoryGridState(phase = MemoryGridPhase.GameOver),
                        mode = MemoryGridGameMode.Flash,
                        onStartClick = {},
                        onCellClick = {},
                        onBackClick = {},
                    )
                }
            }
            onNodeWithText("Duration:", substring = true).assertDoesNotExist()
        }
}
