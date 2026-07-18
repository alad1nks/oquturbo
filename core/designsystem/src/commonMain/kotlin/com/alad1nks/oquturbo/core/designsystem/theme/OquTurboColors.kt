package com.alad1nks.oquturbo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val ColorScheme.success: Color
    get() = if (background.luminance() > 0.5f) Color(0xFF2E7D32) else Color(0xFF81C784)
