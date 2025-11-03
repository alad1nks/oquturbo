package com.alad1nks.oquturbo.core.storage.datastore.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.alad1nks.oquturbo.core.storage.datastore.STORAGE_DATA_STORE_FILE_NAME
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val StorageDataStoreModuleAndroid =
    module {
        single {
            PreferenceDataStoreFactory.createWithPath(
                produceFile = { androidContext().filesDir.resolve(STORAGE_DATA_STORE_FILE_NAME).absolutePath.toPath() },
            )
        }
    }
