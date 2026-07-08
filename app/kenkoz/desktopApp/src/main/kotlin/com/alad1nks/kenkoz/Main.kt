package com.alad1nks.kenkoz

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alad1nks.kenkoz.shared.App

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "KenKoz",
        ) {
            App()
        }
    }
