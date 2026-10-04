package com.alad1nks.oquturbo.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.practice.calculatePracticeRhythm
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview

internal fun previewProfilePractice(
    start: Long = 19_900,
    days: List<Long> =
        (19_970L..19_976L).toList() +
            listOf(
                19_999,
                20_000,
            ),
) =
    ProfileDemoData.midRank.withPracticeHistory(
        ProfilePracticeState.Ready(calculatePracticeRhythm(DayHistory(start, days), 20_000)),
    )

@Composable
private fun PracticeProfileGallery(state: ProfileUiState, dark: Boolean = false) {
    OquTurboTheme(darkTheme = dark) {
        Column(
            Modifier.fillMaxSize().appBackground().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ProfileSummarySection(state, {}, {})
            AchievementCard(
                state.achievements.single { it.id == AchievementId.SevenDayStreak },
                history = state.practiceHistory,
            )
            if (state.practiceHistory == ProfilePracticeState.Loading) {
                AchievementCard(state.achievements.first { it.id != AchievementId.SevenDayStreak })
            }
        }
    }
}

@Preview(name = "Practice full Profile", widthDp = 390, heightDp = 2300)
@ScreenshotPreview
@Composable
private fun PracticeFullProfilePreview() {
    OquTurboTheme {
        val state = previewProfilePractice()
        ProfileScreen(
            state.copy(achievements = state.achievements.sortedBy { it.id.ordinal }),
            {},
            {},
            {},
            {},
            {},
            {},
            {},
        )
    }
}

@Preview(name = "Practice profile lower bound", widthDp = 320, heightDp = 2100, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun PracticeLowerBoundPreview() {
    PracticeProfileGallery(previewProfilePractice(19_999, (19_970L..19_975L).toList() + listOf(19_999, 20_000)))
}

@Preview(name = "Practice profile unknown", widthDp = 320, heightDp = 2350, locale = "kk", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun PracticeUnknownProfilePreview() {
    PracticeProfileGallery(previewProfilePractice(20_000, emptyList()))
}

@Preview(name = "Practice profile error", widthDp = 320, heightDp = 2100, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun PracticeErrorProfilePreview() {
    PracticeProfileGallery(ProfileDemoData.midRank.withPracticeHistory(ProfilePracticeState.Error), true)
}

@Preview(name = "Practice profile loading", widthDp = 390, heightDp = 1300)
@ScreenshotPreview
@Composable
private fun PracticeLoadingProfilePreview() {
    PracticeProfileGallery(ProfileDemoData.midRank.withPracticeHistory(ProfilePracticeState.Loading))
}

@Preview(name = "Practice profile future earned", widthDp = 390, heightDp = 1500)
@ScreenshotPreview
@Composable
private fun PracticeFutureProfilePreview() {
    PracticeProfileGallery(previewProfilePractice(days = (20_001L..20_007L).toList()))
}

@Preview(name = "Practice all achievements unavailable", widthDp = 320, heightDp = 900, locale = "kk", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun PracticeAchievementUnavailablePreview() {
    OquTurboTheme {
        val state = ProfileDemoData.midRank.withPracticeHistory(ProfilePracticeState.Error)
        Column(Modifier.fillMaxSize().appBackground().padding(24.dp)) {
            AchievementCard(
                state.achievements.single { it.id == AchievementId.SevenDayStreak },
                history = state.practiceHistory,
                showFutureNote = true,
            )
        }
    }
}

@Preview(name = "Practice profile wide", widthDp = 800, heightDp = 1000)
@ScreenshotPreview
@Composable
private fun PracticeWideProfilePreview() {
    PracticeProfileGallery(previewProfilePractice())
}
