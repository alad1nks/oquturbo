package com.alad1nks.oquturbo.feature.stats.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview
import com.alad1nks.oquturbo.feature.stats.demo.DemoStatsFixtures
import com.alad1nks.oquturbo.feature.stats.model.ActivityStatus
import com.alad1nks.oquturbo.feature.stats.model.ModeTrend
import com.alad1nks.oquturbo.feature.stats.model.StatsDetailUiState
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.model.StatsUiState

@Preview(
    name = "Stats — empty",
    widthDp = 390,
    heightDp = 1200,
)
@ScreenshotPreview
@Composable
private fun EmptyStatsPreview() {
    StatsPreview(uiState = DemoStatsFixtures.empty())
}

@Preview(name = "Stats — populated")
@Composable
private fun PopulatedStatsPreview() {
    StatsPreview(uiState = DemoStatsFixtures.populated())
}

@Preview(
    name = "Stats — rich",
    widthDp = 390,
    heightDp = 1600,
)
@ScreenshotPreview
@Composable
private fun RichStatsPreview() {
    StatsPreview(uiState = DemoStatsFixtures.rich())
}

@Preview(
    name = "Stats — games-only day selected",
    widthDp = 390,
    heightDp = 1200,
)
@ScreenshotPreview
@Composable
private fun GamesOnlyDaySelectedStatsPreview() {
    val state = DemoStatsFixtures.rich()
    val selectedDay =
        state.snapshot.activityDays.first {
            it.status == ActivityStatus.GamesOnly && it.games > 0 && it.minutes > 0
        }
    StatsPreview(uiState = state.copy(selectedDayId = selectedDay.id))
}

@Preview(
    name = "Stats — one mode",
    widthDp = 390,
    heightDp = 1200,
)
@ScreenshotPreview
@Composable
private fun OneModeStatsPreview() {
    StatsPreview(uiState = DemoStatsFixtures.oneMode())
}

@Preview(name = "Stats — multiple modes")
@Composable
private fun MultiModeStatsPreview() {
    StatsPreview(uiState = DemoStatsFixtures.multiMode())
}

@Preview(name = "Stats — no activity")
@Composable
private fun NoActivityStatsPreview() {
    StatsPreview(uiState = DemoStatsFixtures.noActivity())
}

@Preview(name = "Stats — new record", heightDp = 1200)
@Composable
private fun NewRecordStatsPreview() {
    StatsPreview(uiState = DemoStatsFixtures.newRecord())
}

@Preview(
    name = "Stats — dark",
    widthDp = 390,
    heightDp = 1200,
)
@ScreenshotPreview
@Composable
private fun DarkStatsPreview() {
    OquTurboTheme(darkTheme = true) {
        StatsScreenPreviewContent(uiState = DemoStatsFixtures.rich())
    }
}

@Composable
private fun StatsPreview(uiState: StatsUiState) {
    OquTurboTheme {
        StatsScreenPreviewContent(uiState = uiState)
    }
}

@Composable
private fun StatsScreenPreviewContent(uiState: StatsUiState) {
    StatsScreen(
        uiState = uiState,
        onPeriodSelected = {},
        onDaySelected = {},
        onGameSelected = {},
        onModeSelected = {},
        onGamesClick = {},
        onGameClick = {},
        onActivityClick = { _, _ -> },
    )
}

@Preview(name = "Stats detail — game populated", widthDp = 320, heightDp = 1600, locale = "en")
@ScreenshotPreview
@Composable
private fun StatsDetailGamePopulatedPreview() {
    OquTurboTheme {
        StatsGameDetailScreen(
            uiState = detailPreviewState(StatsPeriod.SevenDays),
            onPeriodSelected = {},
            onBackClick = {},
            onModeClick = { _, _, _ -> },
        )
    }
}

