package com.alad1nks.rotationmatch.shared

import com.alad1nks.oquturbo.core.storage.datastore.di.storageDataStoreModule
import org.koin.core.module.Module
import java.io.File

internal const val ROTATION_MATCH_DATA_STORE_FILE_NAME = "rotationmatch.preferences_pb"

internal fun rotationMatchDataStorePath(userHome: String): String =
    File(File(userHome), ".rotationmatch/$ROTATION_MATCH_DATA_STORE_FILE_NAME").absolutePath

internal fun rotationMatchStorageDataStoreModule(userHome: String) =
    storageDataStoreModule {
        rotationMatchDataStorePath(userHome).also { path ->
            check(File(path).parentFile.let { it.isDirectory || it.mkdirs() }) {
                "Unable to create the Rotation Match preferences directory"
            }
        }
    }

private val RotationMatchStorageDataStoreModule =
    rotationMatchStorageDataStoreModule(checkNotNull(System.getProperty("user.home")))

actual fun getPlatformModules(): List<Module> = listOf(RotationMatchStorageDataStoreModule)
