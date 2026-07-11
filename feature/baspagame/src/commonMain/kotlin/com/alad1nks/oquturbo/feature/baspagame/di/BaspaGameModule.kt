package com.alad1nks.oquturbo.feature.baspagame.di

import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameContent
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameMode
import com.alad1nks.oquturbo.feature.baspagame.ui.BaspaGameViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val BaspaGameModule =
    module {
        viewModel { (mode: BaspaGameMode, content: BaspaGameContent) ->
            BaspaGameViewModel(mode, content, get())
        }
    }
