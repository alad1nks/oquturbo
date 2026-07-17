package com.alad1nks.oquturbo.feature.home.di

import com.alad1nks.oquturbo.feature.home.ui.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val HomeModule =
    module {
        viewModelOf(::HomeViewModel)
    }
