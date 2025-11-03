package com.alad1nks.oquturbo.core.storage.web.di

import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.core.storage.web.StorageImpl
import org.koin.dsl.module

val StorageWebModule =
    module {
        single<Storage> { StorageImpl() }
    }
