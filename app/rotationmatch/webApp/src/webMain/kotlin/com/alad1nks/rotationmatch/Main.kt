package com.alad1nks.rotationmatch

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.alad1nks.rotationmatch.shared.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport { App() }
}
