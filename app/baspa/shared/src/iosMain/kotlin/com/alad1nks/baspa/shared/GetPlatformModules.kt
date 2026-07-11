package com.alad1nks.baspa.shared

import com.alad1nks.oquturbo.core.storage.datastore.di.StorageDataStoreModule
import org.koin.core.module.Module

actual fun getPlatformModules(): List<Module> = listOf(StorageDataStoreModule)
