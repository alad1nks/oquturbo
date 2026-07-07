package com.alad1nks.sansprint

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alad1nks.sansprint.shared.App

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "SanSprint",
        ) {
            App()
        }
    }
