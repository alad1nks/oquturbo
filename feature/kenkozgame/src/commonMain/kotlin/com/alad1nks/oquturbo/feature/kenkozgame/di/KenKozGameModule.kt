package com.alad1nks.oquturbo.feature.kenkozgame.di

import com.alad1nks.oquturbo.feature.kenkozgame.navigation.KenKozGameRoute
import com.alad1nks.oquturbo.feature.kenkozgame.ui.KenKozGameViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val KenKozGameModule =
    module {
        viewModel { parameters ->
            val route = parameters.get<KenKozGameRoute>(0)
            val characters = parameters.get<List<String>>(1)
            val words = parameters.get<List<String>>(2)
            val differencePairs = parameters.get<List<Pair<String, String>>>(3)

            KenKozGameViewModel(
                mode = route.mode,
                characters = characters,
                words = words,
                differencePairs = differencePairs,
                trainingEntryId = route.trainingEntryId,
                trainingRequiredScore = route.trainingRequiredScore,
                kenKozGameRepository = get(),
                gameActivityRepository = get(),
                dailyTrainingRepository = get(),
            )
        }
    }
