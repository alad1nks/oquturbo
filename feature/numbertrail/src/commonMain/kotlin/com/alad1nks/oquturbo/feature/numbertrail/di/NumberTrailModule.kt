package com.alad1nks.oquturbo.feature.numbertrail.di

import com.alad1nks.oquturbo.feature.numbertrail.ui.NumberTrailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val NumberTrailModule = module { viewModel { NumberTrailViewModel(activityRepository = get()) } }
