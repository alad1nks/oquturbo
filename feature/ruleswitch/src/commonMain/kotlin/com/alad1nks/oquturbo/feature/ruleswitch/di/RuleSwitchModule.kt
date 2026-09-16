package com.alad1nks.oquturbo.feature.ruleswitch.di

import com.alad1nks.oquturbo.feature.ruleswitch.ui.RuleSwitchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val RuleSwitchModule = module { viewModel { RuleSwitchViewModel(activityRepository = get()) } }
