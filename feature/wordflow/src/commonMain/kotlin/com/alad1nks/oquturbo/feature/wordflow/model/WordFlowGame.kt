package com.alad1nks.oquturbo.feature.wordflow.model

interface WordFlowShuffler {
    fun <T> shuffle(values: List<T>): List<T>
}

object RandomWordFlowShuffler : WordFlowShuffler {
    override fun <T> shuffle(values: List<T>): List<T> = values.shuffled()
}

enum class WordFlowPhase { Ready, Active, CorrectFeedback, Result }

enum class WordFlowFailure { Wrong, Timeout }

data class WordFlowRound(
    val prompt: WordFlowPrompt,
    val choices: List<String>,
    val totalTimeMillis: Long,
    val remainingTimeMillis: Long = totalTimeMillis,
)

data class WordFlowState(
    val phase: WordFlowPhase = WordFlowPhase.Ready,
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val round: WordFlowRound? = null,
    val selectedAnswer: String? = null,
    val failure: WordFlowFailure? = null,
) {
    val tier: WordFlowTier? get() = round?.prompt?.tier
}

class WordFlowGame(
    private val content: WordFlowContent,
    private val shuffler: WordFlowShuffler = RandomWordFlowShuffler,
) {
    var state: WordFlowState = WordFlowState()
        private set
    private val decks = mutableMapOf<WordFlowTier, MutableList<WordFlowPrompt>>()
    private val previousPromptIds = mutableMapOf<WordFlowTier, String>()

    fun start() {
        decks.clear()
        previousPromptIds.clear()
        state = WordFlowState(phase = WordFlowPhase.Active, round = nextRound(score = 0))
    }

    fun selectAnswer(answer: String) {
        val current = state
        val round = current.round ?: return
        if (current.phase != WordFlowPhase.Active || answer !in round.choices) return
        state =
            if (answer == round.prompt.correctAnswer) {
                current.copy(
                    phase = WordFlowPhase.CorrectFeedback,
                    score = current.score + 1,
                    correctAnswers = current.correctAnswers + 1,
                    selectedAnswer = answer,
                )
            } else {
                current.copy(phase = WordFlowPhase.Result, selectedAnswer = answer, failure = WordFlowFailure.Wrong)
            }
    }

    fun continueAfterCorrect() {
        val current = state
        if (current.phase != WordFlowPhase.CorrectFeedback) return
        state = current.copy(phase = WordFlowPhase.Active, round = nextRound(current.score), selectedAnswer = null)
    }

    fun elapse(millis: Long) {
        val current = state
        val round = current.round ?: return
        if (current.phase != WordFlowPhase.Active || millis <= 0) return
        val remaining = (round.remainingTimeMillis - millis).coerceAtLeast(0)
        state =
            if (remaining == 0L) {
                current.copy(
                    phase = WordFlowPhase.Result,
                    round = round.copy(remainingTimeMillis = 0),
                    failure = WordFlowFailure.Timeout,
                    selectedAnswer = null,
                )
            } else {
                current.copy(round = round.copy(remainingTimeMillis = remaining))
            }
    }

    private fun nextRound(score: Int): WordFlowRound {
        val tier = tierFor(score)
        val deck = decks.getOrPut(tier) { mutableListOf() }
        if (deck.isEmpty()) {
            val shuffled = shuffler.shuffle(content.prompts(tier)).toMutableList()
            val previousId = previousPromptIds[tier]
            if (shuffled.size > 1 && shuffled.first().id == previousId) {
                val replacement = shuffled.indexOfFirst { it.id != previousId }
                val first = shuffled[0]
                shuffled[0] = shuffled[replacement]
                shuffled[replacement] = first
            }
            deck += shuffled
        }
        val prompt = deck.removeAt(0)
        previousPromptIds[tier] = prompt.id
        val choices = shuffler.shuffle(prompt.answers)
        require(choices.toSet() == prompt.answers.toSet()) { "Word Flow shuffler changed the available choices" }
        return WordFlowRound(prompt, choices, timeFor(tier))
    }

    companion object {
        const val EASY_TIME_MILLIS = 10_000L
        const val MEDIUM_TIME_MILLIS = 8_000L
        const val HARD_TIME_MILLIS = 6_000L

        fun tierFor(score: Int): WordFlowTier =
            when {
                score < 5 -> WordFlowTier.Easy
                score < 10 -> WordFlowTier.Medium
                else -> WordFlowTier.Hard
            }

        fun timeFor(tier: WordFlowTier): Long =
            when (tier) {
                WordFlowTier.Easy -> EASY_TIME_MILLIS
                WordFlowTier.Medium -> MEDIUM_TIME_MILLIS
                WordFlowTier.Hard -> HARD_TIME_MILLIS
            }
    }
}
