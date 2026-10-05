@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.alad1nks.oquturbo.shared.reminders

import com.alad1nks.oquturbo.core.data.model.ReminderContent
import com.alad1nks.oquturbo.core.data.reminders.ReminderController
import com.alad1nks.oquturbo.core.data.repository.SettingsRepository
import com.alad1nks.oquturbo.resources.reminderResourceContent
import com.alad1nks.oquturbo.shared.getCommonModules
import com.alad1nks.oquturbo.shared.getPlatformModules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import platform.Foundation.NSCurrentLocaleDidChangeNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSystemTimeZoneDidChangeNotification

/** Constructed on Main by the launch delegate or view factory; Compose borrows this single graph. */
internal object IosReminderRuntime {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val platform = IosReminderPlatform()
    val application =
        koinApplication {
            modules(
                getCommonModules() + getPlatformModules() +
                    module {
                        single {
                            PracticeReminderController(get(), platform, scope) { code ->
                                reminderResourceContent(code).let { ReminderContent(code, it.title, it.body) }
                            }
                        }
                        single<ReminderController> { get<PracticeReminderController>() }
                    },
            )
        }
    val controller get() = application.koin.get<PracticeReminderController>()
    val homeActions = ReminderHomeActions()
    val responses = mutableSetOf<String>()

    // NotificationCenter keeps these registrations; retain tokens for the process owner's lifetime too.
    private val observers =
        listOf(NSCurrentLocaleDidChangeNotification, NSSystemTimeZoneDidChangeNotification).map { name ->
            NSNotificationCenter.defaultCenter.addObserverForName(name, null, NSOperationQueue.mainQueue) { refresh() }
        }

    init {
        IosReminderDiagnostics.record("runtime-created")
        IosReminderDiagnostics.capture(platform.center)
        scope.launch {
            application.koin.get<SettingsRepository>().getLanguage().retryWhen { error, _ ->
                if (error is CancellationException) {
                    false
                } else {
                    delay(5_000)
                    true
                }
            }.distinctUntilChanged().drop(1).collectLatest { refresh() }
        }
    }

    fun refresh() {
        controller.refresh()
        IosReminderDiagnostics.capture(platform.center)
    }
}
