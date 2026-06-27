package com.alad1nks.oquturbo.core.storage.web.di

import com.alad1nks.oquturbo.core.storage.common.AppPreferences
import com.alad1nks.oquturbo.core.storage.web.AppPreferencesImpl
import org.koin.dsl.module

val StorageWebModule =
    module {
        single<AppPreferences> { AppPreferencesImpl() }
    }
