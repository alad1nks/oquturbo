package com.alad1nks.oquturbo.core.storage.datastore.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.alad1nks.oquturbo.core.storage.common.AppPreferences
import com.alad1nks.oquturbo.core.storage.datastore.AppPreferencesImpl
import com.alad1nks.oquturbo.core.storage.datastore.PreferenceDataStoreProduceFile
import okio.Path.Companion.toPath
import org.koin.core.scope.Scope
import org.koin.dsl.module

fun storageDataStoreModule(produceFilePath: Scope.() -> String) =
    module {
        single {
            val scope = this
            PreferenceDataStoreFactory.createWithPath { scope.produceFilePath().toPath() }
        }
        single<AppPreferences> { AppPreferencesImpl(get()) }
    }

val StorageDataStoreModule = storageDataStoreModule { PreferenceDataStoreProduceFile.toString() }
