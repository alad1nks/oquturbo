package com.alad1nks.oquturbo.feature.numbertrail.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailBoard
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailFailure
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailPhase
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailState

@Composable
private fun SummaryPreview(timeout: Boolean = false, early: Boolean = false, scrolled: Boolean = false) {
    OquTurboTheme(darkTheme = timeout) {
        NumberTrailScreen(
            NumberTrailUiState(
                game =
                    NumberTrailState(
                        phase = NumberTrailPhase.Result,
                        board = NumberTrailBoard(3, 2, listOf(4, 1, 3, 2), 12_000, target = if (early) 1 else 3),
                        score = if (early) 0 else 10,
                        completedBoards = if (early) 0 else 2,
                        activeDurationMillis =
                            if (early) {
                                999
                            } else if (timeout) {
                                7_505_000
                            } else {
                                65_300
                            },
                        failure = if (timeout) NumberTrailFailure.Timeout else NumberTrailFailure.Wrong,
                        selectedNumber = if (timeout) null else 4,
                    ),
                record = 12,
                isRecordLoading = false,
                saveStatus =
                    if (early) {
                        NumberTrailSaveStatus.Pending
                    } else if (timeout) {
                        NumberTrailSaveStatus.Failed
                    } else {
                        NumberTrailSaveStatus.Saved
                    },
            ),
            {},
            { _, _ -> },
            {},
            scrollState = rememberScrollState(if (scrolled) Int.MAX_VALUE else 0),
        )
    }
}

@Preview(name = "Number Trail summary ordinary", widthDp = 390, heightDp = 1100, locale = "en")
@ScreenshotPreview
@Composable
private fun NumberTrailSummaryOrdinaryPreview() = SummaryPreview()

@Preview(name = "Number Trail summary compact ru", widthDp = 320, heightDp = 600, fontScale = 1.5f, locale = "ru")
@ScreenshotPreview
@Composable
private fun NumberTrailSummaryRussianPreview() = SummaryPreview(timeout = true)

@Preview(name = "Number Trail summary scrolled ru", widthDp = 320, heightDp = 600, fontScale = 1.5f, locale = "ru")
@ScreenshotPreview
@Composable
private fun NumberTrailSummaryRussianScrolledPreview() = SummaryPreview(timeout = true, scrolled = true)

@Preview(name = "Number Trail summary compact kk", widthDp = 320, heightDp = 600, fontScale = 1.5f, locale = "kk")
@ScreenshotPreview
@Composable
private fun NumberTrailSummaryKazakhPreview() = SummaryPreview(early = true)

@Preview(name = "Number Trail summary scrolled kk", widthDp = 320, heightDp = 600, fontScale = 1.5f, locale = "kk")
@ScreenshotPreview
@Composable
private fun NumberTrailSummaryKazakhScrolledPreview() = SummaryPreview(early = true, scrolled = true)
