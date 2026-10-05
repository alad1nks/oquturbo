@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.alad1nks.oquturbo.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.alad1nks.oquturbo.shared.reminders.IosReminderBridge
import com.alad1nks.oquturbo.shared.reminders.IosReminderDiagnostics
import com.alad1nks.oquturbo.shared.reminders.IosReminderRuntime
import com.alad1nks.oquturbo.shared.reminders.ReminderHomeNavigation
import com.alad1nks.oquturbo.shared.ui.rememberOquTurboAppState

fun mainViewController() =
    ComposeUIViewController {
        val runtime = IosReminderRuntime
        val state = rememberOquTurboAppState()
        ReminderHomeNavigation(runtime.homeActions, state) { event ->
            IosReminderDiagnostics.record("home-consumed", "eventId" to event.toString(), "destination" to "Home")
        }
        App(state, emptyList(), emptyList(), runtime.application)
    }.also {
        IosReminderBridge.bootstrap()
        IosReminderRuntime.platform.bind(it)
    }
