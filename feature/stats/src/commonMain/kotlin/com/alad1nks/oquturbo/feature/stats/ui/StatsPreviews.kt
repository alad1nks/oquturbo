package com.alad1nks.oquturbo.feature.stats.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview
import com.alad1nks.oquturbo.feature.stats.demo.DemoStatsFixtures
import com.alad1nks.oquturbo.feature.stats.model.ActivityStatus
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
