package com.alad1nks.oquturbo.shared.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.core.ui.component.PageHeader
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.resources.AppResource
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Serializable data object StatsRoute

@Serializable data object ProfileRoute

internal fun NavGraphBuilder.statsScreen() =
    composable<StatsRoute> {
        OquTurboTopLevelScreen(
            titleStringResource = AppResource.String.oquturbo_navigation_stats,
            subtitleStringResource = AppResource.String.oquturbo_stats_subtitle,
            imageVector = Icons.Filled.BarChart,
        )
    }

internal fun NavGraphBuilder.profileScreen() =
    composable<ProfileRoute> {
        OquTurboTopLevelScreen(
            titleStringResource = AppResource.String.oquturbo_navigation_profile,
            subtitleStringResource = AppResource.String.oquturbo_profile_subtitle,
            imageVector = Icons.Filled.Person,
        )
    }

@Composable
private fun OquTurboTopLevelScreen(
    titleStringResource: StringResource,
    subtitleStringResource: StringResource,
    imageVector: ImageVector,
) {
    Box(modifier = Modifier.fillMaxSize().appBackground()) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 32.dp)),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PageHeader(title = stringResource(titleStringResource))
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                shadowElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(
                            modifier = Modifier.size(88.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = imageVector,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        text = stringResource(subtitleStringResource),
                        modifier = Modifier.padding(top = 20.dp).widthIn(max = 420.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
