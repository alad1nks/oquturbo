package com.alad1nks.oquturbo.feature.baspagame.ui

import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

class BaspaGameUiStateTest {
    @Test
    fun nonMatchingTapEndsPlayingSessionWithIncorrectTapReason() {
        val playingState = playingState(shouldTap = false)

        val reason = playingState.mistakeReasonOnTap()
        val mistakeState = playingState.withMistake(requireNotNull(reason), sessionInProgress = true)

        assertEquals(BaspaMistakeReason.IncorrectTap, mistakeState.mistakeReason)
        assertEquals(BaspaGameUiState.Phase.Mistake, mistakeState.phase)
    }

    @Test
    fun matchingItemTimeoutEndsPlayingSessionWithMissedMatchReason() {
        val playingState = playingState(shouldTap = true)

        val reason = playingState.mistakeReasonOnTimeout()
        val mistakeState = playingState.withMistake(requireNotNull(reason), sessionInProgress = true)

        assertEquals(BaspaMistakeReason.MissedMatch, mistakeState.mistakeReason)
        assertEquals(BaspaGameUiState.Phase.Mistake, mistakeState.phase)
    }

    @Test
    fun nonMatchingItemTimeoutHasNoFailureReason() {
        assertNull(playingState(shouldTap = false).mistakeReasonOnTimeout())
    }

    @Test
    fun invalidTapsAndLaterTerminalEventsCannotCreateOrReplaceReason() {
        val blankPlayingState = playingState(shouldTap = false).copy(stimulus = "")
        assertNull(blankPlayingState.mistakeReasonOnTap())
        BaspaGameUiState.Phase.entries
            .filterNot { it == BaspaGameUiState.Phase.Playing }
            .forEach { phase ->
                assertNull(playingState(shouldTap = false).copy(phase = phase).mistakeReasonOnTap())
            }

        val activeState = playingState(shouldTap = false)
        val inactiveSessionState =
            activeState.withMistake(
                reason = BaspaMistakeReason.IncorrectTap,
                sessionInProgress = false,
            )
        assertSame(activeState, inactiveSessionState)

        val firstMistake =
            playingState(shouldTap = false).withMistake(
                reason = BaspaMistakeReason.IncorrectTap,
                sessionInProgress = true,
            )
        val laterMistake =
            firstMistake.withMistake(
                reason = BaspaMistakeReason.MissedMatch,
                sessionInProgress = true,
            )
        assertSame(firstMistake, laterMistake)
        assertEquals(BaspaMistakeReason.IncorrectTap, laterMistake.mistakeReason)
    }

    @Test
    fun restartClearsReasonWithScoreAndSpeedReset() {
        val mistakeState =
            playingState(shouldTap = false)
                .copy(score = 24, intervalMillis = 1_500L)
                .withMistake(BaspaMistakeReason.IncorrectTap, sessionInProgress = true)

        val restartedState = mistakeState.restartingSession()

        assertNull(restartedState.mistakeReason)
        assertEquals(0, restartedState.score)
        assertEquals(2_000L, restartedState.intervalMillis)
        assertEquals(BaspaGameUiState.Phase.Playing, restartedState.phase)
    }

    @Test
    fun thresholdRuleChangeCannotPairPreviousItemWithNewRule() {
        val previousRound =
            playingState(shouldTap = true).copy(
                categoryName = "animals",
                stimulusRoundId = 10L,
                score = 9,
            )
        val thresholdGap =
            previousRound.copy(
                categoryName = "vehicles",
                stimulus = "",
                score = 10,
            )
        val nextRound =
            thresholdGap.copy(
                stimulus = "CAR",
                stimulusRoundId = 11L,
            )

        assertFalse(thresholdGap.isCurrentStimulusRound(previousRound.stimulusRoundId))
        assertFalse(nextRound.isCurrentStimulusRound(previousRound.stimulusRoundId))
        assertNull(thresholdGap.mistakeReason)
        assertEquals(BaspaGameUiState.Phase.Playing, thresholdGap.phase)
    }

    private fun playingState(shouldTap: Boolean) =
        BaspaGameUiState(
            mode = BaspaGameMode.Categories,
            stimulus = "CAT",
            shouldTap = shouldTap,
            phase = BaspaGameUiState.Phase.Playing,
        )
}
