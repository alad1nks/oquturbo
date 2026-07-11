package com.alad1nks.oquturbo.feature.baspagame.model

data class BaspaGameContent(
    val categories: List<Category>,
    val matchingWords: List<String>,
    val otherWords: List<String>,
    val statements: List<Pair<String, Boolean>>,
    val equations: List<Pair<String, Boolean>>,
)

data class Category(val id: String, val name: String, val words: List<String>)
