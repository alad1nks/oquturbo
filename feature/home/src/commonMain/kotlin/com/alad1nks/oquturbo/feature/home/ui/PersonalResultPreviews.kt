package com.alad1nks.oquturbo.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameSeriesKey
import com.alad1nks.oquturbo.core.data.model.ProgressComparison
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

internal fun previewNoRecentResult() = PersonalResultState.Loaded(ProgressComparison.NoRecentSessions(19_973, 20_000))

private val ordinarySeries = GameSeriesKey(GameId.NumberSprint, GameModeId.NumberSprintClassic, null)
private val customSeries =
    GameSeriesKey(GameId.NumberSprint, GameModeId.NumberSprintCustom, "length:12;digits:0123456789")

private fun compared(previous: Int = 2, current: Int = 4, series: GameSeriesKey = ordinarySeries) =
    PersonalResultState.Loaded(
        ProgressComparison.Compared(19_973, 20_000, series, 11, previous, current, current - previous),
    )

private fun scarce(count: Int = 1, series: GameSeriesKey = ordinarySeries) =
    PersonalResultState.Loaded(ProgressComparison.InsufficientData(19_973, 20_000, series, count))

@Composable
private fun CardPreview(state: PersonalResultState, dark: Boolean = false) {
    OquTurboTheme(darkTheme = dark) {
        Column(modifier = Modifier.fillMaxSize().appBackground().padding(24.dp)) {
            PersonalResultCard(state, {}, {})
        }
    }
}

@Preview(name = "Personal result full Home", widthDp = 390, heightDp = 1700)
@ScreenshotPreview
@Composable
private fun ResultFullHomePreview() {
    OquTurboTheme {
        HomeScreen(HomeUiState(dailyTraining = previewDailyTraining(false), personalResult = compared()), {})
    }
}

@Preview(name = "Personal result loading", widthDp = 390, heightDp = 600)
@ScreenshotPreview
@Composable
private fun ResultLoadingPreview() {
    CardPreview(PersonalResultState.Loading)
}

@Preview(name = "Personal result error Russian", widthDp = 320, heightDp = 800, fontScale = 1.5f, locale = "ru")
@ScreenshotPreview
@Composable
private fun ResultErrorPreview() {
    CardPreview(PersonalResultState.Error, dark = true)
}

@Preview(name = "Personal result empty Kazakh", widthDp = 320, heightDp = 900, fontScale = 1.5f, locale = "kk")
@ScreenshotPreview
@Composable
private fun ResultEmptyPreview() {
    CardPreview(previewNoRecentResult())
}

@Preview(name = "Personal result one attempt", widthDp = 390, heightDp = 900)
@ScreenshotPreview
@Composable
private fun ResultOneAttemptPreview() {
    CardPreview(scarce())
}

@Preview(name = "Personal result custom nine", widthDp = 320, heightDp = 1500, fontScale = 1.5f, locale = "ru")
@ScreenshotPreview
@Composable
private fun ResultCustomNinePreview() {
    CardPreview(scarce(9, customSeries))
}

@Preview(name = "Personal result negative", widthDp = 390, heightDp = 1100, locale = "ru")
@ScreenshotPreview
@Composable
private fun ResultNegativePreview() {
    CardPreview(compared(previous = 4, current = 2))
}

@Preview(name = "Personal result equal Kazakh", widthDp = 390, heightDp = 1100, locale = "kk")
@ScreenshotPreview
@Composable
private fun ResultEqualPreview() {
    CardPreview(compared(previous = 4, current = 4), dark = true)
}

@Preview(name = "Personal result zero base", widthDp = 390, heightDp = 1100)
@ScreenshotPreview
@Composable
private fun ResultZeroBasePreview() {
    CardPreview(compared(previous = 0, current = 2))
}

@Preview(name = "Personal result English language", widthDp = 320, heightDp = 1500, fontScale = 1.5f, locale = "ru")
@ScreenshotPreview
@Composable
private fun ResultEnglishLanguagePreview() {
    CardPreview(compared(series = GameSeriesKey(GameId.WordFlow, GameModeId.WordFlowContext, "en")))
}

@Preview(name = "Personal result Russian language", widthDp = 320, heightDp = 1500, fontScale = 1.5f, locale = "kk")
@ScreenshotPreview
@Composable
private fun ResultRussianLanguagePreview() {
    CardPreview(compared(series = GameSeriesKey(GameId.WordFlow, GameModeId.WordFlowContext, "ru")))
}

@Preview(name = "Personal result unknown metadata", widthDp = 390, heightDp = 1800)
@ScreenshotPreview
@Composable
private fun ResultUnknownMetadataPreview() {
    OquTurboTheme {
        Column(
            modifier = Modifier.fillMaxSize().appBackground().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            listOf(
                customSeries.copy(variantId = null),
                GameSeriesKey(GameId.WordFlow, GameModeId.WordFlowContext, "unknown"),
                ordinarySeries.copy(variantId = "unrecognized"),
            ).forEach { PersonalResultCard(scarce(series = it), {}, {}) }
        }
    }
}

@Preview(name = "Personal result scrolled Home", widthDp = 320, heightDp = 640, fontScale = 1.5f, locale = "ru")
@ScreenshotPreview
@Composable
private fun ResultScrolledHomePreview() {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 3)
    LaunchedEffect(listState) {
        val (cardHeight, viewportHeight) =
            snapshotFlow {
                val layout = listState.layoutInfo
                layout.visibleItemsInfo.firstOrNull { it.key == "personal-result" }?.let {
                    it.size to layout.viewportSize.height
                }
            }.filterNotNull().first()
        // Show the result's lower half, independent of cards inserted after it.
        listState.scrollToItem(3, (cardHeight - viewportHeight / 2).coerceAtLeast(0))
    }
    OquTurboTheme {
        HomeScreen(
            uiState =
                HomeUiState(
                    dailyTraining = previewDailyTraining(false),
                    personalResult = compared(series = customSeries),
                ),
            onStartTrainingClick = {},
            listState = listState,
        )
    }
}

@Preview(name = "Personal result wide Home", widthDp = 800, heightDp = 1600)
@ScreenshotPreview
@Composable
private fun ResultWideHomePreview() {
    OquTurboTheme {
        HomeScreen(HomeUiState(dailyTraining = previewDailyTraining(false), personalResult = compared()), {})
    }
}
