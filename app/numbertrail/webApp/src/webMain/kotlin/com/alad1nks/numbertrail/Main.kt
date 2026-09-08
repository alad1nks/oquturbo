package com.alad1nks.numbertrail

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.alad1nks.numbertrail.shared.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport { App() }
}
