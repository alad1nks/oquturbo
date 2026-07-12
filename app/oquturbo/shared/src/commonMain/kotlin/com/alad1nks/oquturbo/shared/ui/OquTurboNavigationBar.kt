package com.alad1nks.oquturbo.shared.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import com.alad1nks.oquturbo.shared.navigation.OquTurboTopLevelDestination
import com.alad1nks.oquturbo.shared.navigation.routeSerialName
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OquTurboNavigationBar(
    destinations: List<OquTurboTopLevelDestination>,
    currentDestination: NavDestination?,
    onNavigateToDestination: (OquTurboTopLevelDestination) -> Unit,
) {
    NavigationBar {
        destinations.forEach { destination ->
            val selected = currentDestination?.route == destination.routeSerialName()
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.labelStringResource)) },
            )
        }
    }
}
