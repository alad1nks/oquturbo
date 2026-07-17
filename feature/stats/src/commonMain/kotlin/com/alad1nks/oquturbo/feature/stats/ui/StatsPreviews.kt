package com.alad1nks.oquturbo.feature.stats.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.feature.stats.demo.DemoStatsFixtures
import com.alad1nks.oquturbo.feature.stats.model.StatsUiState

@Preview(name = "Stats — empty")
@Composable
private fun EmptyStatsPreview() {
    StatsPreview(uiState = DemoStatsFixtures.empty())
}

@Preview(name = "Stats — populated")
@Composable
private fun PopulatedStatsPreview() {
    StatsPreview(uiState = DemoStatsFixtures.populated())
}

@Preview(name = "Stats — rich", heightDp = 1600)
@Composable
private fun RichStatsPreview() {
    StatsPreview(uiState = DemoStatsFixtures.rich())
}

@Preview(name = "Stats — one mode")
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

@Preview(name = "Stats — dark", heightDp = 1200)
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
