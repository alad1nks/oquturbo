package com.alad1nks.ruleswitch.shared

import com.alad1nks.oquturbo.core.storage.datastore.di.storageDataStoreModule
import org.koin.core.module.Module
import java.io.File

internal const val RULE_SWITCH_DATA_STORE_FILE_NAME = "ruleswitch.preferences_pb"

internal fun ruleSwitchDataStorePath(userHome: String): String =
    File(File(userHome), ".ruleswitch/$RULE_SWITCH_DATA_STORE_FILE_NAME").absolutePath

internal fun ruleSwitchStorageDataStoreModule(userHome: String) =
    storageDataStoreModule {
        ruleSwitchDataStorePath(userHome).also { path ->
            check(File(path).parentFile.let { it.isDirectory || it.mkdirs() }) {
                "Unable to create the Rule Switch preferences directory"
            }
        }
    }

private val RuleSwitchStorageDataStoreModule =
    ruleSwitchStorageDataStoreModule(checkNotNull(System.getProperty("user.home")))

actual fun getPlatformModules(): List<Module> = listOf(RuleSwitchStorageDataStoreModule)
