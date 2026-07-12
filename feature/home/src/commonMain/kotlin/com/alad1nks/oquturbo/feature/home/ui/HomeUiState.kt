package com.alad1nks.oquturbo.feature.home.ui

internal data class HomeUiState(
    val overallLevel: Int = 37,
    val rank: Rank = Rank.Master,
    val recentRecords: List<RecentRecord> =
        listOf(
            RecentRecord(Game.NumberSprint, 54),
            RecentRecord(Game.WideEye, 31),
            RecentRecord(Game.DontTap, 28),
        ),
) {
    enum class Rank {
        Master,
    }

    enum class Game {
        NumberSprint,
        WideEye,
        DontTap,
    }

    data class RecentRecord(
        val game: Game,
        val score: Int,
    )
}
