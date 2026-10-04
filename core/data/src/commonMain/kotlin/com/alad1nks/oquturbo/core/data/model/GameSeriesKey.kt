package com.alad1nks.oquturbo.core.data.model

/** Exact, non-persisted series identity. Null and every non-null variant remain distinct. */
data class GameSeriesKey(
    val game: GameId,
    val mode: GameModeId,
    val variantId: String?,
)
