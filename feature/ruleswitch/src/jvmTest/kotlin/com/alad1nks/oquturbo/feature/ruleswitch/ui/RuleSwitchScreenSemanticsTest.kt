package com.alad1nks.oquturbo.feature.ruleswitch.ui

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.feature.ruleswitch.model.RuleSwitchFailure
import com.alad1nks.oquturbo.feature.ruleswitch.model.RuleSwitchPhase
import com.alad1nks.oquturbo.feature.ruleswitch.model.SwitchAnswer
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RuleSwitchScreenSemanticsTest {
    @Test
    fun bothRulesExposeOrderedLabelsAndTimeoutHasNoChosenAnswer() =
        runComposeUiTest {
            val state = mutableStateOf(ruleSwitchPreviewState(RuleSwitchPhase.Active))
            setContent { OquTurboTheme { RuleSwitchScreen(state.value, {}, { _, _ -> }, null) } }
            onNodeWithText("Even or odd?").assertExists()
            val even = onNodeWithTag("answer-Even").fetchSemanticsNode().boundsInRoot
            val odd = onNodeWithTag("answer-Odd").fetchSemanticsNode().boundsInRoot
            assertTrue(even.left < odd.left)
            state.value = ruleSwitchPreviewState(RuleSwitchPhase.Active, 4)
            onNodeWithText("Below or above 5?").assertExists()
            val below = onNodeWithTag("answer-Below").fetchSemanticsNode().boundsInRoot
            val above = onNodeWithTag("answer-Above").fetchSemanticsNode().boundsInRoot
            assertTrue(below.left < above.left)
            onNodeWithText("Even").assertDoesNotExist()
            state.value =
                ruleSwitchPreviewState(
                    RuleSwitchPhase.Result,
                    failure = RuleSwitchFailure.Timeout,
                    save = RuleSwitchSaveStatus.Saved,
                )
            onNodeWithTag("chosen", useUnmergedTree = true).assertDoesNotExist()
            onNodeWithText("Your answer:", substring = true).assertDoesNotExist()
            onNodeWithContentDescription("Back to Games").assertDoesNotExist()
        }

    @Test
    fun failedRecordLoadOffersRetryAndStandaloneReadyHasNoBack() =
        runComposeUiTest {
            var reloads = 0
            setContent {
                OquTurboTheme {
                    RuleSwitchScreen(RuleSwitchUiState(isRecordLoading = false, recordLoadFailed = true), {
                    }, { _, _ -> }, null, onReloadClick = { reloads++ })
                }
            }
            onNodeWithText("Start").assertIsNotEnabled()
            onNodeWithText("Retry loading").performScrollTo().performClick()
            assertEquals(1, reloads)
            onNodeWithContentDescription("Back to Games").assertDoesNotExist()
        }

    @Test
    fun compactStageFiveKeepsWholeFieldAndAllAnswersVisibleWithStandardTargets() =
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    OquTurboTheme {
                        RuleSwitchScreen(
                            ruleSwitchPreviewState(RuleSwitchPhase.Active, 5),
                            {},
                            { _, _ -> },
                            {},
                            modifier = Modifier.requiredSize(320.dp, 640.dp),
                        )
                    }
                }
            }
            onNodeWithTag("digit").assertIsDisplayed()
            for (n in listOf("Even", "Odd")) {
                val node = onNodeWithTag("answer-$n").assertIsDisplayed().assertIsEnabled().fetchSemanticsNode()
                assertTrue(node.boundsInRoot.height >= 48)
                assertTrue(node.boundsInRoot.bottom <= 640)
            }
            onNodeWithContentDescription("Pause").assertIsDisplayed()
        }

    @Test
    fun pauseRemovesFieldTargetAndAnswersAndResumeRetainsChoices() =
        runComposeUiTest {
            val initial = ruleSwitchPreviewState(RuleSwitchPhase.Active)
            val state = mutableStateOf(initial)
            var answered: SwitchAnswer? = null
            setContent {
                OquTurboTheme {
                    RuleSwitchScreen(
                        state.value,
                        {},
                        { _, value -> answered = value },
                        null,
                        onPauseClick = {
                            state.value =
                                initial.copy(
                                    game = initial.game.copy(phase = RuleSwitchPhase.Paused),
                                )
                        },
                        onResumeClick = { state.value = initial },
                    )
                }
            }
            onNodeWithTag("answer-Even").performClick()
            assertEquals(SwitchAnswer.Even, answered)
            onNodeWithContentDescription("Pause").performClick()
            onNodeWithTag("digit").assertDoesNotExist()
            onNodeWithTag("rule").assertDoesNotExist()
            for (n in listOf("Even", "Odd", "Below", "Above")) onNodeWithTag("answer-$n").assertDoesNotExist()
            onNodeWithContentDescription("Back to Games").assertDoesNotExist()
            onNodeWithText("Resume").performClick()
            onNodeWithTag("answer-Even").assertIsEnabled()
        }

    @Test
    fun unfinishedExitNamesActualHubOrStandaloneDestination() =
        runComposeUiTest {
            val hub = mutableStateOf(true)
            var exits = 0
            setContent {
                OquTurboTheme {
                    RuleSwitchScreen(
                        ruleSwitchPreviewState(RuleSwitchPhase.Paused),
                        {},
                        { _, _ -> },
                        if (hub.value) ({}) else null,
                        onExitClick = { exits++ },
                    )
                }
            }
            onNodeWithText("Back to Games").performClick()
            assertEquals(1, exits)
            onNodeWithText("Exit to Ready").assertDoesNotExist()
            hub.value = false
            onNodeWithText("Exit to Ready").performClick()
            assertEquals(2, exits)
            onNodeWithText("Back to Games").assertDoesNotExist()
        }

    @Test
    fun pausedFeedbackHidesTaskAndBackgroundResumeIsDisabled() =
        runComposeUiTest {
            val feedback = ruleSwitchPreviewState(RuleSwitchPhase.Correct, 4)
            val state =
                mutableStateOf(
                    feedback.copy(
                        game =
                            feedback.game.copy(
                                phase = RuleSwitchPhase.Paused,
                                returnPhase = RuleSwitchPhase.Correct,
                            ),
                        isForeground = false,
                    ),
                )
            setContent { OquTurboTheme { RuleSwitchScreen(state.value, {}, { _, _ -> }, null) } }
            onNodeWithTag("digit").assertDoesNotExist()
            onNodeWithTag("rule").assertDoesNotExist()
            onNodeWithText("Correct").assertDoesNotExist()
            for (answer in listOf("Even", "Odd", "Below", "Above")) {
                onNodeWithTag("answer-$answer").assertDoesNotExist()
            }
            onNodeWithText("Resume").assertIsNotEnabled()
            state.value = state.value.copy(isForeground = true)
            onNodeWithText("Resume").assertIsEnabled()
            onNodeWithTag("digit").assertDoesNotExist()
        }

    @Test
    fun correctFeedbackDisablesAllOptionsButPreservesPause() =
        runComposeUiTest {
            setContent {
                OquTurboTheme {
                    RuleSwitchScreen(ruleSwitchPreviewState(RuleSwitchPhase.Correct), {}, { _, _ -> }, {})
                }
            }
            for (n in listOf("Even", "Odd")) onNodeWithTag("answer-$n").assertIsNotEnabled()
            onNodeWithText("Correct").assertExists()
            onNodeWithContentDescription("Pause").assertIsEnabled()
        }

    @Test
    fun savingAndLoadFailureHaveTruthfulSingleRecoveryActions() =
        runComposeUiTest {
            val state = mutableStateOf(RuleSwitchUiState())
            var retries = 0
            setContent {
                OquTurboTheme {
                    RuleSwitchScreen(state.value, {}, { _, _ -> }, null, onRetrySaveClick = { retries++ })
                }
            }
            onNodeWithText("Start").assertIsNotEnabled()
            state.value =
                ruleSwitchPreviewState(
                    RuleSwitchPhase.Result,
                    failure = RuleSwitchFailure.Timeout,
                    save = RuleSwitchSaveStatus.Pending,
                )
            onNodeWithText("Play again").assertIsNotEnabled()
            state.value = state.value.copy(saveStatus = RuleSwitchSaveStatus.Failed)
            onNodeWithText("Retry saving").performClick()
            assertEquals(1, retries)
            onNodeWithText("New record!").assertDoesNotExist()
        }

    @Test
    fun terminalAndSaveTransitionsExposePoliteLocalizedAnnouncements() =
        runComposeUiTest {
            val state = mutableStateOf(ruleSwitchPreviewState(RuleSwitchPhase.Active))
            setContent {
                OquTurboTheme {
                    RuleSwitchScreen(state.value, {}, { _, _ -> }, null)
                }
            }
            onNodeWithTag("result-announcement").assertDoesNotExist()
            for (failure in listOf(RuleSwitchFailure.Wrong, RuleSwitchFailure.Timeout)) {
                state.value =
                    ruleSwitchPreviewState(
                        RuleSwitchPhase.Result,
                        failure = failure,
                        save = RuleSwitchSaveStatus.Pending,
                    )
                val announcement = onNodeWithTag("result-announcement")
                announcement.assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
                val title = if (failure == RuleSwitchFailure.Timeout) "Time’s up" else "Wrong answer"
                assertTrue(announcement.fetchSemanticsNode().config[SemanticsProperties.Text].any { it.text == title })
                assertTrue(
                    announcement.fetchSemanticsNode().config[SemanticsProperties.Text].any {
                        it.text == "Saving result…"
                    },
                )
                state.value = state.value.copy(saveStatus = RuleSwitchSaveStatus.Failed)
                val text = announcement.fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }
                assertTrue("Result not saved. Retry saving to keep your progress." in text)
                assertTrue("Saving result…" !in text)
            }
        }

    @Test
    fun enlargedTextKeepsAnswersReachableWithoutClipping() =
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    OquTurboTheme {
                        RuleSwitchScreen(
                            ruleSwitchPreviewState(RuleSwitchPhase.Active, 5),
                            {},
                            { _, _ -> },
                            {},
                            modifier = Modifier.requiredSize(320.dp, 600.dp),
                        )
                    }
                }
            }
            onNodeWithTag("answer-Odd").performScrollTo().assertIsDisplayed().assertIsEnabled()
            onNodeWithContentDescription("Pause").performScrollTo().assertIsDisplayed()
        }

    @Test
    fun compactRussianAndKazakhReadyKeepStartReachable() {
        val originalLocale = Locale.getDefault()
        try {
            for ((locale, start) in listOf("ru" to "Начать", "kk" to "Бастау")) {
                Locale.setDefault(Locale.forLanguageTag(locale))
                runComposeUiTest {
                    var starts = 0
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(1f)) {
                            OquTurboTheme {
                                RuleSwitchScreen(
                                    ruleSwitchPreviewState(),
                                    { starts++ },
                                    { _, _ -> },
                                    {},
                                    modifier = Modifier.requiredSize(320.dp, 640.dp),
                                )
                            }
                        }
                    }
                    onNodeWithText(start).performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
                    assertEquals(1, starts)
                }
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun enlargedRussianActiveKeepsBothAnswersReachable() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ru"))
            runComposeUiTest {
                var answered: SwitchAnswer? = null
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                        OquTurboTheme {
                            val state = ruleSwitchPreviewState(RuleSwitchPhase.Active, 5)
                            RuleSwitchScreen(
                                state.copy(game = state.game.copy(score = 12344), record = 23456),
                                {},
                                { _, answer -> answered = answer },
                                {},
                                modifier = Modifier.requiredSize(320.dp, 640.dp),
                            )
                        }
                    }
                }
                for (answer in listOf(SwitchAnswer.Even, SwitchAnswer.Odd)) {
                    onNodeWithTag("answer-$answer")
                        .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
                    assertEquals(answer, answered)
                }
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun enlargedKazakhResultKeepsReplayAndBackReachable() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("kk"))
            runComposeUiTest {
                var replayCount = 0
                var backCount = 0
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                        OquTurboTheme {
                            RuleSwitchScreen(
                                ruleSwitchPreviewState(
                                    RuleSwitchPhase.Result,
                                    5,
                                    RuleSwitchFailure.Wrong,
                                    RuleSwitchSaveStatus.Saved,
                                ).copy(record = 10),
                                { replayCount++ },
                                { _, _ -> },
                                { backCount++ },
                                modifier = Modifier.requiredSize(320.dp, 600.dp),
                            )
                        }
                    }
                }
                onNodeWithText("Қайта ойнау").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
                assertEquals(1, replayCount)
                onNodeWithText("Ойындарға оралу").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
                assertEquals(1, backCount)
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
