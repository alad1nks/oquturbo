package com.alad1nks.oquturbo.feature.remembernumbermenu.di

import com.alad1nks.oquturbo.feature.remembernumbermenu.ui.RememberNumberMenuViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val RememberNumberMenuModule =
    module {
        viewModel { RememberNumberMenuViewModel() }
    }
