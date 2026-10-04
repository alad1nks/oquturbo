package com.alad1nks.oquturbo.feature.profile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.practice.calculatePracticeRhythm
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PracticeProfileUiTest {
    @Test
    fun russianLowerBoundUsesTheEntirePhrasePluralForOneAndTwentyOne() =
        inLocale("ru") {
            runDesktopComposeUiTest {
                val count = mutableStateOf(1)
                setContent {
                    OquTurboTheme {
                        val start = 100L - count.value + 1
                        Text(
                            currentPracticeValue(
                                ProfilePracticeState.Ready(
                                    calculatePracticeRhythm(DayHistory(start, (start..100L).toList()), 100),
                                ),
                            ),
                        )
                    }
                }
                for ((number, expected) in listOf(
                    1 to "Не менее 1 дня",
                    2 to "Не менее 2 дней",
                    5 to "Не менее 5 дней",
                    11 to "Не менее 11 дней",
                    21 to "Не менее 21 дня",
                    22 to "Не менее 22 дней",
                )) {
                    runOnIdle { count.value = number }
                    onNodeWithText(expected).assertIsDisplayed()
                }
            }
        }

    @Test
    fun unavailableSummaryKeepsKnownAchievementCountAndOffersIndependentRetry() =
        inLocale("en") {
            runDesktopComposeUiTest(width = 320, height = 640) {
                var retries = 0
                val state =
                    ProfileUiState(
                        achievements =
                            listOf(
                                ProfileUiState.Achievement(AchievementId.FirstTraining, AchievementStatus.Earned),
                            ),
                    ).withPracticeHistory(ProfilePracticeState.Error)
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                        OquTurboTheme {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                ProfileSummarySection(state, {}, {}, onRetryPracticeHistory = { retries++ })
                            }
                        }
                    }
                }
                onAllNodesWithText("Unavailable").assertCountEquals(2)
                onNodeWithText("At least 1").assertExists()
                onNodeWithText("0 days").assertDoesNotExist()
                onNodeWithText("Retry").performScrollTo().assertIsDisplayed().performClick()
                assertEquals(1, retries)
            }
        }

    @Test
    fun standaloneSevenDayCardRetriesWithoutFalseProgressAndRetainsFutureEarnedEvidence() =
        inLocale("en") {
            runDesktopComposeUiTest(width = 320, height = 640) {
                val history = mutableStateOf<ProfilePracticeState>(ProfilePracticeState.Error)
                var retries = 0
                setContent {
                    OquTurboTheme {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            val state = ProfileUiState().withPracticeHistory(history.value)
                            AchievementCard(
                                state.achievements.single { it.id == AchievementId.SevenDayStreak },
                                history = history.value,
                                onRetryPracticeHistory = {
                                    retries++
                                    history.value = ProfilePracticeState.Loading
                                },
                                showFutureNote = true,
                            )
                        }
                    }
                }
                onNodeWithText("0 / 7").assertDoesNotExist()
                onNodeWithText("Retry").performScrollTo().performClick()
                assertEquals(1, retries)
                onNodeWithText("Loading practice history…").assertExists()
                runOnIdle {
                    history.value =
                        ProfilePracticeState.Ready(
                            calculatePracticeRhythm(DayHistory(80, (101L..107L).toList()), 100),
                        )
                }
                onNodeWithText("Confirmed by saved practice days").assertExists()
                onNodeWithText(
                    "History includes dates after today. Check your device date.",
                ).performScrollTo().assertIsDisplayed()
                onNodeWithText("Earned today").assertDoesNotExist()
            }
        }

    private fun inLocale(language: String, block: () -> Unit) {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag(language))
            block()
        } finally {
            Locale.setDefault(original)
        }
    }
}
