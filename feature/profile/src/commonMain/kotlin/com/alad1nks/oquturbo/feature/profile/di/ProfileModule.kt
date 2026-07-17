package com.alad1nks.oquturbo.feature.profile.di

import com.alad1nks.oquturbo.feature.profile.ui.ProfileDemoStore
import com.alad1nks.oquturbo.feature.profile.ui.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val ProfileModule =
    module {
        single { ProfileDemoStore() }
        viewModel { ProfileViewModel(get(), get()) }
    }
