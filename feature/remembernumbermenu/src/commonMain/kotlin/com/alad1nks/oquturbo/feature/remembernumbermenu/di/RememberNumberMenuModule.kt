package com.alad1nks.oquturbo.feature.remembernumbermenu.di

import com.alad1nks.oquturbo.feature.remembernumbermenu.ui.RememberNumberMenuViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun rememberNumberMenuModule(showThemeIcon: Boolean) =
    module {
        viewModel {
            RememberNumberMenuViewModel(showThemeIcon, get())
        }
    }
