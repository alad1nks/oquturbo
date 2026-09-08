package com.alad1nks.numbertrail.shared

import com.alad1nks.oquturbo.core.data.di.DataModule
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import com.alad1nks.oquturbo.feature.main.di.MainModule
import com.alad1nks.oquturbo.feature.numbertrail.di.NumberTrailModule
import org.koin.core.module.Module

fun getCommonModules(): List<Module> {
    return listOf(NumberTrailModule, DataModule, MainModule, StorageCommonModule)
}
