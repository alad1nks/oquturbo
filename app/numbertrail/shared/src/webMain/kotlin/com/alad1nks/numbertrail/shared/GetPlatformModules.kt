package com.alad1nks.numbertrail.shared

import com.alad1nks.oquturbo.core.storage.web.di.storageWebModule
import org.koin.core.module.Module

actual fun getPlatformModules(): List<Module> = listOf(storageWebModule(namespace = "numbertrail"))
