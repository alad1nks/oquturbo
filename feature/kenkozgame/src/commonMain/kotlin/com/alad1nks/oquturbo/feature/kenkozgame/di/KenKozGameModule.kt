package com.alad1nks.oquturbo.feature.kenkozgame.di

import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import com.alad1nks.oquturbo.feature.kenkozgame.ui.KenKozGameViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val KenKozGameModule =
    module {
        viewModel { (mode: KenKozGameMode) ->
            KenKozGameViewModel(mode)
        }
    }
