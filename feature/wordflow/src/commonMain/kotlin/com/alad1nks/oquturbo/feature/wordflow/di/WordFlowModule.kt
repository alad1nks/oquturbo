package com.alad1nks.oquturbo.feature.wordflow.di

import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowContent
import com.alad1nks.oquturbo.feature.wordflow.ui.WordFlowViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val WordFlowModule =
    module {
        viewModel { parameters ->
            WordFlowViewModel(
                locale = parameters.get<String>(),
                content = parameters.get<WordFlowContent>(),
                activityRepository = get(),
            )
        }
    }
