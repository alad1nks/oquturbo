package com.alad1nks.numbertrail.shared

import com.alad1nks.oquturbo.core.storage.datastore.di.storageDataStoreModule
import org.koin.core.module.Module
import java.io.File

internal const val NUMBER_TRAIL_DATA_STORE_FILE_NAME = "numbertrail.preferences_pb"

internal fun numberTrailDataStorePath(userHome: String): String =
    File(File(userHome), ".numbertrail/$NUMBER_TRAIL_DATA_STORE_FILE_NAME").absolutePath

internal fun numberTrailStorageDataStoreModule(userHome: String) =
    storageDataStoreModule {
        numberTrailDataStorePath(userHome).also { path ->
            check(File(path).parentFile.let { it.isDirectory || it.mkdirs() }) {
                "Unable to create the Number Trail preferences directory"
            }
        }
    }

private val NumberTrailStorageDataStoreModule =
    numberTrailStorageDataStoreModule(checkNotNull(System.getProperty("user.home")))

actual fun getPlatformModules(): List<Module> = listOf(NumberTrailStorageDataStoreModule)
