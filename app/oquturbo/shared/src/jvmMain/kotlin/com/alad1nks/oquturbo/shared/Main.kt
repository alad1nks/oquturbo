package com.alad1nks.oquturbo.shared

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "OquTurbo",
        ) {
            App()
        }
    }
