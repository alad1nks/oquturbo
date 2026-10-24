package com.alad1nks.oquturbo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alad1nks.oquturbo.resources.AppResource
import com.alad1nks.oquturbo.shared.App
import org.jetbrains.compose.resources.stringResource

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(AppResource.String.daily_mind_app_name),
        ) {
            App()
        }
    }
