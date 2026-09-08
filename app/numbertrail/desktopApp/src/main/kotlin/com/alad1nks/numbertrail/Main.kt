package com.alad1nks.numbertrail

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alad1nks.numbertrail.shared.App

fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "Number Trail", icon = painterResource("icon.png")) {
            App()
        }
    }
