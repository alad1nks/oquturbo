package com.alad1nks.oquturbo.feature.wordflow.model

enum class WordFlowTier { Easy, Medium, Hard }

data class WordFlowPrompt(
    val id: String,
    val tier: WordFlowTier,
    val sentenceTemplate: String,
    val correctAnswer: String,
    val wrongAnswers: List<String>,
) {
    val answers: List<String> = listOf(correctAnswer) + wrongAnswers

    fun sentence(answer: String): String = sentenceTemplate.replace(ANSWER_SLOT, answer)

    fun sentenceWithBlank(blank: String): String = sentence(blank)
}

class WordFlowContent(val prompts: List<WordFlowPrompt>) {
    init {
        require(
            prompts.map(WordFlowPrompt::id).distinct().size == prompts.size,
        ) { "Word Flow prompt IDs must be unique" }
        WordFlowTier.entries.forEach { tier ->
            require(prompts.count { it.tier == tier } == PROMPTS_PER_TIER) {
                "Word Flow requires exactly $PROMPTS_PER_TIER ${tier.name} prompts"
            }
        }
        prompts.forEach { prompt ->
            require(prompt.id.isNotBlank()) { "Word Flow prompt ID must not be blank" }
            require(prompt.sentenceTemplate.countOccurrences(ANSWER_SLOT) == 1) {
                "Word Flow prompt ${prompt.id} must contain exactly one $ANSWER_SLOT slot"
            }
            require(prompt.wrongAnswers.size == WRONG_ANSWER_COUNT) {
                "Word Flow prompt ${prompt.id} must contain exactly $WRONG_ANSWER_COUNT wrong answers"
            }
            require(prompt.answers.all(String::isNotBlank)) { "Word Flow prompt ${prompt.id} has a blank answer" }
            require(prompt.answers.map { it.trim().lowercase() }.distinct().size == CHOICE_COUNT) {
                "Word Flow prompt ${prompt.id} choices must be pairwise distinct"
            }
        }
    }

    fun prompts(tier: WordFlowTier): List<WordFlowPrompt> = prompts.filter { it.tier == tier }

    companion object {
        const val PROMPTS_PER_TIER = 6
        const val WRONG_ANSWER_COUNT = 2
        const val CHOICE_COUNT = 3
    }
}

const val ANSWER_SLOT = "%1\$s"

fun normalizeWordFlowLocale(locale: String?): String {
    val language = locale.orEmpty().trim().substringBefore('-').substringBefore('_').lowercase()
    return language.takeIf { it == "en" || it == "ru" || it == "kk" } ?: "en"
}

private fun String.countOccurrences(value: String): Int {
    var count = 0
    var start = 0
    while (true) {
        val index = indexOf(value, start)
        if (index < 0) return count
        count++
        start = index + value.length
    }
}
