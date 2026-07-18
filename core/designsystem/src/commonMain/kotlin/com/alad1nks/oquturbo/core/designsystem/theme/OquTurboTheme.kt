package com.alad1nks.oquturbo.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
        primary = Color(0xFFC9B9FF),
        onPrimary = Color(0xFF2F1D70),
        primaryContainer = Color(0xFF44337D),
        onPrimaryContainer = Color(0xFFE8E0FF),
        secondary = Color(0xFFCEC3D6),
        onSecondary = Color(0xFF352E3D),
        secondaryContainer = Color(0xFF403847),
        onSecondaryContainer = Color(0xFFECE2F1),
        tertiary = Color(0xFFE7B8D0),
        onTertiary = Color(0xFF482036),
        tertiaryContainer = Color(0xFF62334A),
        onTertiaryContainer = Color(0xFFFFD8E8),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF111016),
        onBackground = Color(0xFFF0EAF4),
        surface = Color(0xFF19171F),
        onSurface = Color(0xFFF0EAF4),
        surfaceVariant = Color(0xFF4A4550),
        onSurfaceVariant = Color(0xFFCEC5D2),
        outline = Color(0xFF978F9C),
        outlineVariant = Color(0xFF4B4651),
        inverseSurface = Color(0xFFF0EAF4),
        inverseOnSurface = Color(0xFF312E35),
        inversePrimary = Color(0xFF6250C7),
        surfaceDim = Color(0xFF111016),
        surfaceBright = Color(0xFF3A363F),
        surfaceContainerLowest = Color(0xFF0C0B10),
        surfaceContainerLow = Color(0xFF17151C),
        surfaceContainer = Color(0xFF1D1B23),
        surfaceContainerHigh = Color(0xFF27232D),
        surfaceContainerHighest = Color(0xFF322E39),
        surfaceTint = Color.Transparent,
        scrim = Color(0xFF000000),
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
    if (darkTheme) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = OquTurboTypography,
            shapes = OquTurboShapes,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides DarkColorScheme.onBackground,
                content = content,
            )
        }
    } else {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = OquTurboTypography,
            shapes = OquTurboShapes,
            content = content,
        )
    }
}
