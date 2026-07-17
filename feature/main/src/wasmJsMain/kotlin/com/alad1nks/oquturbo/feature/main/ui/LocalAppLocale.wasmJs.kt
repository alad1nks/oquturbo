package com.alad1nks.oquturbo.feature.main.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale

@Suppress("ktlint:standard:class-naming")
private external object window {
    var __customLocale: String?
}

actual object LocalAppLocale {
    private val localAppLocale = staticCompositionLocalOf { Locale.current }

    actual val current: String
        @Composable get() = localAppLocale.current.toString()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        window.__customLocale = value?.replace('_', '-')
        return localAppLocale.provides(Locale.current)
    }
}
