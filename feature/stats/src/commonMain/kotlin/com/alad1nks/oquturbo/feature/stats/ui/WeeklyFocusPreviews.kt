package com.alad1nks.oquturbo.feature.stats.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alad1nks.oquturbo.core.data.model.WeeklyFocus
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusSelection
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview

private const val FOCUS_DAY = 20_089L

@Composable
private fun FocusReviewPreview(state: WeeklyFocusUiState, dark: Boolean = false) {
    OquTurboTheme(darkTheme = dark) {
        WeeklyReviewScreen(
            previewWeeklyReview().copy(focus = state),
            {},
            {},
            {},
            {},
            {},
            {},
            listState = rememberLazyListState(initialFirstVisibleItemIndex = 3),
        )
    }
}

@Preview(name = "Focus Off", widthDp = 390, heightDp = 1800, locale = "en")
@ScreenshotPreview
@Composable
private fun FocusOffPreview() = FocusReviewPreview(WeeklyFocusUiState.Ready(WeeklyFocus()))

@Preview(name = "Focus Scheduled", widthDp = 320, heightDp = 2400, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun FocusScheduledPreview() =
    FocusReviewPreview(
        WeeklyFocusUiState.Ready(WeeklyFocus(WeeklyFocusSelection.after(FOCUS_DAY))),
    )

@Preview(name = "Focus Active", widthDp = 320, heightDp = 2500, locale = "kk", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun FocusActivePreview() =
    FocusReviewPreview(
        WeeklyFocusUiState.Ready(WeeklyFocus(WeeklyFocusSelection(FOCUS_DAY - 2, FOCUS_DAY + 5))),
        dark = true,
    )

@Preview(name = "Focus Expired", widthDp = 390, heightDp = 1700, locale = "en")
@ScreenshotPreview
@Composable
private fun FocusExpiredPreview() =
    FocusReviewPreview(
        WeeklyFocusUiState.Ready(WeeklyFocus(WeeklyFocusSelection(FOCUS_DAY - 7, FOCUS_DAY))),
    )

@Preview(name = "Focus Loading", widthDp = 390, heightDp = 1000, locale = "en")
@ScreenshotPreview
@Composable
private fun FocusLoadingPreview() = FocusReviewPreview(WeeklyFocusUiState.Loading)

@Preview(name = "Focus Saving", widthDp = 390, heightDp = 1000, locale = "en")
@ScreenshotPreview
@Composable
private fun FocusSavingPreview() = FocusReviewPreview(WeeklyFocusUiState.Saving)

@Preview(name = "Focus Error recovery", widthDp = 320, heightDp = 2000, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun FocusErrorPreview() = FocusReviewPreview(WeeklyFocusUiState.Error)

@Preview(name = "Focus Unconfirmed checking", widthDp = 320, heightDp = 1400, locale = "kk", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun FocusCheckingPreview() = FocusReviewPreview(WeeklyFocusUiState.Checking)

@Preview(name = "Focus Unconfirmed recovery", widthDp = 320, heightDp = 2000, locale = "kk", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun FocusUnconfirmedPreview() = FocusReviewPreview(WeeklyFocusUiState.Unconfirmed)

@Preview(name = "Focus scrolled recovery", widthDp = 320, heightDp = 640, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun FocusFooterPreview() {
    OquTurboTheme {
        WeeklyReviewScreen(
            previewWeeklyReview().copy(focus = WeeklyFocusUiState.Error),
            {},
            {},
            {},
            {},
            {},
            {},
            listState =
                rememberLazyListState(
                    initialFirstVisibleItemIndex = 3,
                    initialFirstVisibleItemScrollOffset = 1500,
                ),
        )
    }
}
