package com.alad1nks.oquturbo.feature.remembernumbermenu.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme

@Composable
internal fun RememberNumberMenuThemeIcon(
    darkTheme: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = { onChange(!darkTheme) },
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (darkTheme) Icons.Default.Bedtime else Icons.Default.WbSunny,
            contentDescription = null,
        )
    }
}

@Preview
@Composable
private fun RememberNumberMenuThemeIconLightPreview() {
    OquTurboTheme {
        RememberNumberMenuThemeIcon(
            darkTheme = false,
            onChange = {},
        )
    }
}

@Preview
@Composable
private fun RememberNumberMenuThemeIconDarkPreview() {
    OquTurboTheme(darkTheme = true) {
        RememberNumberMenuThemeIcon(
            darkTheme = true,
            onChange = {},
        )
    }
}