@Preview(name = "Stats detail — mode populated", widthDp = 320, heightDp = 1600, locale = "ru")
@ScreenshotPreview
@Composable
private fun StatsDetailModePopulatedPreview() {
    OquTurboTheme {
        StatsModeDetailScreen(
            uiState = detailPreviewState(StatsPeriod.ThirtyDays),
            onPeriodSelected = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Stats detail — mode empty", widthDp = 320, heightDp = 1200, locale = "kk")
@ScreenshotPreview
@Composable
private fun StatsDetailModeEmptyPreview() {
    OquTurboTheme {
        StatsModeDetailScreen(
            uiState = detailPreviewState(StatsPeriod.AllTime).copy(modes = emptyList()),
            onPeriodSelected = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Stats detail — game empty", widthDp = 320, heightDp = 1200, locale = "en")
@ScreenshotPreview
@Composable
private fun StatsDetailGameEmptyPreview() {
    OquTurboTheme {
        StatsGameDetailScreen(
            uiState = detailPreviewState(StatsPeriod.ThirtyDays).copy(modes = emptyList()),
            onPeriodSelected = {},
            onBackClick = {},
            onModeClick = { _, _, _ -> },
        )
    }
}

private fun detailPreviewState(period: StatsPeriod) =
    StatsDetailUiState(
        game = StatsGame.NumberSprint,
        period = period,
        selectedMode = StatsMode.Custom,
        modes =
            listOf("length:3;digits:012", "length:4;digits:345").mapIndexed { index, variant ->
                ModeTrend(
                    mode = StatsMode.Custom,
                    variantId = variant,
                    scores = listOf(3, 4, 2, 6, 5 + index),
                    record = 6,
                    lastResult = 5 + index,
                    averageResult = 4,
                    gamesPlayed = 5,
                )
            },
    )

@Preview(name = "Stats Number Trail mode", widthDp = 320, heightDp = 1400, locale = "kk")
@ScreenshotPreview
@Composable
private fun NumberTrailStatsModePreview() {
    OquTurboTheme {
        StatsModeDetailScreen(
            uiState =
                StatsDetailUiState(
                    game = StatsGame.NumberTrail,
                    period = StatsPeriod.AllTime,
                    selectedMode = StatsMode.Ascending,
                    modes =
                        listOf(
                            ModeTrend(
                                mode = StatsMode.Ascending,
                                scores = listOf(4, 12, 8, 48),
                                record = 48,
                                lastResult = 48,
                                averageResult = 18,
                                gamesPlayed = 4,
                            ),
                        ),
                ),
            onPeriodSelected = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Stats Number Trail history", widthDp = 320, heightDp = 1800, locale = "ru")
@ScreenshotPreview
@Composable
private fun NumberTrailStatsHistoryPreview() {
    val base = DemoStatsFixtures.oneMode()
    StatsPreview(
        base.copy(
            snapshot =
                base.snapshot.copy(
                    recentActivity =
                        base.snapshot.recentActivity.map {
                            it.copy(
                                game = StatsGame.NumberTrail,
                                mode = StatsMode.Ascending,
                            )
                        },
                    trends =
                        base.snapshot.trends.map {
                            it.copy(
                                game = StatsGame.NumberTrail,
                                modes = it.modes.take(1).map { mode -> mode.copy(mode = StatsMode.Ascending) },
                            )
                        },
                    games = base.snapshot.games.map { it.copy(game = StatsGame.NumberTrail) },
                ),
        ),
    )
}

@Preview(name = "Stats Number Trail visible history", widthDp = 320, heightDp = 844, locale = "ru")
@ScreenshotPreview
@Composable
private fun NumberTrailVisibleHistoryPreview() {
    OquTurboTheme {
        Box(Modifier.fillMaxSize().appBackground().padding(16.dp)) {
            RecentHistorySection(
                activities =
                    listOf(
                        com.alad1nks.oquturbo.feature.stats.model.RecentActivity(
                            type = com.alad1nks.oquturbo.feature.stats.model.RecentActivityType.GameResult,
                            game = StatsGame.NumberTrail,
                            mode = StatsMode.Ascending,
                            score = 48,
                        ),
                    ),
                onActivityClick = { _, _ -> },
            )
        }
    }
}
