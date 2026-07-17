package com.alad1nks.oquturbo.shared

import com.alad1nks.oquturbo.core.data.di.DataModule
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import com.alad1nks.oquturbo.feature.baspagame.di.BaspaGameModule
import com.alad1nks.oquturbo.feature.kenkozgame.di.KenKozGameModule
import com.alad1nks.oquturbo.feature.main.di.MainModule
import com.alad1nks.oquturbo.feature.profile.di.ProfileModule
import com.alad1nks.oquturbo.feature.remembernumber.di.RememberNumberModule
import com.alad1nks.oquturbo.feature.remembernumbermenu.di.rememberNumberMenuModule
import com.alad1nks.oquturbo.feature.stats.di.StatsModule
import org.koin.core.module.Module

fun getCommonModules(): List<Module> {
    return listOf(
        DataModule,
        BaspaGameModule,
        KenKozGameModule,
        MainModule,
        ProfileModule,
        RememberNumberModule,
        rememberNumberMenuModule(showThemeIcon = false),
        StatsModule,
        StorageCommonModule,
    )
}
