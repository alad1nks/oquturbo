package com.alad1nks.oquturbo.feature.main.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

actual object LocalAppLocale {
    private var default: Locale? = null
    private val localAppLocale = staticCompositionLocalOf { Locale.getDefault().toString() }

    actual val current: String
        @Composable get() = localAppLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (default == null) {
            default = Locale.getDefault()
        }

        val newLocale = value?.let(Locale::forLanguageTag) ?: requireNotNull(default)
        Locale.setDefault(newLocale)
        return localAppLocale.provides(newLocale.toString())
    }
}
