package com.alad1nks.ruleswitch

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alad1nks.ruleswitch.shared.App

fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "Rule Switch", icon = painterResource("icon.png")) {
            App()
        }
    }
