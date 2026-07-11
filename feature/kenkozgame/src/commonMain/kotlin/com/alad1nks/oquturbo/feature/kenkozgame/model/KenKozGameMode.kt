package com.alad1nks.oquturbo.feature.kenkozgame.model

import kotlinx.serialization.Serializable

@Serializable
enum class KenKozGameMode {
    Characters,
    Words,
    FindDifference,
    WideLine,
}
