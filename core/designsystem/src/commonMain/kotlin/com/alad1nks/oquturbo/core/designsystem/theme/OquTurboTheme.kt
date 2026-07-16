package com.alad1nks.oquturbo.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF6250C7),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE8E2FF),
        onPrimaryContainer = Color(0xFF21105B),
        secondary = Color(0xFF655A74),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFEBDDF5),
        onSecondaryContainer = Color(0xFF21182D),
        tertiary = Color(0xFF9A476A),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD8E6),
        onTertiaryContainer = Color(0xFF3E0824),
        error = Color(0xFFBA1A1A),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFCF8FF),
        onBackground = Color(0xFF1D1B20),
        surface = Color(0xFFFCF8FF),
        onSurface = Color(0xFF1D1B20),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),
        outline = Color(0xFF7A757F),
        outlineVariant = Color(0xFFCBC4CF),
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFCCBEFF),
        onPrimary = Color(0xFF332279),
        primaryContainer = Color(0xFF493795),
        onPrimaryContainer = Color(0xFFE8E2FF),
        secondary = Color(0xFFCEC2DA),
        onSecondary = Color(0xFF352D40),
        secondaryContainer = Color(0xFF4C4358),
        onSecondaryContainer = Color(0xFFEBDDF5),
        tertiary = Color(0xFFFFB0CC),
        onTertiary = Color(0xFF5D113A),
        tertiaryContainer = Color(0xFF7A2D51),
        onTertiaryContainer = Color(0xFFFFD8E6),
        error = Color(0xFFFFB4AB),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF141217),
        onBackground = Color(0xFFE7E0E8),
        surface = Color(0xFF141217),
        onSurface = Color(0xFFE7E0E8),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCBC4CF),
        outline = Color(0xFF958F99),
        outlineVariant = Color(0xFF49454F),
    )

private val OquTurboTypography =
    Typography(
        displaySmall =
            Typography().displaySmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            ),
        headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = Typography().headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )

private val OquTurboShapes =
    Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

@Composable
fun OquTurboTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = OquTurboTypography,
        shapes = OquTurboShapes,
        content = content,
    )
}
