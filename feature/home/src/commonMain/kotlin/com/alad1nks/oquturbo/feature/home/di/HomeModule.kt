package com.alad1nks.oquturbo.feature.home.di

import com.alad1nks.oquturbo.feature.home.ui.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val HomeModule =
    module {
        viewModel { HomeViewModel(get(), get()) }
    }
