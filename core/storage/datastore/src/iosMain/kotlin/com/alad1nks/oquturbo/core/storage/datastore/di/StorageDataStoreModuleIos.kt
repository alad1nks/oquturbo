package com.alad1nks.oquturbo.core.storage.datastore.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.alad1nks.oquturbo.core.storage.datastore.STORAGE_DATA_STORE_FILE_NAME
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
val StorageDataStoreModuleIos =
    module {
        single {
            PreferenceDataStoreFactory.createWithPath(
                produceFile = {
                    val documentDirectory: NSURL? =
                        NSFileManager.defaultManager.URLForDirectory(
                            directory = NSDocumentDirectory,
                            inDomain = NSUserDomainMask,
                            appropriateForURL = null,
                            create = false,
                            error = null,
                        )
                    (requireNotNull(documentDirectory).path + "/$STORAGE_DATA_STORE_FILE_NAME").toPath()
                },
            )
        }
    }
