package com.alad1nks.rotationmatch.shared

import com.alad1nks.oquturbo.core.storage.web.di.StorageWebModule
import org.koin.core.module.Module

actual fun getPlatformModules(): List<Module> = listOf(StorageWebModule)
