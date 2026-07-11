package com.alad1nks.baspa

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alad1nks.baspa.shared.App

fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "Baspa") {
            App()
        }
    }
