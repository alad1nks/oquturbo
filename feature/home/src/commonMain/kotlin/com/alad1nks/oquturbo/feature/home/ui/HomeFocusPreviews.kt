package com.alad1nks.oquturbo.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alad1nks.oquturbo.core.data.model.WeeklyFocus
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusSelection
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview

private const val FOCUS_DAY = 20_089L

@Preview(name = "Focus Home partial ordinary plan", widthDp = 320, heightDp = 2400, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun FocusHomeActivePreview() {
    val ordinary = previewDailyTraining(false)
    val items = ordinary.items.sortedBy { if (it.game == HomeUiState.Game.NumberSprint)1 else 0 }
    OquTurboTheme {
        HomeScreen(
            HomeUiState(
                dailyTraining =
                    ordinary.copy(
                        items =
                            items.mapIndexed {
                                index,
                                item,
                                ->
                                item.copy(isCompleted = index == 0, entry = item.entry.copy(isCompleted = index == 0))
                            },
                    ),
                personalResult = previewNoRecentResult(),
                practiceRhythm = previewPractice(),
                focus =
                    HomeFocusState.Ready(
                        WeeklyFocus(WeeklyFocusSelection(FOCUS_DAY - 2, FOCUS_DAY + 5)),
                        FOCUS_DAY,
                    ),
            ),
            {},
        )
    }
}

@Preview(name = "Focus Home generation error", widthDp = 390, heightDp = 1900, locale = "en")
@ScreenshotPreview
@Composable
private fun FocusHomeErrorPreview() {
    OquTurboTheme(darkTheme = true) {
        HomeScreen(
            HomeUiState(
                trainingLoadFailed = true,
                personalResult = previewNoRecentResult(),
                practiceRhythm = previewPractice(),
                focus = HomeFocusState.Error,
            ),
            {},
        )
    }
}

@Preview(name = "Focus Home scheduled", widthDp = 390, heightDp = 1800, locale = "en")
@ScreenshotPreview
@Composable
private fun FocusHomeScheduledPreview() {
    OquTurboTheme {
        HomeScreen(
            HomeUiState(
                dailyTraining = previewDailyTraining(false),
                focus =
                    HomeFocusState.Ready(
                        WeeklyFocus(WeeklyFocusSelection.after(FOCUS_DAY)),
                        FOCUS_DAY,
                    ),
            ),
            {},
        )
    }
}

@Preview(name = "Focus Home expired", widthDp = 390, heightDp = 1800, locale = "en")
@ScreenshotPreview
@Composable
private fun FocusHomeExpiredPreview() {
    OquTurboTheme {
        HomeScreen(
            HomeUiState(
                dailyTraining = previewDailyTraining(false),
                focus =
                    HomeFocusState.Ready(
                        WeeklyFocus(WeeklyFocusSelection(FOCUS_DAY - 7, FOCUS_DAY)),
                        FOCUS_DAY,
                    ),
            ),
            {},
        )
    }
}
