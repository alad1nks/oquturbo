package com.alad1nks.oquturbo.feature.remembernumber.di

import com.alad1nks.oquturbo.feature.remembernumber.ui.RememberNumberViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val RememberNumberModule =
    module {
        viewModel { parameters ->
            val maxLength: Int = parameters[0]
            val availableDigits: String = parameters[1]
            val trainingEntryId: String? = parameters[2]
            val trainingRequiredScore: Int? = parameters[3]
            RememberNumberViewModel(
                maxLength = maxLength,
                availableDigits = availableDigits,
                trainingEntryId = trainingEntryId,
                trainingRequiredScore = trainingRequiredScore,
                rememberNumberRepository = get(),
                gameActivityRepository = get(),
                dailyTrainingRepository = get(),
            )
        }
    }
