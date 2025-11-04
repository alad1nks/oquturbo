package com.alad1nks.oquturbo

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class OquTurboApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@OquTurboApplication)
            modules(getPlatformModules() + getCommonModules())
        }
    }
}
