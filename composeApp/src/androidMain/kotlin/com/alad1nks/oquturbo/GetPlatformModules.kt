package com.alad1nks.oquturbo

import com.alad1nks.oquturbo.core.storage.datastore.di.StorageDataStoreModule
import com.alad1nks.oquturbo.core.storage.datastore.di.StorageDataStoreModuleAndroid
import org.koin.core.module.Module

actual fun getPlatformModules(): List<Module> {
    return listOf(
        StorageDataStoreModule,
        StorageDataStoreModuleAndroid,
    )
}
