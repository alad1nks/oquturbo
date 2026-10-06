package com.alad1nks.oquturbo.shared

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavDestination.Companion.hasRoute
import com.alad1nks.oquturbo.core.data.repository.SettingsRepository
import com.alad1nks.oquturbo.core.storage.common.AppPreferences
import com.alad1nks.oquturbo.feature.home.navigation.HomeRoute
import com.alad1nks.oquturbo.feature.profile.navigation.ProfileSettingsRoute
import com.alad1nks.oquturbo.feature.stats.navigation.WeeklyReviewRoute
import com.alad1nks.oquturbo.shared.navigation.OquTurboTopLevelDestination
import com.alad1nks.oquturbo.shared.reminders.ReminderHomeActions
import com.alad1nks.oquturbo.shared.reminders.ReminderHomeNavigation
import com.alad1nks.oquturbo.shared.ui.OquTurboAppState
import com.alad1nks.oquturbo.shared.ui.rememberOquTurboAppState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ReminderRuntimeAssemblyTest {
    @Test fun suppliedRuntimeSurvivesCompositionDisposalAndQueuedHomeActionsUseExistingGraphOnce() {
        stopKoin()
        val prefs = Preferences()
        val application = koinApplication { modules(getCommonModules() + module { single<AppPreferences> { prefs } }) }
        val owner =
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        // Receiver-first access resolves the same graph before any Compose scene exists.
        val receiverSettings = application.koin.get<SettingsRepository>()
        val actions = ReminderHomeActions().apply { offer() }
        try {
            runDesktopComposeUiTest(width = 390, height = 800) {
                val shown = mutableStateOf(true)
                lateinit var state: OquTurboAppState
                var consumed = 0
                setContent {
                    if (shown.value) {
                        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                            state = rememberOquTurboAppState()
                            ReminderHomeNavigation(actions, state) { consumed++ }
                            App(state, emptyList(), emptyList(), application)
                        }
                    }
                }
                runOnIdle {
                    assertEquals(1, consumed)
                    assertTrue(state.navController.currentBackStackEntry!!.destination.hasRoute<HomeRoute>())
                    assertSame(prefs, application.koin.get<AppPreferences>())
                    assertSame(receiverSettings, application.koin.get<SettingsRepository>())
                    // Fresh graph: no prior top-level transition has cached a Home mapping.
                    state.navController.navigate(WeeklyReviewRoute)
                }
                runOnIdle { actions.offer() }
                runOnIdle {
                    assertEquals(2, consumed)
                    assertTrue(state.navController.currentBackStackEntry!!.destination.hasRoute<HomeRoute>())
                    state.navigateToTopLevelDestination(OquTurboTopLevelDestination.PROFILE)
                }
                runOnIdle { actions.offer() }
                runOnIdle {
                    assertEquals(3, consumed)
                    assertTrue(state.navController.currentBackStackEntry!!.destination.hasRoute<HomeRoute>())
                    state.navigateToTopLevelDestination(OquTurboTopLevelDestination.PROFILE)
                    state.navController.navigate(ProfileSettingsRoute)
                }
                runOnIdle { actions.offer() }
                runOnIdle {
                    assertEquals(4, consumed)
                    assertTrue(state.navController.currentBackStackEntry!!.destination.hasRoute<HomeRoute>())
                    assertEquals(
                        1,
                        state.navController.currentBackStack.value.count { it.destination.hasRoute<HomeRoute>() },
                    )
                    shown.value = false
                }
                runOnIdle {
                    assertSame(receiverSettings, application.koin.get<SettingsRepository>())
                    shown.value = true
                }
                runOnIdle {
                    assertEquals(4, consumed)
                    assertSame(prefs, application.koin.get<AppPreferences>())
                }
            }
        } finally {
            owner.viewModelStore.clear()
            application.close()
            stopKoin()
        }
    }

    private class Preferences : AppPreferences {
        val strings = mutableMapOf<String, MutableStateFlow<String?>>()

        override fun getString(key: String): Flow<String?> = strings.getOrPut(key) { MutableStateFlow(null) }

        override fun getBoolean(key: String): Flow<Boolean?> = flowOf(null)

        override fun getInt(key: String): Flow<Int?> = flowOf(null)

        override suspend fun setString(key: String, value: String) {
            strings.getOrPut(key) { MutableStateFlow(null) }.value = value
        }

        override suspend fun setBoolean(key: String, value: Boolean) = error("No reminder opt-in")

        override suspend fun setInt(key: String, value: Int) = error("No game played")
    }
}
