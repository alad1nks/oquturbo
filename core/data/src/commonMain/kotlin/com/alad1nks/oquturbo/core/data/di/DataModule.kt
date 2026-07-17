package com.alad1nks.oquturbo.core.data.di

import com.alad1nks.oquturbo.core.data.repository.BaspaGameRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.data.repository.KenKozGameRepository
import com.alad1nks.oquturbo.core.data.repository.ProfileRepository
import com.alad1nks.oquturbo.core.data.repository.RememberNumberRepository
import com.alad1nks.oquturbo.core.data.repository.SettingsRepository
import org.koin.dsl.module

val DataModule =
    module {
        single { BaspaGameRepository(get()) }
        single { GameActivityRepository(get()) }
        single { KenKozGameRepository(get()) }
        single { ProfileRepository(get()) }
        single { RememberNumberRepository(get()) }
        single { SettingsRepository(get()) }
    }
