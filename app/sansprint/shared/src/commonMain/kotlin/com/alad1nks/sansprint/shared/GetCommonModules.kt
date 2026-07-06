package com.alad1nks.sansprint.shared

import com.alad1nks.oquturbo.core.data.di.DataModule
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import com.alad1nks.oquturbo.feature.main.di.MainModule
import com.alad1nks.oquturbo.feature.remembernumber.di.RememberNumberModule
import com.alad1nks.oquturbo.feature.remembernumbermenu.di.rememberNumberMenuModule
import org.koin.core.module.Module

fun getCommonModules(): List<Module> {
    return listOf(
        DataModule,
        MainModule,
        RememberNumberModule,
        rememberNumberMenuModule(showThemeIcon = true),
        StorageCommonModule,
    )
}
