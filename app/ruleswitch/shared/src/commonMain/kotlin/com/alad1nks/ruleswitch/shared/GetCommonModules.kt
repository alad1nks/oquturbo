package com.alad1nks.ruleswitch.shared

import com.alad1nks.oquturbo.core.data.di.DataModule
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import com.alad1nks.oquturbo.feature.main.di.MainModule
import com.alad1nks.oquturbo.feature.ruleswitch.di.RuleSwitchModule
import org.koin.core.module.Module

fun getCommonModules(): List<Module> {
    return listOf(RuleSwitchModule, DataModule, MainModule, StorageCommonModule)
}
