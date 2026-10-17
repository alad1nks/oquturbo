package com.alad1nks.oquturbo.feature.numbertrail.ui

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailBoard
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailFailure
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailPhase
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailState
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NumberTrailSummarySemanticsTest {
    @Test
    fun summaryOnlyAppearsInResultAndRemainsFrozenAcrossSaveFeedback() =
        runComposeUiTest {
            val state = mutableStateOf(result())
            setContent {
                OquTurboTheme {
                    NumberTrailScreen(state.value, {}, { _, _ -> }, null)
                }
            }
            for (failure in NumberTrailFailure.entries) {
                for (save in listOf(
                    NumberTrailSaveStatus.Pending,
                    NumberTrailSaveStatus.Saved,
                    NumberTrailSaveStatus.Failed,
                )) {
                    state.value = result().copy(game = result().game.copy(failure = failure), saveStatus = save)
                    onNodeWithText("Completed boards: 2").assertExists()
                    onNodeWithText("Active time: 1 minute 5 seconds").assertExists()
                    if (save == NumberTrailSaveStatus.Failed) {
                        onNodeWithText(
                            "Couldn’t save this result.",
                        ).assertExists()
                    }
                    if (save == NumberTrailSaveStatus.Pending) onNodeWithText("Saving result…").assertExists()
                    mainClock.advanceTimeBy(1_000)
                    onNodeWithText("Active time: 1 minute 5 seconds").assertExists()
                }
            }
            for (phase in NumberTrailPhase.entries.filter { it != NumberTrailPhase.Result }) {
                state.value = result().copy(game = result().game.copy(phase = phase))
                onNodeWithText("Completed boards:", substring = true).assertDoesNotExist()
                onNodeWithText("Active time:", substring = true).assertDoesNotExist()
            }
            state.value = result().copy(game = result().game.copy(completedBoards = 0, activeDurationMillis = 999))
            onNodeWithText("Completed boards: 0").assertExists()
            onNodeWithText("Active time: less than 1 second").assertExists()
            onNodeWithText("Back").assertDoesNotExist()
        }

    @Test
    fun compactLocalizedSummaryAndBothActionsAreReachable() {
        val original = Locale.getDefault()
        try {
            for ((locale, copy) in mapOf(
                "en" to listOf("Completed boards: 2", "Active time: 125 minutes 5 seconds", "Try again", "Back"),
                "ru" to listOf("Завершено полей: 2", "Активное время: 125 минут 5 секунд", "Ещё раз", "Назад"),
                "kk" to listOf("Аяқталған тақталар: 2", "Белсенді уақыт: 125 минут 5 секунд", "Қайта ойнау", "Артқа"),
            )) {
                Locale.setDefault(Locale.forLanguageTag(locale))
                runComposeUiTest {
                    var retries = 0
                    var backs = 0
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                            OquTurboTheme(darkTheme = locale == "ru") {
                                NumberTrailScreen(
                                    result().copy(
                                        game = result().game.copy(activeDurationMillis = 7_505_000),
                                        saveStatus = NumberTrailSaveStatus.Failed,
                                    ),
                                    { retries++ },
                                    { _, _ -> },
                                    { backs++ },
                                    Modifier.requiredSize(320.dp, 600.dp),
                                )
                            }
                        }
                    }
                    onNodeWithText(copy[0]).performScrollTo().assertIsDisplayed()
                    onNodeWithText(copy[1]).performScrollTo().assertIsDisplayed()
                    onNodeWithText(copy[2]).performScrollTo().assertIsDisplayed().performClick()
                    onNodeWithText(copy[3]).performScrollTo().assertIsDisplayed().performClick()
                    assertEquals(1, retries)
                    assertEquals(1, backs)
                }
            }
        } finally {
            Locale.setDefault(original)
        }
    }

    private fun result() =
        NumberTrailUiState(
            game =
                NumberTrailState(
                    phase = NumberTrailPhase.Result,
                    board = NumberTrailBoard(3, 2, listOf(4, 1, 3, 2), 12_000, target = 3),
                    score = 10,
                    completedBoards = 2,
                    activeDurationMillis = 65_300,
                    failure = NumberTrailFailure.Wrong,
                    selectedNumber = 4,
                ),
            isRecordLoading = false,
            saveStatus = NumberTrailSaveStatus.Saved,
        )
}
