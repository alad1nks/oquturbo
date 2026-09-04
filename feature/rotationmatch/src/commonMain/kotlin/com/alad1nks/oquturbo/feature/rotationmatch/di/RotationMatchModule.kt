package com.alad1nks.oquturbo.feature.rotationmatch.di

import com.alad1nks.oquturbo.feature.rotationmatch.ui.RotationMatchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val RotationMatchModule = module { viewModel { RotationMatchViewModel(activityRepository = get()) } }
