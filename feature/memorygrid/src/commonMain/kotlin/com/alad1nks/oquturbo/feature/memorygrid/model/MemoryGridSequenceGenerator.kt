package com.alad1nks.oquturbo.feature.memorygrid.model

import kotlin.random.Random

fun interface MemoryGridSequenceGenerator {
    fun generate(
        cellCount: Int,
        length: Int,
        allowRepeatedCells: Boolean,
    ): List<Int>
}

class RandomMemoryGridSequenceGenerator(
    private val random: Random = Random.Default,
) : MemoryGridSequenceGenerator {
    override fun generate(
        cellCount: Int,
        length: Int,
        allowRepeatedCells: Boolean,
    ): List<Int> {
        require(cellCount > 0) { "Memory Grid must contain at least one cell" }
        require(length > 0) { "Memory Grid sequence must contain at least one cell" }
        require(allowRepeatedCells || length <= cellCount) {
            "Cannot generate $length unique cells from a grid containing $cellCount cells"
        }

        return if (allowRepeatedCells) {
            List(length) { random.nextInt(cellCount) }
        } else {
            (0 until cellCount).shuffled(random).take(length)
        }
    }
}
