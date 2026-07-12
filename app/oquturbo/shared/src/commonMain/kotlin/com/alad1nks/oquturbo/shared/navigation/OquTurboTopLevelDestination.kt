package com.alad1nks.oquturbo.shared.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.alad1nks.oquturbo.feature.home.navigation.HomeRoute
import com.alad1nks.oquturbo.feature.remembernumbermenu.navigation.RememberNumberMenuRoute
import com.alad1nks.oquturbo.resources.AppResource
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import org.jetbrains.compose.resources.StringResource

internal enum class OquTurboTopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelStringResource: StringResource,
    val route: Any,
) {
    HOME(
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        labelStringResource = AppResource.String.oquturbo_navigation_home,
        route = HomeRoute,
    ),
    GAMES(
        selectedIcon = Icons.Filled.Games,
        unselectedIcon = Icons.Outlined.Games,
        labelStringResource = AppResource.String.oquturbo_navigation_games,
        route = RememberNumberMenuRoute,
    ),
    STATS(
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        labelStringResource = AppResource.String.oquturbo_navigation_stats,
        route = StatsRoute,
    ),
    PROFILE(
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        labelStringResource = AppResource.String.oquturbo_navigation_profile,
        route = ProfileRoute,
    ),
}

@OptIn(ExperimentalSerializationApi::class)
internal fun OquTurboTopLevelDestination.routeSerialName(): String =
    when (route) {
        HomeRoute -> serializer<HomeRoute>().descriptor.serialName
        RememberNumberMenuRoute -> serializer<RememberNumberMenuRoute>().descriptor.serialName
        StatsRoute -> serializer<StatsRoute>().descriptor.serialName
        ProfileRoute -> serializer<ProfileRoute>().descriptor.serialName
        else -> error("Unsupported top-level route: $route")
    }
