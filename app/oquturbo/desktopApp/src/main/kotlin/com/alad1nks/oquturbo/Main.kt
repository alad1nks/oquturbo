package com.alad1nks.oquturbo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alad1nks.oquturbo.shared.App

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "OquTurbo",
        ) {
            App()
        }
    }
