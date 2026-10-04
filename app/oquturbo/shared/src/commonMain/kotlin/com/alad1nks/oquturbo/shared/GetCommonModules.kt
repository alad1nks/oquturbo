package com.alad1nks.oquturbo.shared

import com.alad1nks.oquturbo.core.data.di.DataModule
import com.alad1nks.oquturbo.core.data.reminders.ReminderController
import com.alad1nks.oquturbo.core.data.reminders.UnavailableReminderController
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import com.alad1nks.oquturbo.feature.baspagame.di.BaspaGameModule
import com.alad1nks.oquturbo.feature.dualfocus.di.DualFocusModule
import com.alad1nks.oquturbo.feature.home.di.HomeModule
import com.alad1nks.oquturbo.feature.kenkozgame.di.KenKozGameModule
import com.alad1nks.oquturbo.feature.main.di.MainModule
import com.alad1nks.oquturbo.feature.memorygrid.di.MemoryGridModule
import com.alad1nks.oquturbo.feature.numbertrail.di.NumberTrailModule
import com.alad1nks.oquturbo.feature.profile.di.ProfileModule
import com.alad1nks.oquturbo.feature.remembernumber.di.RememberNumberModule
import com.alad1nks.oquturbo.feature.remembernumbermenu.di.rememberNumberMenuModule
import com.alad1nks.oquturbo.feature.rotationmatch.di.RotationMatchModule
import com.alad1nks.oquturbo.feature.ruleswitch.di.RuleSwitchModule
import com.alad1nks.oquturbo.feature.stats.di.StatsModule
import com.alad1nks.oquturbo.feature.symbolcount.di.SymbolCountModule
import com.alad1nks.oquturbo.feature.wordflow.di.WordFlowModule
import org.koin.core.module.Module
import org.koin.dsl.module

fun getCommonModules(): List<Module> {
    return listOf(
        module { single<ReminderController> { UnavailableReminderController() } },
        DataModule,
        BaspaGameModule,
        HomeModule,
        KenKozGameModule,
        MainModule,
        MemoryGridModule,
        ProfileModule,
        RememberNumberModule,
        rememberNumberMenuModule(showThemeIcon = false),
        StatsModule,
        WordFlowModule,
        DualFocusModule,
        RotationMatchModule,
        NumberTrailModule,
        SymbolCountModule,
        RuleSwitchModule,
        StorageCommonModule,
    )
}
