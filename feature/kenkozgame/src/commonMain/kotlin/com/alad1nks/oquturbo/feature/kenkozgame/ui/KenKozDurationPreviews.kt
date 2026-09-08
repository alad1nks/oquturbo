package com.alad1nks.oquturbo.feature.kenkozgame.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode

@Preview(name = "Wide Eye — duration Characters", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun KenKozDurationCharactersPreview() {
    KenKozDurationPreview(
        KenKozGameUiState(
            mode = KenKozGameMode.Characters,
            phase = KenKozGameUiState.Phase.Mistake,
            score = 0,
            record = 8,
            isNewRecord = false,
            completedDurationMillis = 999,
            correctAnswer = "A",
            selectedAnswer = "B",
            trainingRequiredScore = null,
            isTrainingCompletionReady = false,
        ),
    )
}

@Preview(name = "Wide Eye — duration Words", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun KenKozDurationWordsPreview() {
    KenKozDurationPreview(
        KenKozGameUiState(
            mode = KenKozGameMode.Words,
            phase = KenKozGameUiState.Phase.Mistake,
            score = 4,
            record = 8,
            isNewRecord = false,
            completedDurationMillis = 59999,
            correctAnswer = "mountain",
            selectedAnswer = "morning",
            trainingRequiredScore = null,
            isTrainingCompletionReady = false,
        ),
    )
}

@Preview(name = "Wide Eye — duration Difference", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun KenKozDurationDifferencePreview() {
    KenKozDurationPreview(
        KenKozGameUiState(
            mode = KenKozGameMode.FindDifference,
            phase = KenKozGameUiState.Phase.Mistake,
            score = 4,
            record = 8,
            isNewRecord = false,
            completedDurationMillis = 60000,
            correctAnswer = "Top",
            selectedAnswer = "Left",
            trainingRequiredScore = null,
            isTrainingCompletionReady = false,
        ),
    )
}

@Preview(name = "Wide Eye — duration NewRecord", widthDp = 320, heightDp = 844, locale = "ru")
@ScreenshotPreview
@Composable
private fun KenKozDurationNewRecordPreview() {
    KenKozDurationPreview(
        KenKozGameUiState(
            mode = KenKozGameMode.WideLine,
            phase = KenKozGameUiState.Phase.Mistake,
            score = 8,
            record = 8,
            isNewRecord = true,
            completedDurationMillis = 61999,
            correctAnswer = "mountain",
            selectedAnswer = "morning",
            trainingRequiredScore = null,
            isTrainingCompletionReady = false,
        ),
    )
}

@Preview(name = "Wide Eye — duration TrainingRetry", widthDp = 320, heightDp = 640, locale = "ru")
@ScreenshotPreview
@Composable
private fun KenKozDurationTrainingRetryPreview() {
    KenKozDurationPreview(
        KenKozGameUiState(
            mode = KenKozGameMode.Words,
            phase = KenKozGameUiState.Phase.Mistake,
            score = 0,
            record = 8,
            isNewRecord = false,
            completedDurationMillis = 21000,
            correctAnswer = "mountain",
            selectedAnswer = "morning",
            trainingRequiredScore = 5,
            isTrainingCompletionReady = true,
        ),
    )
}

@Preview(name = "Wide Eye — duration TrainingPending", widthDp = 320, heightDp = 844, locale = "ru")
@ScreenshotPreview
@Composable
private fun KenKozDurationTrainingPendingPreview() {
    KenKozDurationPreview(
        KenKozGameUiState(
            mode = KenKozGameMode.FindDifference,
            phase = KenKozGameUiState.Phase.Mistake,
            score = 5,
            record = 8,
            isNewRecord = false,
            completedDurationMillis = 61000,
            correctAnswer = "Top",
            selectedAnswer = "Left",
            trainingRequiredScore = 5,
            isTrainingCompletionReady = false,
        ),
    )
}

@Preview(name = "Wide Eye — duration TrainingFinal", widthDp = 320, heightDp = 844, locale = "kk")
@ScreenshotPreview
@Composable
private fun KenKozDurationTrainingFinalPreview() {
    KenKozDurationPreview(
        KenKozGameUiState(
            mode = KenKozGameMode.WideLine,
            phase = KenKozGameUiState.Phase.Mistake,
            score = 5,
            record = 8,
            isNewRecord = false,
            completedDurationMillis = Long.MAX_VALUE,
            correctAnswer = "mountain",
            selectedAnswer = "morning",
            trainingRequiredScore = 5,
            isTrainingCompletionReady = true,
        ),
    )
}

@Composable
private fun KenKozDurationPreview(state: KenKozGameUiState) {
    OquTurboTheme {
        KenKozGameScreen(
            uiState = state,
            onBackClick = {},
            onStartClick = {},
            onTrainingContinueClick = {},
            onAnswerClick = {},
        )
    }
}
