package com.alad1nks.symbolcount.shared

import com.alad1nks.oquturbo.core.storage.datastore.di.storageDataStoreModule
import org.koin.core.module.Module
import java.io.File

internal const val SYMBOL_COUNT_DATA_STORE_FILE_NAME = "symbolcount.preferences_pb"

internal fun symbolCountDataStorePath(userHome: String): String =
    File(File(userHome), ".symbolcount/$SYMBOL_COUNT_DATA_STORE_FILE_NAME").absolutePath

internal fun symbolCountStorageDataStoreModule(userHome: String) =
    storageDataStoreModule {
        symbolCountDataStorePath(userHome).also { path ->
            check(File(path).parentFile.let { it.isDirectory || it.mkdirs() }) {
                "Unable to create the Symbol Count preferences directory"
            }
        }
    }

private val SymbolCountStorageDataStoreModule =
    symbolCountStorageDataStoreModule(checkNotNull(System.getProperty("user.home")))

actual fun getPlatformModules(): List<Module> = listOf(SymbolCountStorageDataStoreModule)
