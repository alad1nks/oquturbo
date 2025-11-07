package com.alad1nks.sansprint

import com.alad1nks.oquturbo.core.storage.datastore.di.StorageDataStoreModule
import com.alad1nks.oquturbo.core.storage.datastore.di.StorageDataStoreModuleIos
import org.koin.core.module.Module

actual fun getPlatformModules(): List<Module> {
    return listOf(
        StorageDataStoreModuleIos,
        StorageDataStoreModule,
    )
}
