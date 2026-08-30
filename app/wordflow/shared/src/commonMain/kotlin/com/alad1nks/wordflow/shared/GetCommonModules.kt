package com.alad1nks.wordflow.shared

import com.alad1nks.oquturbo.core.data.di.DataModule
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import com.alad1nks.oquturbo.feature.main.di.MainModule
import com.alad1nks.oquturbo.feature.wordflow.di.WordFlowModule
import org.koin.core.module.Module

fun getCommonModules(): List<Module> {
    return listOf(WordFlowModule, DataModule, MainModule, StorageCommonModule)
}
