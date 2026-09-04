package com.alad1nks.rotationmatch

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alad1nks.rotationmatch.shared.App

fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "Rotation Match") {
            App()
        }
    }
