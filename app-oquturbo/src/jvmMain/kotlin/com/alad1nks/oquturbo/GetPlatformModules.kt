package com.alad1nks.oquturbo

import com.alad1nks.oquturbo.core.storage.datastore.di.StorageDataStoreModule
import com.alad1nks.oquturbo.core.storage.datastore.di.StorageDataStoreModuleJvm
import org.koin.core.module.Module

actual fun getPlatformModules(): List<Module> {
    return listOf(
        StorageDataStoreModuleJvm,
        StorageDataStoreModule,
    )
}
