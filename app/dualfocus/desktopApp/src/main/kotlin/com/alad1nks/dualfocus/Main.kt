package com.alad1nks.dualfocus

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alad1nks.dualfocus.shared.App

fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "Dual Focus") {
            App()
        }
    }
