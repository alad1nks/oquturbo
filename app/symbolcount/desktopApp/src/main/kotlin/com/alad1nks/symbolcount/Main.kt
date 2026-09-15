package com.alad1nks.symbolcount

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alad1nks.symbolcount.shared.App

fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "Symbol Count", icon = painterResource("icon.png")) {
            App()
        }
    }
