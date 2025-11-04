package com.alad1nks.oquturbo.core.storage.datastore.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.alad1nks.oquturbo.core.storage.datastore.STORAGE_DATA_STORE_FILE_NAME
import okio.Path.Companion.toPath
import org.koin.dsl.module
import java.io.File

val StorageDataStoreModuleJvm =
    module {
        single {
            PreferenceDataStoreFactory.createWithPath(
                produceFile = {
                    File(System.getProperty("java.io.tmpdir"), STORAGE_DATA_STORE_FILE_NAME).absolutePath.toPath()
                },
            )
        }
    }
