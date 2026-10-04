package com.alad1nks.oquturbo.shared

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.alad1nks.oquturbo.core.storage.common.AppPreferences
import com.alad1nks.oquturbo.feature.home.navigation.HomeRoute
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.navigation.StatsModeDetailRoute
import com.alad1nks.oquturbo.feature.stats.navigation.WeeklyReviewRoute
import com.alad1nks.oquturbo.shared.ui.OquTurboAppState
import com.alad1nks.oquturbo.shared.ui.rememberOquTurboAppState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.module
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Executes the production App graph, DI assembly, Scaffold and all three real route contents. */
@OptIn(ExperimentalTestApi::class)
class WeeklyReviewNavigationTest {
    @Test
    fun realRootGraphReturnsToSameHomeAndReviewAnchorsAndCoalescesDoubleTaps() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            runDesktopComposeUiTest(width = 320, height = 640) {
                val preferences = ReviewPreferences()
                val platform = listOf(module { single<AppPreferences> { preferences } })
                val common = getCommonModules()
                lateinit var appState: OquTurboAppState
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                        appState = rememberOquTurboAppState()
                        App(appState, common, platform)
                    }
                }
                waitUntil(
                    timeoutMillis = 10_000,
                ) { onAllNodes(hasText("Loading practice history…")).fetchSemanticsNodes().isEmpty() }
                onNode(hasScrollAction()).performScrollToNode(hasText("7-day review"))
                val entry = onNodeWithText("7-day review").assertIsDisplayed()
                val homeTop = entry.fetchSemanticsNode().boundsInRoot.top
                val homeId = runOnIdle { appState.navController.currentBackStackEntry!!.id }
                val bytes = preferences.snapshot()
                // Invoke the displayed node's same action twice before recomposition, as rapid taps do.
                val openReview = entry.fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
                runOnIdle {
                    openReview()
                    openReview()
                }
                runOnIdle {
                    assertTrue(appState.navController.currentBackStackEntry!!.destination.hasRoute<WeeklyReviewRoute>())
                    assertEquals(
                        1,
                        appState.navController.currentBackStack.value.count {
                            it.destination.hasRoute<WeeklyReviewRoute>()
                        },
                    )
                    assertEquals(
                        1,
                        appState.navController.currentBackStack.value.count { it.destination.hasRoute<HomeRoute>() },
                    )
                }
                waitUntil(
                    timeoutMillis = 10_000,
                ) { onAllNodes(hasText("Loading saved attempts…")).fetchSemanticsNodes().isEmpty() }
                onNode(hasScrollAction()).performScrollToNode(hasText("Mode statistics"))
                val statistics = onNodeWithText("Mode statistics").assertIsDisplayed()
                val reviewTop = statistics.fetchSemanticsNode().boundsInRoot.top
                val reviewId = runOnIdle { appState.navController.currentBackStackEntry!!.id }
                val openMode = statistics.fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
                runOnIdle {
                    openMode()
                    openMode()
                }
                runOnIdle {
                    assertEquals(
                        StatsModeDetailRoute(StatsGame.NumberSprint, StatsMode.Binary, StatsPeriod.ThirtyDays),
                        appState.navController.currentBackStackEntry!!.toRoute<StatsModeDetailRoute>(),
                    )
                    assertEquals(
                        1,
                        appState.navController.currentBackStack.value.count {
                            it.destination.hasRoute<StatsModeDetailRoute>()
                        },
                    )
                }
                onNodeWithText("30 days").assertIsSelected()
                onAllNodes(hasClickAction()).onFirst().performClick() // Production top-bar Back.
                runOnIdle { assertEquals(reviewId, appState.navController.currentBackStackEntry!!.id) }
                waitUntil(
                    timeoutMillis = 10_000,
                ) { onAllNodes(hasText("Mode statistics")).fetchSemanticsNodes().isNotEmpty() }
                onNodeWithText("Mode statistics").assertIsDisplayed()
                assertEquals(reviewTop, onNodeWithText("Mode statistics").fetchSemanticsNode().boundsInRoot.top, 1f)
                onNode(hasScrollAction()).performScrollToNode(hasText("Home"))
                onNodeWithText("Home").assertIsDisplayed().performClick()
                runOnIdle {
                    assertEquals(homeId, appState.navController.currentBackStackEntry!!.id)
                    assertEquals(
                        1,
                        appState.navController.currentBackStack.value.count { it.destination.hasRoute<HomeRoute>() },
                    )
                    assertEquals(
                        0,
                        appState.navController.currentBackStack.value.count {
                            it.destination.hasRoute<WeeklyReviewRoute>()
                        },
                    )
                }
                waitUntil(
                    timeoutMillis = 10_000,
                ) { onAllNodes(hasText("Loading practice history…")).fetchSemanticsNodes().isEmpty() }
                onNodeWithText("7-day review").assertIsDisplayed()
                assertEquals(homeTop, onNodeWithText("7-day review").fetchSemanticsNode().boundsInRoot.top, 1f)
                assertEquals(bytes, preferences.snapshot())
                // Ordinary top-bar Back has the same existing-Home destination as the footer.
                onNodeWithText("7-day review").performClick()
                onAllNodes(hasClickAction()).onFirst().performClick()
                runOnIdle { assertEquals(homeId, appState.navController.currentBackStackEntry!!.id) }
            }
        } finally {
            Locale.setDefault(original)
        }
    }

    private class ReviewPreferences : AppPreferences {
        private val values = ConcurrentHashMap<String, MutableStateFlow<String?>>()

        init {
            val today = System.currentTimeMillis() / 86_400_000L
            val sessions =
                (0..9).joinToString(",") { index ->
                    """{"game":"NumberSprint",
                "mode":"NumberSprintBinary",
                "score":${if (index < 5) 2 else 4},
                "correctAnswers":0,
                "durationMillis":0,
                "completedAtEpochMillis":${today * 86_400_000L + index},
                "completedEpochDay":$today,
                "isNewRecord":false}"""
                }
            values["language"] = MutableStateFlow("en")
            values["game_sessions_v1"] =
                MutableStateFlow(
                    """{"sessions":[$sessions],
                "practiceHistory":{"version":1,
                "trackingStartedEpochDay":${today - 20},
                "completedEpochDays":[$today]}}""",
                )
            values["daily_training_progress_v1"] =
                MutableStateFlow(
                    """{"completionHistory":{"version":1,
                "trackingStartedEpochDay":${today - 20},
                "completedEpochDays":[]}}""",
                )
        }

        fun snapshot() = values.mapValues { it.value.value }

        override fun getString(key: String): Flow<String?> = values.getOrPut(key) { MutableStateFlow(null) }

        override suspend fun setString(key: String, value: String) {
            values.getOrPut(key) { MutableStateFlow(null) }.value = value
        }

        override fun getInt(key: String): Flow<Int?> = flowOf(null)

        override fun getBoolean(key: String): Flow<Boolean?> = flowOf(false)

        override suspend fun setInt(key: String, value: Int) {
            error("No game played")
        }

        override suspend fun setBoolean(key: String, value: Boolean) {
            error("No settings changed")
        }
    }
}
