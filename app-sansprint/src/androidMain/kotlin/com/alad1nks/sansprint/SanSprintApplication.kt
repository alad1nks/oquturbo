package com.alad1nks.sansprint

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SanSprintApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SanSprintApplication)
            modules(getPlatformModules() + getCommonModules())
        }
    }
}
