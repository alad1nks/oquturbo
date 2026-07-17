package com.alad1nks.oquturbo.feature.remembernumber.di

import com.alad1nks.oquturbo.feature.remembernumber.ui.RememberNumberViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val RememberNumberModule =
    module {
        viewModel { (maxLength: Int, availableDigits: String) ->
            RememberNumberViewModel(
                maxLength = maxLength,
                availableDigits = availableDigits,
                rememberNumberRepository = get(),
                gameActivityRepository = get(),
            )
        }
    }
