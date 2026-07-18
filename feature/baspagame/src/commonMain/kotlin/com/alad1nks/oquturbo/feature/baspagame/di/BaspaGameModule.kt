package com.alad1nks.oquturbo.feature.baspagame.di

import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameContent
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameMode
import com.alad1nks.oquturbo.feature.baspagame.ui.BaspaGameViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val BaspaGameModule =
    module {
        viewModel { parameters ->
            val mode = parameters.get<BaspaGameMode>(0)
            val content = parameters.get<BaspaGameContent>(1)
            val trainingEntryId = parameters.get<String?>(2)
            val trainingRequiredScore = parameters.get<Int?>(3)
            BaspaGameViewModel(
                mode = mode,
                content = content,
                trainingEntryId = trainingEntryId,
                trainingRequiredScore = trainingRequiredScore,
                repository = get(),
                gameActivityRepository = get(),
                dailyTrainingRepository = get(),
            )
        }
    }
