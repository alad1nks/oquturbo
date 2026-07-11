package com.alad1nks.oquturbo.feature.baspagame.model

import kotlinx.serialization.Serializable

@Serializable
enum class BaspaGameMode {
    Categories,
    Letter,
    WordLength,
    TextColor,
    TrueFalse,
    Math,
    SpeedReading,
}
