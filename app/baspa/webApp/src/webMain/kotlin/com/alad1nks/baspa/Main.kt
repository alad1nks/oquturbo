package com.alad1nks.baspa

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.alad1nks.baspa.shared.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport { App() }
}
