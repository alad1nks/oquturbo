package com.alad1nks.oquturbo.shared

import com.alad1nks.oquturbo.core.data.reminders.ReminderCapability
import com.alad1nks.oquturbo.core.data.reminders.ReminderController
import com.alad1nks.oquturbo.core.data.reminders.UnavailableReminderController
import com.alad1nks.oquturbo.core.storage.datastore.di.StorageDataStoreModule
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getPlatformModules(): List<Module> {
    return listOf(
        StorageDataStoreModule,
        module { single<ReminderController> { UnavailableReminderController(ReminderCapability.IosPending) } },
    )
}
