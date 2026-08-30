package com.alad1nks.wordflow.shared

import com.alad1nks.oquturbo.core.storage.datastore.di.storageDataStoreModule
import org.koin.core.module.Module
import java.io.File

internal const val WORD_FLOW_DATA_STORE_FILE_NAME = "wordflow.preferences_pb"

internal fun wordFlowDataStorePath(userHome: String): String =
    File(File(userHome), ".wordflow/$WORD_FLOW_DATA_STORE_FILE_NAME").absolutePath

internal fun wordFlowStorageDataStoreModule(userHome: String) =
    storageDataStoreModule {
        wordFlowDataStorePath(userHome).also { path ->
            check(File(path).parentFile.let { it.isDirectory || it.mkdirs() }) {
                "Unable to create the Word Flow preferences directory"
            }
        }
    }

private val WordFlowStorageDataStoreModule =
    wordFlowStorageDataStoreModule(checkNotNull(System.getProperty("user.home")))

actual fun getPlatformModules(): List<Module> = listOf(WordFlowStorageDataStoreModule)
