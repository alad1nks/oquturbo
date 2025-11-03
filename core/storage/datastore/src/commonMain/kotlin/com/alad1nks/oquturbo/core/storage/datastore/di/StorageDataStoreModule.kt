package com.alad1nks.oquturbo.core.storage.datastore.di

import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.core.storage.datastore.StorageImpl
import org.koin.dsl.module

val StorageDataStoreModule =
    module {
        single<Storage> { StorageImpl(get()) }
    }
