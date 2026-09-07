package com.alad1nks.oquturbo.feature.stats.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelStore
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.feature.stats.data.StatsDataSource
import com.alad1nks.oquturbo.feature.stats.model.GameTrend
import com.alad1nks.oquturbo.feature.stats.model.ModeTrend
import com.alad1nks.oquturbo.feature.stats.model.StatsDetailUiState
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriodSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class StatsDetailSemanticsTest {
    @Test
    fun bothSelectorsRemainReachableAndSelectedInEveryCompactLocale() {
        val original = Locale.getDefault()
        try {
            listOf(
                "en" to listOf("7 days", "30 days", "All time"),
                "ru" to listOf("7 дней", "30 дней", "Всё время"),
                "kk" to listOf("7 күн", "30 күн", "Барлық уақыт"),
            ).forEach { (locale, labels) ->
                Locale.setDefault(Locale.forLanguageTag(locale))
                listOf(false, true).forEach { modeDetail ->
                    runDesktopComposeUiTest(width = 320, height = 1200) {
                        val state = mutableStateOf(emptyState())
                        setContent {
                            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                                OquTurboTheme {
                                    if (modeDetail) {
                                        StatsModeDetailScreen(
                                            state.value,
                                            onPeriodSelected = { state.value = state.value.copy(period = it) },
                                            onBackClick = {},
                                        )
                                    } else {
                                        StatsGameDetailScreen(
                                            state.value,
                                            onPeriodSelected = { state.value = state.value.copy(period = it) },
                                            onBackClick = {},
                                            onModeClick = { _, _, _ -> },
                                        )
                                    }
                                }
                            }
                        }
                        if (modeDetail) {
                            val title =
                                when (locale) {
                                    "ru" -> "Пользователский"
                                    "kk" -> "Пайдаланушы режимі"
                                    else -> "Custom"
                                }
                            onNodeWithText(title).assertIsDisplayed()
                        }
                        onNodeWithText(labels.first()).assertIsSelected()
                        labels.forEachIndexed { index, label ->
                            onNodeWithText(
                                label,
                            ).performScrollTo().assertIsDisplayed().performClick().assertIsSelected()
                            onAllNodes(isSelected()).assertCountEquals(1)
                            runOnIdle { assertEquals(StatsPeriod.entries[index], state.value.period) }
                        }
                    }
                }
            }
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun modeCardUsesRenderedPeriodAfterSelectionAndBackCallbackIsPreserved() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            runDesktopComposeUiTest(width = 320, height = 1200) {
                val streams = StatsPeriod.entries.associateWith { MutableSharedFlow<StatsPeriodSnapshot>(replay = 1) }
                val snapshot =
                    StatsPeriodSnapshot.Empty.copy(
                        trends =
                            listOf(
                                GameTrend(
                                    StatsGame.NumberSprint,
                                    listOf(
                                        ModeTrend(
                                            StatsMode.Classic,
                                            scores = listOf(2, 4),
                                            record = 4,
                                            lastResult = 4,
                                            averageResult = 3,
                                            gamesPlayed = 2,
                                        ),
                                    ),
                                ),
                            ),
                    )
                streams.getValue(StatsPeriod.SevenDays).tryEmit(snapshot)
                val source =
                    object : StatsDataSource {
                        override fun observeSnapshot(period: StatsPeriod) = streams.getValue(period)
                    }
                val vm = StatsGameDetailViewModel(StatsGame.NumberSprint, StatsPeriod.SevenDays, source)
                val store = ViewModelStore().apply { put("detail", vm) }
                var navigation: Triple<StatsGame, StatsMode, StatsPeriod>? = null
                var backs = 0
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        OquTurboTheme {
                            StatsGameDetailRouteContent(
                                viewModel = vm,
                                onBackClick = { backs++ },
                                onModeClick = { game, mode, period -> navigation = Triple(game, mode, period) },
                            )
                        }
                    }
                }
                waitUntil { vm.uiState.value.modes.isNotEmpty() }
                onNodeWithText("30 days").performClick()
                onNodeWithText("7 days").assertIsSelected()
                onNodeWithText("Classic", substring = true).performScrollTo().performClick()
                runOnIdle {
                    assertEquals(Triple(StatsGame.NumberSprint, StatsMode.Classic, StatsPeriod.SevenDays), navigation)
                    streams.getValue(StatsPeriod.ThirtyDays).tryEmit(snapshot)
                }
                waitUntil { vm.uiState.value.period == StatsPeriod.ThirtyDays }
                onNodeWithText("30 days").assertIsSelected()
                onNodeWithText("Classic", substring = true).performScrollTo().performClick()
                runOnIdle {
                    assertEquals(
                        Triple(StatsGame.NumberSprint, StatsMode.Classic, StatsPeriod.ThirtyDays),
                        navigation,
                    )
                }
                onAllNodes(hasClickAction()).onFirst().performClick()
                runOnIdle {
                    assertEquals(1, backs)
                    store.clear()
                }
            }
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun compactRussianCustomVariantsHaveReadableCompleteLabels() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ru"))
            runDesktopComposeUiTest(width = 320, height = 1600) {
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        OquTurboTheme {
                            StatsModeDetailScreen(
                                emptyState().copy(
                                    modes =
                                        listOf("length:3;digits:012", "length:4;digits:345").map { variant ->
                                            ModeTrend(StatsMode.Custom, variant, listOf(2, 4), 4, 4, 3, 2)
                                        },
                                ),
                                onPeriodSelected = {},
                                onBackClick = {},
                            )
                        }
                    }
                }
                listOf("012", "345").forEach { digits ->
                    val label = onNodeWithText(digits, substring = true)
                    label.performScrollTo().assertIsDisplayed()
                    val layouts = mutableListOf<TextLayoutResult>()
                    label.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    assertFalse(layouts.single().hasVisualOverflow)
                }
            }
        } finally {
            Locale.setDefault(original)
        }
    }

    private fun emptyState() =
        StatsDetailUiState(StatsGame.NumberSprint, StatsPeriod.SevenDays, emptyList(), StatsMode.Custom)
}
