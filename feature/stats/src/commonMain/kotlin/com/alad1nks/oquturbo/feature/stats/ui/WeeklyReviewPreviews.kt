package com.alad1nks.oquturbo.feature.stats.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameSeriesKey
import com.alad1nks.oquturbo.core.data.model.ProgressComparison
import com.alad1nks.oquturbo.core.data.practice.calculatePracticeRhythm
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview

private const val REVIEW_DAY = 20_089L // 2025-01-01: both ranges cross the year boundary.
private val previewSeries = GameSeriesKey(GameId.NumberSprint, GameModeId.NumberSprintBinary, null)

internal fun previewWeeklyReview() =
    WeeklyReviewUiState(
        REVIEW_DAY,
        previewWeeklyHistory(listOf(20_083, 20_085, 20_087, REVIEW_DAY)),
        previewWeeklyHistory(listOf(20_085, REVIEW_DAY)),
        WeeklySource.Ready(ProgressComparison.Compared(REVIEW_DAY - 27, REVIEW_DAY, previewSeries, 10, 2, 4, 2)),
    )

private fun previewWeeklyHistory(days: List<Long>, start: Long = 20_000) =
    WeeklySource.Ready(calculatePracticeRhythm(DayHistory(start, days), REVIEW_DAY))

@Composable
private fun ReviewPreview(state: WeeklyReviewUiState, dark: Boolean = false, footer: Boolean = false) {
    OquTurboTheme(darkTheme = dark) {
        WeeklyReviewScreen(
            state,
            {},
            {},
            {},
            {},
            {},
            {},
            listState = rememberLazyListState(initialFirstVisibleItemIndex = if (footer) 3 else 0),
        )
    }
}

@Preview(name = "Weekly ready exact", widthDp = 390, heightDp = 2300, locale = "en")
@ScreenshotPreview
@Composable
private fun WeeklyReadyPreview() = ReviewPreview(previewWeeklyReview())

@Preview(name = "Weekly partial independent", widthDp = 320, heightDp = 3600, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun WeeklyPartialPreview() =
    ReviewPreview(
        previewWeeklyReview().copy(
            practice = previewWeeklyHistory(listOf(20_084, 20_087, REVIEW_DAY), 20_086),
            training = previewWeeklyHistory(listOf(REVIEW_DAY), 20_088),
            comparison =
                WeeklySource.Ready(
                    ProgressComparison.InsufficientData(
                        REVIEW_DAY - 27,
                        REVIEW_DAY,
                        GameSeriesKey(
                            GameId.NumberSprint,
                            GameModeId.NumberSprintCustom,
                            "length:12;digits:0123456789",
                        ),
                        9,
                    ),
                ),
        ),
    )

@Preview(name = "Weekly practice error negative", widthDp = 320, heightDp = 3600, locale = "kk", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun WeeklyPracticeErrorPreview() =
    ReviewPreview(
        previewWeeklyReview().copy(
            practice = WeeklySource.Error,
            comparison =
                WeeklySource.Ready(
                    ProgressComparison.Compared(
                        REVIEW_DAY - 27,
                        REVIEW_DAY,
                        GameSeriesKey(GameId.WordFlow, GameModeId.WordFlowContext, "ru"),
                        10,
                        4,
                        2,
                        -2,
                    ),
                ),
        ),
        dark = true,
    )

@Preview(name = "Weekly training error no recent", widthDp = 390, heightDp = 2000, locale = "en")
@ScreenshotPreview
@Composable
private fun WeeklyTrainingErrorPreview() =
    ReviewPreview(
        previewWeeklyReview().copy(
            training = WeeklySource.Error,
            comparison = WeeklySource.Ready(ProgressComparison.NoRecentSessions(REVIEW_DAY - 27, REVIEW_DAY)),
        ),
    )

@Preview(name = "Weekly sessions error", widthDp = 320, heightDp = 3200, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun WeeklySessionsErrorPreview() = ReviewPreview(previewWeeklyReview().copy(comparison = WeeklySource.Error))

@Preview(name = "Weekly loading", widthDp = 390, heightDp = 1900, locale = "en")
@ScreenshotPreview
@Composable
private fun WeeklyLoadingPreview() = ReviewPreview(WeeklyReviewUiState(REVIEW_DAY))

@Preview(name = "Weekly zero old comparison", widthDp = 390, heightDp = 2300, locale = "en")
@ScreenshotPreview
@Composable
private fun WeeklyZeroPreview() =
    ReviewPreview(
        previewWeeklyReview().copy(
            practice = previewWeeklyHistory(emptyList()),
            training = previewWeeklyHistory(emptyList()),
            comparison =
                WeeklySource.Ready(
                    ProgressComparison.Compared(REVIEW_DAY - 27, REVIEW_DAY, previewSeries, 10, 0, 2, 2),
                ),
        ),
    )

@Preview(name = "Weekly future equal", widthDp = 390, heightDp = 2700, locale = "kk")
@ScreenshotPreview
@Composable
private fun WeeklyFuturePreview() =
    ReviewPreview(
        previewWeeklyReview().copy(
            practice = previewWeeklyHistory(listOf(REVIEW_DAY + 1)),
            training = previewWeeklyHistory(listOf(REVIEW_DAY, REVIEW_DAY + 2)),
            comparison =
                WeeklySource.Ready(
                    ProgressComparison.Compared(REVIEW_DAY - 27, REVIEW_DAY, previewSeries, 10, 2, 2, 0),
                ),
        ),
        dark = true,
    )

@Preview(name = "Weekly scrolled footer", widthDp = 320, heightDp = 640, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun WeeklyFooterPreview() = ReviewPreview(previewWeeklyReview(), footer = true)

@Preview(name = "Weekly wide", widthDp = 800, heightDp = 2000, locale = "en")
@ScreenshotPreview
@Composable
private fun WeeklyWidePreview() = ReviewPreview(previewWeeklyReview())
