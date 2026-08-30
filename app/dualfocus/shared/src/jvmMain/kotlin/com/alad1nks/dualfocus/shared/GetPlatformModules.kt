package com.alad1nks.dualfocus.shared

import com.alad1nks.oquturbo.core.storage.datastore.di.storageDataStoreModule
import org.koin.core.module.Module
import java.io.File

internal const val DUAL_FOCUS_DATA_STORE_FILE_NAME = "dualfocus.preferences_pb"

internal fun dualFocusDataStorePath(userHome: String): String =
    File(File(userHome), ".dualfocus/$DUAL_FOCUS_DATA_STORE_FILE_NAME").absolutePath

internal fun dualFocusStorageDataStoreModule(userHome: String) =
    storageDataStoreModule {
        dualFocusDataStorePath(userHome).also { path ->
            check(File(path).parentFile.let { it.isDirectory || it.mkdirs() }) {
                "Unable to create the Dual Focus preferences directory"
            }
        }
    }

private val DualFocusStorageDataStoreModule =
    dualFocusStorageDataStoreModule(checkNotNull(System.getProperty("user.home")))

actual fun getPlatformModules(): List<Module> = listOf(DualFocusStorageDataStoreModule)
