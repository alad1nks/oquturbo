package com.alad1nks.oquturbo.screenshottests

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Preview(
    name = "Stats subtitle — Russian",
    locale = "ru",
    widthDp = 480,
    heightDp = 120,
)
@Composable
private fun RussianResourcePreview() {
    StatsSubtitlePreview()
}

@Preview(
    name = "Stats subtitle — Kazakh",
    locale = "kk",
    widthDp = 480,
    heightDp = 120,
)
@Composable
private fun KazakhResourcePreview() {
    StatsSubtitlePreview()
}

@Composable
private fun StatsSubtitlePreview() {
    OquTurboTheme {
        Surface {
            Text(
                text = stringResource(AppResource.String.oquturbo_stats_subtitle),
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}
