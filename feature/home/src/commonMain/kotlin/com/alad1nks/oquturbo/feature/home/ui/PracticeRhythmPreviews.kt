package com.alad1nks.oquturbo.feature.home.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.practice.calculatePracticeRhythm
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview

internal fun previewPractice(start: Long = 20_000, days: List<Long> = listOf(20_084, 20_087, 20_089)) =
    PracticeRhythmState.Ready(calculatePracticeRhythm(DayHistory(start, days), 20_089))

@Composable
private fun RhythmPreview(state: PracticeRhythmState, dark: Boolean = false) {
    OquTurboTheme(darkTheme = dark) {
        Column(Modifier.fillMaxSize().appBackground().padding(24.dp)) { PracticeRhythmCard(state, {}) }
    }
}

@Preview(name = "Practice full Home", widthDp = 390, heightDp = 2400)
@ScreenshotPreview
@Composable
private fun PracticeHomePreview() {
    OquTurboTheme {
        HomeScreen(
            HomeUiState(
                dailyTraining = previewDailyTraining(false),
                personalResult = previewNoRecentResult(),
                practiceRhythm = previewPractice(),
            ),
            {},
        )
    }
}

@Preview(name = "Practice partial reached", widthDp = 320, heightDp = 1600, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun PracticePartialPreview() {
    RhythmPreview(previewPractice(20_086, listOf(20_084, 20_086, 20_087, 20_089)))
}

@Preview(name = "Practice zero future", widthDp = 320, heightDp = 1750, locale = "kk", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun PracticeFuturePreview() {
    RhythmPreview(previewPractice(days = listOf(20_090)), dark = true)
}

@Preview(name = "Practice loading", widthDp = 390, heightDp = 400)
@ScreenshotPreview
@Composable
private fun PracticeLoadingPreview() {
    RhythmPreview(PracticeRhythmState.Loading)
}

@Preview(name = "Practice error", widthDp = 320, heightDp = 600, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun PracticeErrorPreview() {
    RhythmPreview(PracticeRhythmState.Error)
}

@Preview(name = "Practice error scrolled Home", widthDp = 320, heightDp = 640, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun PracticeScrolledErrorPreview() {
    OquTurboTheme {
        HomeScreen(
            HomeUiState(
                dailyTraining = previewDailyTraining(false),
                personalResult = previewNoRecentResult(),
                practiceRhythm = PracticeRhythmState.Error,
            ),
            {},
            listState = rememberLazyListState(initialFirstVisibleItemIndex = 4),
        )
    }
}

@Preview(name = "Practice wide", widthDp = 800, heightDp = 950)
@ScreenshotPreview
@Composable
private fun PracticeWidePreview() {
    RhythmPreview(previewPractice())
}
