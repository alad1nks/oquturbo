package com.alad1nks.oquturbo

import com.alad1nks.oquturbo.feature.remembernumber.di.RememberNumberModule
import com.alad1nks.oquturbo.feature.remembernumbermenu.di.RememberNumberMenuModule
import org.koin.core.module.Module

fun getCommonModules(): List<Module> {
    return listOf(
        RememberNumberModule,
        RememberNumberMenuModule,
    )
}
