package com.alad1nks.oquturbo.feature.main.di

import com.alad1nks.oquturbo.feature.main.ui.MainViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val MainModule =
    module {
        viewModel { MainViewModel(get()) }
    }
