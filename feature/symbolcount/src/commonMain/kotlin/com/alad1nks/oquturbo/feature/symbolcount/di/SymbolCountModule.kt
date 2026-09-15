package com.alad1nks.oquturbo.feature.symbolcount.di

import com.alad1nks.oquturbo.feature.symbolcount.ui.SymbolCountViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val SymbolCountModule = module { viewModel { SymbolCountViewModel(activityRepository = get()) } }
