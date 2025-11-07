package com.alad1nks.sansprint

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "SanSprint",
        ) {
            KoinApp()
        }
    }
