package com.alad1nks.oquturbo.feature.kenkozgame.ui

import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KenKozGameUiStateTest {
    @Test
    fun wrongAnswerIsRetainedUntilNewSessionStarts() {
        val answeringState =
            KenKozGameUiState(
                mode = KenKozGameMode.Words,
                phase = KenKozGameUiState.Phase.Answering,
                correctAnswer = "mountain",
            )

        val mistakeState = answeringState.withMistake("morning")

        assertEquals(KenKozGameUiState.Phase.Mistake, mistakeState.phase)
        assertEquals("morning", mistakeState.selectedAnswer)
        assertEquals("mountain", mistakeState.correctAnswer)

        val restartedState = mistakeState.startingSession()

        assertNull(restartedState.selectedAnswer)
    }
}
