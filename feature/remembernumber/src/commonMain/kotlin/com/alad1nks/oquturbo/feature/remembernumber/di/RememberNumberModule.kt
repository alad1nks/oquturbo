package com.alad1nks.oquturbo.feature.remembernumber.di

import com.alad1nks.oquturbo.feature.remembernumber.data.repository.RememberNumberRepository
import com.alad1nks.oquturbo.feature.remembernumber.ui.RememberNumberViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val RememberNumberModule =
    module {
        viewModel { (maxLength: Int) ->
            RememberNumberViewModel(
                maxLength = maxLength,
                rememberNumberRepository = get(),
            )
        }
        single {
            RememberNumberRepository(get())
        }
    }
