package com.alad1nks.oquturbo.feature.memorygrid.di

import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridGameMode
import com.alad1nks.oquturbo.feature.memorygrid.ui.MemoryGridViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val MemoryGridModule =
    module {
        viewModel { parameters -> MemoryGridViewModel(parameters.get<MemoryGridGameMode>(), get()) }
    }
