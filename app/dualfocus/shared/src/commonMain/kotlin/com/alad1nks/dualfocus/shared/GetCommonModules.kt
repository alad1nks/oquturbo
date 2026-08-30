package com.alad1nks.dualfocus.shared

import com.alad1nks.oquturbo.core.data.di.DataModule
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import com.alad1nks.oquturbo.feature.dualfocus.di.DualFocusModule
import com.alad1nks.oquturbo.feature.main.di.MainModule
import org.koin.core.module.Module

fun getCommonModules(): List<Module> {
    return listOf(DualFocusModule, DataModule, MainModule, StorageCommonModule)
}
