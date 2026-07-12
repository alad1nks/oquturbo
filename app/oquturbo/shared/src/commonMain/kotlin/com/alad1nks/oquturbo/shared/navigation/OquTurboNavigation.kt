package com.alad1nks.oquturbo.shared.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.resources.AppResource
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Serializable data object HomeRoute

@Serializable data object StatsRoute

@Serializable data object ProfileRoute

internal fun NavGraphBuilder.homeScreen() =
    composable<HomeRoute> {
        OquTurboTopLevelScreen(AppResource.String.oquturbo_navigation_home)
    }

internal fun NavGraphBuilder.statsScreen() =
    composable<StatsRoute> {
        OquTurboTopLevelScreen(AppResource.String.oquturbo_navigation_stats)
    }

internal fun NavGraphBuilder.profileScreen() =
    composable<ProfileRoute> {
        OquTurboTopLevelScreen(AppResource.String.oquturbo_navigation_profile)
    }

@Composable
private fun OquTurboTopLevelScreen(labelStringResource: StringResource) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(labelStringResource),
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
    }
}
