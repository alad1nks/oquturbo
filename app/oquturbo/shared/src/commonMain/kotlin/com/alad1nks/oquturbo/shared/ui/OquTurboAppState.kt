package com.alad1nks.oquturbo.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alad1nks.oquturbo.feature.home.navigation.HomeRoute
import com.alad1nks.oquturbo.feature.home.navigation.navigateToHome
import com.alad1nks.oquturbo.feature.remembernumbermenu.navigation.RememberNumberMenuRoute
import com.alad1nks.oquturbo.feature.remembernumbermenu.navigation.navigateToRememberNumberMenu
import com.alad1nks.oquturbo.shared.navigation.OquTurboTopLevelDestination
import com.alad1nks.oquturbo.shared.navigation.ProfileRoute
import com.alad1nks.oquturbo.shared.navigation.StatsRoute
import com.alad1nks.oquturbo.shared.navigation.routeSerialName

@Composable
internal fun rememberOquTurboAppState(
    navController: NavHostController = rememberNavController(),
): OquTurboAppState = remember(navController) { OquTurboAppState(navController) }

internal class OquTurboAppState(
    val navController: NavHostController,
) {
    val currentDestination: NavDestination? @Composable get() =
        navController.currentBackStackEntryAsState().value?.destination

    val topLevelDestinations: List<OquTurboTopLevelDestination> = OquTurboTopLevelDestination.entries

    val shouldShowNavigationBar: Boolean @Composable get() {
        val currentRoute = currentDestination?.route
        return topLevelDestinations.any { it.routeSerialName() == currentRoute }
    }

    fun navigateToTopLevelDestination(destination: OquTurboTopLevelDestination) {
        val navOptions: NavOptionsBuilder.() -> Unit = {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }

        when (destination.route) {
            HomeRoute -> navController.navigateToHome(navOptions)
            RememberNumberMenuRoute -> navController.navigateToRememberNumberMenu(navOptions)
            StatsRoute -> navController.navigate(StatsRoute, navOptions)
            ProfileRoute -> navController.navigate(ProfileRoute, navOptions)
        }
    }
}
