package com.alad1nks.oquturbo.feature.wordflow.ui

import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowPrompt
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowRound
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowState
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowTier
import com.alad1nks.oquturbo.resources.AppResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WordFlowDifficultyResourceTest {
    @Test
    fun finalDifficultyUsesTierFromCurrentRound() {
        assertEquals(AppResource.String.word_flow_difficulty_easy, state(WordFlowTier.Easy).difficultyResource())
        assertEquals(AppResource.String.word_flow_difficulty_medium, state(WordFlowTier.Medium).difficultyResource())
        assertEquals(AppResource.String.word_flow_difficulty_hard, state(WordFlowTier.Hard).difficultyResource())
    }

    @Test
    fun missingRoundHasNoFinalDifficulty() {
        assertNull(WordFlowState().difficultyResource())
    }

    private fun state(tier: WordFlowTier): WordFlowState {
        val prompt =
            WordFlowPrompt(
                id = tier.name,
                tier = tier,
                sentenceTemplate = "The answer is %1\$s.",
                correctAnswer = "correct",
                wrongAnswers = listOf("wrong-a", "wrong-b"),
            )
        return WordFlowState(round = WordFlowRound(prompt, prompt.answers, totalTimeMillis = 1_000))
    }
}
