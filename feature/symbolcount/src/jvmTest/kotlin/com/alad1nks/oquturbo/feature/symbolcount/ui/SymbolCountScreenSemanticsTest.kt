package com.alad1nks.oquturbo.feature.symbolcount.ui

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.feature.symbolcount.model.CountShape
import com.alad1nks.oquturbo.feature.symbolcount.model.SymbolCountFailure
import com.alad1nks.oquturbo.feature.symbolcount.model.SymbolCountGame
import com.alad1nks.oquturbo.feature.symbolcount.model.SymbolCountPhase
import java.util.Locale
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SymbolCountScreenSemanticsTest {
    @Test
    fun reviewPreservesEveryPositionAndMarksOnlyTargetsAcrossShapesAndSizes() =
        runComposeUiTest {
            val state = mutableStateOf(symbolCountReviewPreviewState(3, SymbolCountFailure.Wrong))
            setContent {
                OquTurboTheme { SymbolCountScreen(state.value, {}, { _, _ -> error("Read-only review") }, null) }
            }
            for (size in 3..5) {
                for (target in CountShape.entries) {
                    val fixture = symbolCountReviewPreviewState(size, SymbolCountFailure.Wrong)
                    val board = requireNotNull(fixture.game.board)
                    val shapes = board.shapes.map { CountShape.entries[(it.ordinal + target.ordinal) % 4] }
                    state.value =
                        fixture.copy(
                            game = fixture.game.copy(board = board.copy(shapes = shapes, target = target)),
                        )
                    onNodeWithTag("review-explanation").assertExists()
                    var marked = 0
                    shapes.forEachIndexed { index, shape ->
                        val row = index / size
                        val column = index % size
                        val match = shape == target
                        val suffix = if (match) "Matches the target." else "Does not match the target."
                        val cell = onNodeWithTag("review-cell-$row-$column")
                        cell.assert(
                            SemanticsMatcher.expectValue(
                                SemanticsProperties.ContentDescription,
                                listOf("Row ${row + 1}, column ${column + 1}: ${shape.name}. $suffix"),
                            ),
                        )
                        cell.assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
                        cell.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
                        val marker = onNodeWithTag("review-match-$row-$column")
                        if (match) {
                            marker.assertExists()
                            marked++
                        } else {
                            marker.assertDoesNotExist()
                        }
                    }
                    assertEquals(state.value.game.board!!.actualCount, marked)
                    assertEquals(
                        if (size == 3) {
                            1
                        } else if (size == 4) {
                            5
                        } else {
                            8
                        },
                        marked,
                    )
                }
            }
        }

    @Test
    fun realFailureAndReplayShowReviewOnlyInResult() =
        runComposeUiTest {
            val game = SymbolCountGame(Random(32))
            val state = mutableStateOf(SymbolCountUiState(isRecordLoading = false))

            fun publish() {
                state.value = state.value.copy(game = game.state, saveStatus = SymbolCountSaveStatus.Saved)
            }
            setContent {
                OquTurboTheme {
                    SymbolCountScreen(state.value, {
                        game.start()
                        publish()
                    }, { id, answer ->
                        game.answer(id, answer)
                        publish()
                    }, null)
                }
            }
            onNodeWithTag("review-explanation").assertDoesNotExist()
            onNodeWithText("Start").performClick()
            onNodeWithTag("review-explanation").assertDoesNotExist()
            val first = requireNotNull(game.state.board)
            onNodeWithTag("answer-${first.options.first { it != first.actualCount }}").performClick()
            onNodeWithTag("review-cell-0-0").assertExists()
            onNodeWithText("Play again").performScrollTo().performClick()
            assertTrue(game.state.board!!.id != first.id)
            onNodeWithTag("review-cell-0-0").assertDoesNotExist()
            onNodeWithTag("review-explanation").assertDoesNotExist()
            val next = requireNotNull(game.state.board)
            onNodeWithTag("answer-${next.actualCount}").performScrollTo().performClick()
            onNodeWithTag("review-explanation").assertDoesNotExist()
            game.pause()
            publish()
            onNodeWithTag("field").assertDoesNotExist()
            onNodeWithTag("review-explanation").assertDoesNotExist()
        }

    @Test
    fun reviewRemainsStableDuringSaveAndLoadRecoveryAndBackStaysAvailable() =
        runComposeUiTest {
            val initial = symbolCountReviewPreviewState(5, SymbolCountFailure.Timeout)
            val state = mutableStateOf(initial.copy(saveStatus = SymbolCountSaveStatus.Pending))
            var saves = 0
            var loads = 0
            var backs = 0
            setContent {
                OquTurboTheme {
                    SymbolCountScreen(
                        state.value,
                        {},
                        { _, _ -> },
                        { backs++ },
                        onRetrySaveClick = { saves++ },
                        onReloadClick = { loads++ },
                    )
                }
            }
            val cell =
                onNodeWithTag(
                    "review-cell-4-4",
                ).fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
            onNodeWithText("Play again").performScrollTo().assertIsNotEnabled()
            onNodeWithText("Back to Games").performScrollTo().performClick()
            assertEquals(1, backs)
            state.value = state.value.copy(saveStatus = SymbolCountSaveStatus.Failed, recordLoadFailed = true)
            onNodeWithText("Retry saving").performScrollTo().performClick()
            assertEquals(1, saves)
            assertEquals(0, loads)
            state.value = state.value.copy(saveStatus = SymbolCountSaveStatus.Saved)
            onNodeWithText("Retry loading").performScrollTo().performClick()
            assertEquals(1, loads)
            state.value = state.value.copy(isRecordLoading = true)
            onNodeWithText("Retry loading").assertIsNotEnabled()
            assertEquals(
                cell,
                onNodeWithTag("review-cell-4-4").fetchSemanticsNode().config[SemanticsProperties.ContentDescription],
            )
            assertEquals(initial.game, state.value.game)
        }

    @Test
    fun compactEnlargedReviewAndActionsAreReachableInEveryLocale() {
        val original = Locale.getDefault()
        try {
            for ((locale, replay, back) in listOf(
                Triple("en", "Play again", "Back to Games"),
                Triple("ru", "Играть снова", "К играм"),
                Triple("kk", "Қайта ойнау", "Ойындарға оралу"),
            )) {
                Locale.setDefault(Locale.forLanguageTag(locale))
                runComposeUiTest {
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                            OquTurboTheme {
                                SymbolCountScreen(
                                    symbolCountReviewPreviewState(5, SymbolCountFailure.Timeout),
                                    {},
                                    { _, _ -> },
                                    {},
                                    Modifier.requiredSize(320.dp, 640.dp),
                                )
                            }
                        }
                    }
                    onNodeWithTag("review-explanation").performScrollTo().assertIsDisplayed()
                    val field = onNodeWithTag("field").performScrollTo().assertIsDisplayed().fetchSemanticsNode()
                    assertEquals(280f, field.boundsInRoot.width)
                    assertEquals(280f, field.boundsInRoot.height)
                    onNodeWithTag("review-cell-4-4").assertIsDisplayed()
                    onNodeWithText(replay).performScrollTo().assertIsDisplayed().assertIsEnabled()
                    onNodeWithText(back).performScrollTo().assertIsDisplayed().assertIsEnabled()
                }
            }
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun compactStageFiveKeepsWholeFieldAndAllAnswersVisibleWithStandardTargets() =
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    OquTurboTheme {
                        SymbolCountScreen(
                            symbolCountPreviewState(SymbolCountPhase.Active, 5),
                            {},
                            { _, _ -> },
                            {},
                            modifier = Modifier.requiredSize(320.dp, 640.dp),
                        )
                    }
                }
            }
            onNodeWithTag("field").assertIsDisplayed()
            for (n in listOf(2, 5, 6, 1)) {
                val node = onNodeWithTag("answer-$n").assertIsDisplayed().assertIsEnabled().fetchSemanticsNode()
                assertTrue(node.boundsInRoot.height >= 48)
                assertTrue(node.boundsInRoot.bottom <= 640)
            }
            onNodeWithContentDescription("Pause").assertIsDisplayed()
        }

    @Test
    fun pauseRemovesFieldTargetAndAnswersAndResumeRetainsChoices() =
        runComposeUiTest {
            val initial = symbolCountPreviewState(SymbolCountPhase.Active)
            val state = mutableStateOf(initial)
            var answered = 0
            setContent {
                OquTurboTheme {
                    SymbolCountScreen(
                        state.value,
                        {},
                        { _, value -> answered = value },
                        null,
                        onPauseClick = {
                            state.value =
                                initial.copy(
                                    game = initial.game.copy(phase = SymbolCountPhase.Paused),
                                )
                        },
                        onResumeClick = { state.value = initial },
                    )
                }
            }
            onNodeWithTag("answer-3").performClick()
            assertEquals(3, answered)
            onNodeWithContentDescription("Pause").performClick()
            onNodeWithTag("field").assertDoesNotExist()
            onNodeWithTag("target").assertDoesNotExist()
            for (n in 1..4) onNodeWithTag("answer-$n").assertDoesNotExist()
            onNodeWithContentDescription("Back to Games").assertDoesNotExist()
            onNodeWithText("Resume").performClick()
            onNodeWithTag("answer-3").assertIsEnabled()
        }

    @Test
    fun correctFeedbackDisablesAllOptionsButPreservesPause() =
        runComposeUiTest {
            setContent {
                OquTurboTheme {
                    SymbolCountScreen(symbolCountPreviewState(SymbolCountPhase.Correct), {}, { _, _ -> }, {})
                }
            }
            for (n in 1..4) onNodeWithTag("answer-$n").assertIsNotEnabled()
            onNodeWithText("Correct").assertExists()
            onNodeWithContentDescription("Pause").assertIsEnabled()
        }

    @Test
    fun savingAndLoadFailureHaveTruthfulSingleRecoveryActions() =
        runComposeUiTest {
            val state = mutableStateOf(SymbolCountUiState())
            var retries = 0
            setContent {
                OquTurboTheme {
                    SymbolCountScreen(state.value, {}, { _, _ -> }, null, onRetrySaveClick = { retries++ })
                }
            }
            onNodeWithText("Start").assertIsNotEnabled()
            state.value =
                symbolCountPreviewState(
                    SymbolCountPhase.Result,
                    failure = SymbolCountFailure.Timeout,
                    save = SymbolCountSaveStatus.Pending,
                )
            onNodeWithText("Play again").assertIsNotEnabled()
            state.value = state.value.copy(saveStatus = SymbolCountSaveStatus.Failed)
            onNodeWithText("Retry saving").performScrollTo().performClick()
            assertEquals(1, retries)
            onNodeWithText("New record!").assertDoesNotExist()
        }

    @Test
    fun terminalAndSaveTransitionsExposePoliteLocalizedAnnouncements() =
        runComposeUiTest {
            val state = mutableStateOf(symbolCountPreviewState(SymbolCountPhase.Active))
            setContent {
                OquTurboTheme {
                    SymbolCountScreen(state.value, {}, { _, _ -> }, null)
                }
            }
            onNodeWithTag("result-announcement").assertDoesNotExist()
            for (failure in listOf(SymbolCountFailure.Wrong, SymbolCountFailure.Timeout)) {
                state.value =
                    symbolCountPreviewState(
                        SymbolCountPhase.Result,
                        failure = failure,
                        save = SymbolCountSaveStatus.Pending,
                    )
                val announcement = onNodeWithTag("result-announcement")
                announcement.assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
                val feedback = announcement.fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }
                if (failure == SymbolCountFailure.Timeout) {
                    assertTrue(feedback.any { it.contains("3") })
                    assertTrue(feedback.none { it.startsWith("Your answer:") })
                } else {
                    assertTrue(feedback.any { it.contains("Your answer: 2") && it.contains("3") })
                }
                val title = if (failure == SymbolCountFailure.Timeout) "Time’s up" else "Wrong answer"
                assertTrue(announcement.fetchSemanticsNode().config[SemanticsProperties.Text].any { it.text == title })
                assertTrue(
                    announcement.fetchSemanticsNode().config[SemanticsProperties.Text].any {
                        it.text == "Saving result…"
                    },
                )
                state.value = state.value.copy(saveStatus = SymbolCountSaveStatus.Failed)
                val text = announcement.fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }
                assertTrue("Result not saved. Retry saving to keep your progress." in text)
                assertTrue("Saving result…" !in text)
            }
        }

    @Test
    fun enlargedTextKeepsAnswersReachableWithoutClipping() =
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    OquTurboTheme {
                        SymbolCountScreen(
                            symbolCountPreviewState(SymbolCountPhase.Active, 5),
                            {},
                            { _, _ -> },
                            {},
                            modifier = Modifier.requiredSize(320.dp, 600.dp),
                        )
                    }
                }
            }
            onNodeWithTag("answer-1").performScrollTo().assertIsDisplayed().assertIsEnabled()
            onNodeWithContentDescription("Pause").performScrollTo().assertIsDisplayed()
        }

    @Test
    fun enlargedKazakhResultKeepsReplayAndBackReachable() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("kk"))
            runComposeUiTest {
                var replayCount = 0
                var backCount = 0
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                        OquTurboTheme {
                            SymbolCountScreen(
                                symbolCountPreviewState(
                                    SymbolCountPhase.Result,
                                    5,
                                    SymbolCountFailure.Wrong,
                                    SymbolCountSaveStatus.Saved,
                                ).copy(record = 10),
                                { replayCount++ },
                                { _, _ -> },
                                { backCount++ },
                                modifier = Modifier.requiredSize(320.dp, 600.dp),
                            )
                        }
                    }
                }
                onNodeWithText("Қайта ойнау").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
                assertEquals(1, replayCount)
                onNodeWithText("Ойындарға оралу").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
                assertEquals(1, backCount)
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
