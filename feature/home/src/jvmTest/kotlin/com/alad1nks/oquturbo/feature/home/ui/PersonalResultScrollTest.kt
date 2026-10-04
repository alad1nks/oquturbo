package com.alad1nks.oquturbo.feature.home.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameSeriesKey
import com.alad1nks.oquturbo.core.data.model.ProgressComparison
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.resources.AppResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PersonalResultScrollTest {
    @Test
    fun backRestoresInsufficientCardPositionThroughFreshLoading() =
        verifyLoadingPosition(
            ProgressComparison.InsufficientData(19_973, 20_000, series, 9),
            recreateHome = true,
        )

    @Test
    fun refreshingComparedCardDoesNotClampPositionOrShowOldNumbers() =
        verifyLoadingPosition(
            ProgressComparison.Compared(19_973, 20_000, series, 10, 2, 4, 2),
            recreateHome = false,
        )

    @Test
    fun kazakhComparedCardRetainsPositionAcrossDetailScaffoldResize() = verifyScaffoldPosition(127f)

    @Test
    fun kazakhComparedCardRetainsNearEndPositionAcrossDetailScaffoldResize() = verifyScaffoldPosition(220f)

    @Test
    fun kazakhComparedCardRetainsEndPositionThroughFreshRead() =
        verifyScaffoldPosition(0f, scrollToEnd = true)

    @Test
    fun kazakhComparedCardRetainsEndPositionOnImmediateReadyReturn() =
        verifyScaffoldPosition(0f, scrollToEnd = true, freshRead = false)

    private fun verifyScaffoldPosition(
        additionalScroll: Float,
        scrollToEnd: Boolean = false,
        freshRead: Boolean = true,
    ) {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("kk"))
            runDesktopComposeUiTest(width = 320, height = if (scrollToEnd) 1200 else 640) {
                val ready =
                    HomeUiState(
                        dailyTraining = previewDailyTraining(false),
                        personalResult =
                            PersonalResultState.Loaded(
                                ProgressComparison.Compared(
                                    19_973,
                                    20_000,
                                    GameSeriesKey(GameId.WordFlow, GameModeId.WordFlowContext, "ru"),
                                    10,
                                    2,
                                    4,
                                    2,
                                ),
                            ),
                    )
                val state = mutableStateOf(ready)
                lateinit var list: LazyListState
                lateinit var scope: CoroutineScope
                lateinit var navigation: NavHostController
                val viewportHeights = mutableListOf<Int>()
                val layoutTimeline = mutableListOf<String>()
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                        OquTurboTheme {
                            navigation = rememberNavController()
                            val isHome = navigation.currentBackStackEntryAsState().value?.destination?.route == "home"
                            // Status inset stays on Home. The system navigation inset belongs to the
                            // conditional bottom bar: edge-to-edge content regains it on detail.
                            Scaffold(
                                modifier = Modifier.fillMaxSize().padding(top = 24.dp),
                                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                bottomBar = {
                                    if (isHome) {
                                        NavigationBar(windowInsets = WindowInsets(0, 0, 0, 48)) {
                                            listOf(
                                                AppResource.String.oquturbo_navigation_home,
                                                AppResource.String.oquturbo_navigation_games,
                                                AppResource.String.oquturbo_navigation_stats,
                                                AppResource.String.oquturbo_navigation_profile,
                                            ).forEachIndexed { index, label ->
                                                NavigationBarItem(
                                                    selected = index == 0,
                                                    onClick = {},
                                                    icon = { Box(Modifier.size(24.dp)) },
                                                    label = { Text(stringResource(label)) },
                                                )
                                            }
                                        }
                                    }
                                },
                            ) { padding ->
                                NavHost(
                                    navController = navigation,
                                    startDestination = "home",
                                    modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
                                    enterTransition = { EnterTransition.None },
                                    exitTransition = { ExitTransition.None },
                                    popEnterTransition = { EnterTransition.None },
                                    popExitTransition = { ExitTransition.None },
                                ) {
                                    composable("home") {
                                        list = rememberLazyListState()
                                        scope = rememberCoroutineScope()
                                        HomeScreen(
                                            uiState = state.value,
                                            onStartTrainingClick = {},
                                            onModeStatisticsClick = { navigation.navigate("detail") },
                                            listState = list,
                                            modifier =
                                                Modifier.onSizeChanged { viewportHeights.add(it.height) }
                                                    .onGloballyPositioned {
                                                        val info = list.layoutInfo
                                                        val itemSizes =
                                                            info.visibleItemsInfo.map { item ->
                                                                "${item.index}:${item.offset}:${item.size}"
                                                            }
                                                        val resultState = state.value.personalResult::class.simpleName
                                                        layoutTimeline.add(
                                                            "route=${navigation.currentDestination?.route} " +
                                                                "state=$resultState " +
                                                                "position=${list.firstVisibleItemIndex}:" +
                                                                "${list.firstVisibleItemScrollOffset} " +
                                                                "viewport=${info.viewportStartOffset}.." +
                                                                "${info.viewportEndOffset} " +
                                                                "items=$itemSizes",
                                                        )
                                                    },
                                        )
                                    }
                                    composable("detail") { Text("Detail") }
                                }
                            }
                        }
                    }
                }
                runOnIdle { scope.launch { list.scrollToItem(3) } }
                val action = "Режим статистикасы"
                onNodeWithText(action).performScrollTo().assertIsDisplayed()
                // Rhythm now sits below the result. End cases use a taller natural viewport
                // so both the result action and the actual last item remain reachable together.
                // The weekly-review entry adds height to Rhythm; 1200 keeps the Mode CTA fully visible.
                runOnIdle {
                    scope.launch {
                        if (scrollToEnd) {
                            list.scrollToItem(list.layoutInfo.totalItemsCount - 1)
                        } else {
                            list.scrollBy(additionalScroll)
                        }
                    }
                }
                onNodeWithText(action).assertIsDisplayed()
                val before = runOnIdle { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
                val buttonTop = onNodeWithText(action).fetchSemanticsNode().boundsInRoot.top
                println("KK CTA bounds=${onNodeWithText(action).fetchSemanticsNode().boundsInRoot}")
                onNodeWithText(action).performClick()
                onNodeWithText("Detail").assertIsDisplayed()
                runOnIdle {
                    if (freshRead) state.value = ready.copy(personalResult = PersonalResultState.Loading)
                    navigation.popBackStack()
                }
                if (freshRead) {
                    onNodeWithText(action).assertDoesNotExist()
                    onNodeWithText("+2").assertDoesNotExist()
                }
                val loading = runOnIdle { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
                runOnIdle { state.value = ready }
                onNodeWithText(action).assertIsDisplayed()
                val after = runOnIdle { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
                val afterTop = onNodeWithText(action).fetchSemanticsNode().boundsInRoot.top
                println(
                    "KK scaffold: before=$before loading=$loading after=$after CTA=$buttonTop->$afterTop viewports=$viewportHeights",
                )
                println(
                    layoutTimeline.joinToString("\n"),
                )
                assertEquals(before, loading)
                assertEquals(before, after)
                assertEquals(buttonTop, afterTop, 1f)
                // A later user scroll/recomposition must not reapply the already-consumed anchor.
                runOnIdle { scope.launch { list.scrollBy(-40f) } }
                val userPosition = runOnIdle { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
                runOnIdle { state.value = ready.copy(personalResult = PersonalResultState.Loading) }
                onNodeWithText(action).assertDoesNotExist()
                runOnIdle { state.value = ready }
                onNodeWithText(action).assertIsDisplayed()
                assertEquals(
                    userPosition,
                    runOnIdle { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset },
                )
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    private fun verifyLoadingPosition(comparison: ProgressComparison, recreateHome: Boolean) {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            runDesktopComposeUiTest(width = 320, height = 640) {
                val visible = mutableStateOf(true)
                val ready =
                    HomeUiState(
                        dailyTraining = previewDailyTraining(false),
                        personalResult = PersonalResultState.Loaded(comparison),
                    )
                val state = mutableStateOf(ready)
                lateinit var list: LazyListState
                lateinit var scope: CoroutineScope
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                        OquTurboTheme {
                            val savedState = rememberSaveableStateHolder()
                            if (visible.value) {
                                savedState.SaveableStateProvider("home") {
                                    list = rememberLazyListState()
                                    scope = rememberCoroutineScope()
                                    HomeScreen(
                                        uiState = state.value,
                                        onStartTrainingClick = {},
                                        onModeStatisticsClick = { if (recreateHome) visible.value = false },
                                        listState = list,
                                    )
                                }
                            }
                        }
                    }
                }
                runOnIdle { scope.launch { list.scrollToItem(3) } }
                onNodeWithText("Mode statistics").performScrollTo().assertIsDisplayed()
                val position = runOnIdle { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
                val buttonTop = onNodeWithText("Mode statistics").fetchSemanticsNode().boundsInRoot.top
                if (recreateHome) {
                    onNodeWithText("Mode statistics").performClick()
                    waitForIdle()
                }
                runOnIdle {
                    state.value = ready.copy(personalResult = PersonalResultState.Loading)
                    visible.value = true
                }
                onNodeWithText("Loading result…").assertExists()
                onNodeWithText("Mode statistics").assertDoesNotExist()
                onNodeWithText("+2").assertDoesNotExist()
                onNodeWithText("Saved attempts: 9 of 10").assertDoesNotExist()
                assertEquals(position, runOnIdle { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset })
                runOnIdle { state.value = ready }
                onNodeWithText("Mode statistics").assertIsDisplayed()
                assertEquals(position, runOnIdle { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset })
                assertEquals(buttonTop, onNodeWithText("Mode statistics").fetchSemanticsNode().boundsInRoot.top, 1f)
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    private companion object {
        val series = GameSeriesKey(GameId.NumberSprint, GameModeId.NumberSprintBinary, null)
    }
}
