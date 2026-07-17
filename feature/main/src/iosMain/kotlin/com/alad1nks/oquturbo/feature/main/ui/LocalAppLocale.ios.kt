package com.alad1nks.oquturbo.feature.main.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.InternalComposeUiApi
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

@OptIn(InternalComposeUiApi::class)
actual object LocalAppLocale {
    private const val LANGUAGE_KEY = "AppleLanguages"
    private val default = NSLocale.preferredLanguages.first() as String
    private val localAppLocale = staticCompositionLocalOf { default }

    actual val current: String
        @Composable get() = localAppLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val newLocale = value ?: default
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(LANGUAGE_KEY)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(listOf(newLocale), LANGUAGE_KEY)
        }
        return localAppLocale.provides(newLocale)
    }
}
