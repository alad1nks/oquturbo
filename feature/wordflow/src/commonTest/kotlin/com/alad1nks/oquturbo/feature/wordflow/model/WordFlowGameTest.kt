package com.alad1nks.oquturbo.feature.wordflow.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WordFlowGameTest {
    @Test
    fun thresholdsUseApprovedTierAndTime() {
        assertEquals(WordFlowTier.Easy, WordFlowGame.tierFor(0))
        assertEquals(WordFlowTier.Easy, WordFlowGame.tierFor(4))
        assertEquals(WordFlowTier.Medium, WordFlowGame.tierFor(5))
        assertEquals(WordFlowTier.Medium, WordFlowGame.tierFor(9))
        assertEquals(WordFlowTier.Hard, WordFlowGame.tierFor(10))
        assertEquals(10_000L, WordFlowGame.timeFor(WordFlowTier.Easy))
        assertEquals(8_000L, WordFlowGame.timeFor(WordFlowTier.Medium))
        assertEquals(6_000L, WordFlowGame.timeFor(WordFlowTier.Hard))
    }

    @Test
    fun correctAnswerCountsOnceAndAdvancesAfterFeedback() {
        val game = game()
        game.start()
        val correct = game.state.round!!.prompt.correctAnswer

        game.selectAnswer(correct)
        game.selectAnswer(correct)

        assertEquals(WordFlowPhase.CorrectFeedback, game.state.phase)
        assertEquals(1, game.state.score)
        assertEquals(1, game.state.correctAnswers)
        game.continueAfterCorrect()
        assertEquals(WordFlowPhase.Active, game.state.phase)
        assertEquals(1, game.state.score)
    }

    @Test
    fun wrongAnswerEndsWithoutIncrementAndLateInputIsIgnored() {
        val game = game()
        game.start()
        val round = game.state.round!!
        val wrong = round.choices.first { it != round.prompt.correctAnswer }

        game.selectAnswer(wrong)
        game.selectAnswer(round.prompt.correctAnswer)

        assertEquals(WordFlowPhase.Result, game.state.phase)
        assertEquals(WordFlowFailure.Wrong, game.state.failure)
        assertEquals(wrong, game.state.selectedAnswer)
        assertEquals(0, game.state.score)
    }

    @Test
    fun countdownTimeoutEndsWithoutSelectionOrIncrement() {
        val game = game()
        game.start()
        game.elapse(9_999)
        assertEquals(WordFlowPhase.Active, game.state.phase)
        assertEquals(1, game.state.round!!.remainingTimeMillis)
        game.elapse(1)
        assertEquals(WordFlowPhase.Result, game.state.phase)
        assertEquals(WordFlowFailure.Timeout, game.state.failure)
        assertNull(game.state.selectedAnswer)
        assertEquals(0, game.state.correctAnswers)
    }

    @Test
    fun tierDeckExhaustsBeforeRepeatAndReshuffleAvoidsImmediateRepeat() {
        val game = game()
        game.start()
        repeat(10) {
            game.selectAnswer(game.state.round!!.prompt.correctAnswer)
            game.continueAfterCorrect()
        }
        val promptIds = mutableListOf<String>()
        repeat(7) {
            promptIds += game.state.round!!.prompt.id
            game.selectAnswer(game.state.round!!.prompt.correctAnswer)
            game.continueAfterCorrect()
        }
        assertEquals(6, promptIds.take(6).toSet().size)
        assertNotEquals(promptIds[5], promptIds[6])
    }

    @Test
    fun choicesAreShuffledIndependentlyFromPromptDeck() {
        val shuffler = RecordingReverseShuffler()
        val game = WordFlowGame(content(), shuffler)
        game.start()
        val round = game.state.round!!
        assertEquals(round.prompt.answers.reversed(), round.choices)
        assertTrue(shuffler.inputSizes.containsAll(listOf(6, 3)))
    }

    @Test
    fun replayResetsAttemptAndDeck() {
        val game = game()
        game.start()
        game.selectAnswer(game.state.round!!.prompt.correctAnswer)
        game.start()
        assertEquals(WordFlowPhase.Active, game.state.phase)
        assertEquals(0, game.state.score)
        assertEquals(0, game.state.correctAnswers)
        assertEquals(10_000L, game.state.round!!.remainingTimeMillis)
    }

    @Test
    fun localeNormalizationAllowsOnlySupportedSeries() {
        assertEquals("en", normalizeWordFlowLocale("en-US"))
        assertEquals("ru", normalizeWordFlowLocale("RU_ru"))
        assertEquals("kk", normalizeWordFlowLocale("kk-KZ"))
        assertEquals("en", normalizeWordFlowLocale("de"))
        assertEquals("en", normalizeWordFlowLocale(null))
    }

    @Test
    fun malformedContentFailsFast() {
        val malformed = content().prompts.toMutableList()
        malformed[0] = malformed[0].copy(sentenceTemplate = "No slot")
        assertFailsWith<IllegalArgumentException> { WordFlowContent(malformed) }
    }

    private fun game() = WordFlowGame(content(), RecordingReverseShuffler())

    private fun content(): WordFlowContent =
        WordFlowContent(
            WordFlowTier.entries.flatMap { tier ->
                (1..6).map { index ->
                    WordFlowPrompt(
                        id = "${tier.name.lowercase()}-$index",
                        tier = tier,
                        sentenceTemplate = "The answer is %1\$s.",
                        correctAnswer = "correct-$index",
                        wrongAnswers = listOf("wrong-a-$index", "wrong-b-$index"),
                    )
                }
            },
        )

    private class RecordingReverseShuffler : WordFlowShuffler {
        val inputSizes = mutableListOf<Int>()

        override fun <T> shuffle(values: List<T>): List<T> {
            inputSizes += values.size
            return values.reversed()
        }
    }
}
