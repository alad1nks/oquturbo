package com.alad1nks.oquturbo.shared.reminders

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.alad1nks.oquturbo.core.data.model.ReminderContent
import com.alad1nks.oquturbo.core.data.reminders.ReminderController
import com.alad1nks.oquturbo.core.data.repository.SettingsRepository
import com.alad1nks.oquturbo.resources.reminderResourceContent
import com.alad1nks.oquturbo.shared.App
import com.alad1nks.oquturbo.shared.getCommonModules
import com.alad1nks.oquturbo.shared.getPlatformModules
import com.alad1nks.oquturbo.shared.ui.rememberOquTurboAppState
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
import kotlinx.coroutines.withTimeout
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.io.File

/** Only this process owner creates the Android Koin/DataStore graph; Compose borrows it. */
internal class AndroidReminderRuntime private constructor(val context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val platform = AndroidReminderPlatform(context)
    val application =
        koinApplication {
            androidContext(context)
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

    init {
        scope.launch {
            application.koin.get<SettingsRepository>().getLanguage().retryWhen { error, _ ->
                if (error is CancellationException) {
                    false
                } else {
                    delay(5_000)
                    true
                }
            }.distinctUntilChanged().drop(1).collectLatest {
                controller.refresh()
            }
        }
    }

    fun onIntent(intent: Intent?) {
        if (intent?.action != REMINDER_HOME_ACTION) return
        intent.action = null
        homeActions.offer()
        ReminderDiagnostics.record(context, "open-received", "event=${homeActions.pending.value}")
    }

    companion object {
        @Volatile private var instance: AndroidReminderRuntime? = null

        fun get(context: Context): AndroidReminderRuntime =
            instance ?: synchronized(this) {
                instance ?: AndroidReminderRuntime(context.applicationContext).also { instance = it }
            }
    }
}

class OquTurboApplication : Application(), Application.ActivityLifecycleCallbacks {
    private var started = 0

    override fun onCreate() {
        super.onCreate()
        AndroidReminderRuntime.get(this)
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        started++
        AndroidReminderRuntime.get(this).platform.foreground = true
    }

    override fun onActivityStopped(activity: Activity) {
        started--
        AndroidReminderRuntime.get(this).platform.foreground = started > 0
    }

    override fun onActivityResumed(activity: Activity) {
        AndroidReminderRuntime.get(this).controller.refresh()
    }

    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}

abstract class PracticeReminderActivity : ComponentActivity() {
    private var handledToken: String? = null
    private val permission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            AndroidReminderRuntime.get(this).platform.permissionReturned()
        }

    internal fun launchReminderPermission() = permission.launch(Manifest.permission.POST_NOTIFICATIONS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidReminderRuntime.get(this).also {
            it.platform.bind(this)
            handledToken = savedInstanceState?.getString("handledReminderOpen")
            val token = intent?.getStringExtra("reminderOpenToken")
            if (token != null && token != handledToken) {
                it.onIntent(intent)
                handledToken = token
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AndroidReminderRuntime.get(this).onIntent(intent)
        handledToken = intent.getStringExtra("reminderOpenToken")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        handledToken?.let { outState.putString("handledReminderOpen", it) }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        AndroidReminderRuntime.get(this).platform.unbind(this)
        super.onDestroy()
    }

    @Composable
    protected fun ReminderApp() {
        val runtime = AndroidReminderRuntime.get(this)
        val state = rememberOquTurboAppState()
        ReminderHomeNavigation(runtime.homeActions, state) { event ->
            ReminderDiagnostics.record(runtime.context, "open-consumed", "event=$event destination=Home")
        }
        App(state, emptyList(), emptyList(), runtime.application)
    }
}

class PracticeReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val accepted =
            setOf(
                REMINDER_ACTION,
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_LOCALE_CHANGED,
            )
        if (intent.action !in accepted) return
        val pending = goAsync()
        val runtime = AndroidReminderRuntime.get(context)
        runtime.scope.launch {
            try {
                withTimeout(8_000) {
                    if (intent.action == REMINDER_ACTION) {
                        runtime.controller.dispatch { runtime.platform.postIfCurrent(intent, it) }
                    } else {
                        runtime.controller.reconcileNow()
                    }
                }
            } catch (error: Exception) {
                ReminderDiagnostics.record(context, "receiver-failed", error.javaClass.simpleName)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Enabled only by a debug, app-private marker; observes operations and never drives them. */
internal object ReminderDiagnostics {
    fun record(context: Context, event: String, details: String) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        runCatching {
            if (File(context.filesDir, "reminder-diagnostics-enabled").isFile) {
                File(
                    context.filesDir,
                    "reminder-diagnostics.log",
                ).appendText("${System.currentTimeMillis()} $event $details\n")
            }
        }
    }
}
