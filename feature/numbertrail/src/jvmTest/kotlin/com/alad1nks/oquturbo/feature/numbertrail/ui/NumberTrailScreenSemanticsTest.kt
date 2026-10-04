package com.alad1nks.oquturbo.feature.numbertrail.ui

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
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
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class NumberTrailScreenSemanticsTest {
    @Test
    fun resultRolesPreservePositionsAndHaveNoAnswerAction() {
        for (size in 2..5) {
            for (timeout in listOf(false, true)) {
                runComposeUiTest {
                    var answers = 0
                    val numbers = (1..size * size).reversed().toList()
                    val target = size * size - 1
                    setContent {
                        OquTurboTheme {
                            NumberTrailScreen(
                                NumberTrailUiState(
                                    game =
                                        NumberTrailState(
                                            phase = NumberTrailPhase.Result,
                                            board = NumberTrailBoard(77, size, numbers, 30_000, target = target),
                                            failure =
                                                if (timeout) NumberTrailFailure.Timeout else NumberTrailFailure.Wrong,
                                            selectedNumber = size * size,
                                        ),
                                    isRecordLoading = false,
                                ),
                                {},
                                { _, _ -> answers++ },
                                null,
                                Modifier.requiredSize(320.dp, 1800.dp),
                            )
                        }
                    }
                    val bounds =
                        numbers.map { number ->
                            val description =
                                when {
                                    number < target -> "Number $number, already completed"
                                    number == target -> "Number $number, next required number"
                                    !timeout -> "Number $number, your wrong choice"
                                    else -> "Number $number"
                                }
                            val cell = onNodeWithContentDescription(description).assertHasNoClickAction()
                            assertFalse(cell.fetchSemanticsNode().config.contains(SemanticsProperties.Role))
                            cell.performTouchInput { click() }
                            cell.fetchSemanticsNode().boundsInRoot
                        }
                    bounds.chunked(size).forEach { row ->
                        assertEquals(1, row.map { it.top }.distinct().size)
                        row.zipWithNext().forEach { (left, right) -> assertEquals(true, left.right < right.left) }
                    }
                    bounds.chunked(size).zipWithNext().forEach {
                        (top, bottom) ->
                        assertEquals(true, top.first().bottom < bottom.first().top)
                    }
                    assertEquals(0, answers)
                    onNodeWithContentDescription("Pause").assertDoesNotExist()
                    if (timeout) onNodeWithText("Cross: your wrong choice").assertDoesNotExist()
                }
            }
        }
    }

    @Test
    fun reviewSurvivesPersistenceFeedbackAndReplayRemovesIt() =
        runComposeUiTest {
            val game = com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailGame(kotlin.random.Random(14))
            game.start()
            val board = game.state.board!!
            game.answer(board.id, board.numbers.indexOf(4))
            val state = mutableStateOf(NumberTrailUiState(game = game.state, isRecordLoading = false))
            var replays = 0
            setContent {
                OquTurboTheme {
                    NumberTrailScreen(state.value, {
                        replays++
                        game.start()
                        state.value = state.value.copy(game = game.state)
                    }, { _, _ -> }, {}, modifier = Modifier.requiredSize(320.dp, 640.dp))
                }
            }
            for (save in listOf(
                NumberTrailSaveStatus.Pending,
                NumberTrailSaveStatus.Failed,
                NumberTrailSaveStatus.Saved,
            )) {
                state.value = state.value.copy(saveStatus = save)
                onNodeWithContentDescription("Number 1, next required number").performScrollTo().assertIsDisplayed()
                onNodeWithContentDescription("Number 4, your wrong choice").assertExists()
                assertEquals(game.state, state.value.game)
            }
            state.value = state.value.copy(recordLoadFailed = true, isRecordLoading = true)
            onNodeWithText("Retry").performScrollTo().assertIsNotEnabled()
            onNodeWithContentDescription("Number 1, next required number").assertExists()
            state.value = state.value.copy(recordLoadFailed = false, isRecordLoading = false)
            onNodeWithText("Try again").performScrollTo().performClick()
            assertEquals(1, replays)
            onNodeWithText("Board review").assertDoesNotExist()
            onNodeWithContentDescription("Number 1, next required number").assertDoesNotExist()
            onNodeWithContentDescription("Number 1").assertExists()
        }

    @Test
    fun enlargedResultAndActionsAreReachableInAllLocales() {
        val original = Locale.getDefault()
        try {
            for ((locale, expected, replay) in listOf(
                Triple("en", "Number 16, next required number", "Try again"),
                Triple("ru", "Число 16, следующее нужное число", "Ещё раз"),
                Triple("kk", "16 саны, келесі қажетті сан", "Қайта ойнау"),
            )) {
                Locale.setDefault(Locale.forLanguageTag(locale))
                runComposeUiTest {
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                            OquTurboTheme {
                                NumberTrailScreen(
                                    active().copy(
                                        game =
                                            NumberTrailState(
                                                phase = NumberTrailPhase.Result,
                                                board = NumberTrailBoard(7, 4, (1..16).toList(), 30_000, target = 16),
                                                failure = NumberTrailFailure.Timeout,
                                            ),
                                    ),
                                    {},
                                    { _, _ -> },
                                    {},
                                    Modifier.requiredSize(320.dp, 640.dp),
                                )
                            }
                        }
                    }
                    onNodeWithContentDescription(
                        expected,
                    ).performScrollTo().assertIsDisplayed().assertHasNoClickAction()
                    onNodeWithText(replay).performScrollTo().assertIsDisplayed().assertIsEnabled()
                }
            }
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun enlargedShortScreenCanScrollToFinalTileAndBackToPause() =
        runComposeUiTest {
            val game = active().game.copy(board = NumberTrailBoard(1, 4, (1..16).toList(), 30_000), score = 12345)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                    OquTurboTheme {
                        NumberTrailScreen(
                            NumberTrailUiState(game = game, record = 23456, isRecordLoading = false),
                            {},
                            { _, _ -> },
                            {},
                            modifier = Modifier.requiredSize(320.dp, 600.dp),
                        )
                    }
                }
            }
            onNodeWithContentDescription("Number 16").performScrollTo().assertIsDisplayed().assertIsEnabled()
            onNodeWithContentDescription("Pause").performScrollTo().assertIsDisplayed().assertIsEnabled()
        }

    @Test
    fun pauseRemovesGridTargetAndTimerAndResumeRestoresSameNumbers() =
        runComposeUiTest {
            val state = mutableStateOf(active())
            var tapped = -1
            setContent {
                OquTurboTheme {
                    NumberTrailScreen(
                        state.value,
                        {},
                        { _, index -> tapped = index },
                        null,
                        onPauseClick = {
                            state.value =
                                state.value.copy(
                                    game = state.value.game.copy(phase = NumberTrailPhase.Paused),
                                )
                        },
                        onResumeClick = { state.value = active() },
                    )
                }
            }
            onNodeWithContentDescription("Number 1").assertIsNotEnabled()
            onNodeWithContentDescription("Number 2").assertIsEnabled().performClick()
            assertEquals(2, tapped)
            onNodeWithContentDescription("Pause").performClick()
            for (n in 1..4) onNodeWithContentDescription("Number $n").assertDoesNotExist()
            onNodeWithText("Find 2").assertDoesNotExist()
            onNodeWithText("Time left: 12 s").assertDoesNotExist()
            onNodeWithContentDescription("Back").assertDoesNotExist()
            onNodeWithText("Resume").performClick()
            onNodeWithContentDescription("Number 2").assertIsEnabled()
        }

    @Test
    fun completedBoardDisablesEveryTileAndHasNoCountdown() =
        runComposeUiTest {
            val active = active()
            setContent {
                OquTurboTheme {
                    NumberTrailScreen(
                        active.copy(
                            game =
                                active.game.copy(
                                    phase = NumberTrailPhase.BoardComplete,
                                    board = active.game.board!!.copy(target = 5),
                                ),
                        ),
                        {},
                        { _, _ -> },
                        {},
                    )
                }
            }
            for (n in 1..4) onNodeWithContentDescription("Number $n").assertIsNotEnabled()
            onNodeWithText("Board complete").assertExists()
            onNodeWithContentDescription("Pause").assertDoesNotExist()
            onNodeWithText("Time left: 12 s").assertDoesNotExist()
            onNodeWithContentDescription("Back").assertExists()
        }

    @Test
    fun loadingAndFailedRecordReadExposeUsableRecoveryWithoutDeadBack() =
        runComposeUiTest {
            val state = mutableStateOf(NumberTrailUiState())
            var reloads = 0
            setContent {
                OquTurboTheme {
                    NumberTrailScreen(
                        state.value,
                        {},
                        { _, _ -> },
                        null,
                        onReloadClick = { reloads++ },
                    )
                }
            }
            onNodeWithText("Start").assertIsNotEnabled()
            onNodeWithContentDescription("Back").assertDoesNotExist()
            state.value =
                active().copy(
                    game = active().game.copy(phase = NumberTrailPhase.Result, failure = NumberTrailFailure.Timeout),
                    recordLoadFailed = true,
                )
            onNodeWithText("Retry").performScrollTo().assertIsEnabled().performClick()
            assertEquals(1, reloads)
            onNodeWithText("New record!").assertDoesNotExist()
        }

    private fun active() =
        NumberTrailUiState(
            game =
                NumberTrailState(
                    phase = NumberTrailPhase.Active,
                    board = NumberTrailBoard(7, 2, listOf(4, 1, 2, 3), 12_000, target = 2),
                    score = 1,
                ),
            isRecordLoading = false,
        )
}
