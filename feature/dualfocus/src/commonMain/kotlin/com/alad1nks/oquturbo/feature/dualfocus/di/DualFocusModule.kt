package com.alad1nks.oquturbo.feature.dualfocus.di

import com.alad1nks.oquturbo.feature.dualfocus.ui.DualFocusViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val DualFocusModule = module { viewModel { DualFocusViewModel(activityRepository = get()) } }
