package com.alad1nks.oquturbo.feature.kenkozgame.di

import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import com.alad1nks.oquturbo.feature.kenkozgame.ui.KenKozGameViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val KenKozGameModule =
    module {
        viewModel { parameters ->
            val mode = parameters.get<KenKozGameMode>(0)
            val characters = parameters.get<List<String>>(1)
            val words = parameters.get<List<String>>(2)
            val differencePairs = parameters.get<List<Pair<String, String>>>(3)

            KenKozGameViewModel(mode, characters, words, differencePairs)
        }
    }
