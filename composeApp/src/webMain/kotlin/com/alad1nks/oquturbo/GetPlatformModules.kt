package com.alad1nks.oquturbo

import com.alad1nks.oquturbo.core.storage.web.di.StorageWebModule
import org.koin.core.module.Module

actual fun getPlatformModules(): List<Module> {
    return listOf(StorageWebModule)
}
