package com.alad1nks.oquturbo.shared.reminders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavDestination.Companion.hasRoute
import com.alad1nks.oquturbo.feature.home.navigation.HomeRoute
import com.alad1nks.oquturbo.shared.ui.OquTurboAppState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

internal class ReminderHomeActions {
    val pending = MutableStateFlow(0L)
    var consumed = 0L

    fun offer() {
        pending.value++
    }
}

@Composable
internal fun ReminderHomeNavigation(
    actions: ReminderHomeActions,
    state: OquTurboAppState,
    onConsumed: (Long) -> Unit = {},
) {
    LaunchedEffect(actions, state) {
        actions.pending.collectLatest { event ->
            if (event <= actions.consumed)return@collectLatest
            if (state.navController.currentBackStackEntry == null)state.navController.currentBackStackEntryFlow.first()
            if (state.navController.currentBackStackEntry?.destination?.hasRoute<HomeRoute>() != true) {
                // A notification opens Home itself, without restoring a saved child of that tab.
                if (!state.navController.popBackStack(HomeRoute, inclusive = false)) {
                    state.navController.navigate(HomeRoute) {
                        popUpTo(state.navController.graph.id)
                        launchSingleTop = true
                        restoreState = false
                    }
                }
                state.navController.currentBackStackEntryFlow.first { it.destination.hasRoute<HomeRoute>() }
            }
            actions.consumed = event
            onConsumed(event)
        }
    }
}
