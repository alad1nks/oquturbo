package com.alad1nks.oquturbo.shared.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import com.alad1nks.oquturbo.core.data.model.ReminderSchedule
import com.alad1nks.oquturbo.core.data.reminders.ReminderAuthorization
import com.alad1nks.oquturbo.core.data.reminders.ReminderCapability
import com.alad1nks.oquturbo.core.data.reminders.ReminderNativeState
import com.alad1nks.oquturbo.core.data.reminders.ReminderPending
import com.alad1nks.oquturbo.core.data.reminders.ReminderPickerLabels
import com.alad1nks.oquturbo.core.data.reminders.ReminderPlatform
import com.alad1nks.oquturbo.resources.reminderResourceContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

internal const val REMINDER_ACTION = "com.alad1nks.oquturbo.PRACTICE_REMINDER"
internal const val REMINDER_HOME_ACTION = "com.alad1nks.oquturbo.PRACTICE_HOME"
private const val CHANNEL = "oquturbo_practice_reminder"
private const val NOTIFICATION_ID = 6001
private const val ALARM_REQUEST = 6001
private const val OPEN_REQUEST = 6002

internal suspend fun cancelReminderNotification(
    cancel: () -> Unit,
    activeNotifications: () -> List<Pair<Int, String?>>,
    monotonicNanos: () -> Long = System::nanoTime,
) {
    cancel()
    // Android queues notification cancellation on its service handler. Observe completion without
    // repeating the mutation; a timeout remains an ordinary incomplete-cancellation failure.
    val started = monotonicNanos()
    val removed =
        withTimeoutOrNull(2_000) {
            while (true) {
                val owned = activeNotifications().any { (id, tag) -> id == NOTIFICATION_ID && tag == CHANNEL }
                currentCoroutineContext().ensureActive()
                // A Binder read cannot be preempted; never accept a late result as within budget.
                check(monotonicNanos() - started < 2_000_000_000L) {
                    "Notification inventory exceeded cancellation budget"
                }
                if (!owned) return@withTimeoutOrNull true
                delay(50)
            }
        }
    check(removed == true) { "Owned reminder notification cancellation was not confirmed" }
}

/** Floating wall-clock resolution belongs to the platform calendar, including DST gaps/folds. */
internal fun nextReminderTime(minutes: Int, now: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    require(minutes in 0..1439)
    val calendar =
        Calendar.getInstance(zone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    if (calendar.timeInMillis <= now) {
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, minutes / 60)
        calendar.set(Calendar.MINUTE, minutes % 60)
    }
    return calendar.timeInMillis
}

/** Display a wall-clock choice without resolving it against today's date or DST rules. */
internal fun formatReminderTime(minutesOfDay: Int, formatter: java.text.DateFormat): String {
    require(minutesOfDay in 0..1439)
    val timeOnlyFormatter =
        (formatter.clone() as java.text.DateFormat).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    return timeOnlyFormatter.format(Date(minutesOfDay * 60_000L))
}

internal class AndroidReminderPlatform(private val context: Context) : ReminderPlatform {
    override val capability = ReminderCapability.Supported
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private var activity: PracticeReminderActivity? = null
    private var picker: TimePickerDialog? = null
    private var pickerResult: CompletableDeferred<Int?>? = null
    private var permissionResult: CompletableDeferred<Unit>? = null
    private var permissionMayBeRequested = false
    var foreground = false

    fun bind(host: PracticeReminderActivity) {
        activity = host
    }

    fun unbind(host: PracticeReminderActivity) {
        if (activity !== host) return
        picker?.dismiss()
        picker = null
        pickerResult?.complete(null)
        pickerResult = null
        permissionResult?.complete(Unit)
        permissionResult = null
        activity = null
    }

    fun permissionReturned() {
        // False rationale does not prove permanent denial. Keep explicit Allow after a live dismiss;
        // settings is always available too. A restored process conservatively uses settings.
        permissionMayBeRequested = activity?.shouldShowRequestPermissionRationale(
            Manifest.permission.POST_NOTIFICATIONS,
        ) == false
        permissionResult?.complete(Unit)
        permissionResult = null
    }

    override fun systemLanguage(): String =
        Resources.getSystem().configuration.locales[0].language.let {
            it.takeIf { code -> code in setOf("en", "ru", "kk") } ?: "en"
        }

    override fun formatTime(minutesOfDay: Int): String =
        formatReminderTime(minutesOfDay, DateFormat.getTimeFormat(activity ?: context))

    override suspend fun inspect(): ReminderNativeState {
        val permission =
            Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        val channel = if (Build.VERSION.SDK_INT >= 26) notifications.getNotificationChannel(CHANNEL) else null
        val allowed =
            permission && notifications.areNotificationsEnabled() &&
                (channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE)
        return ReminderNativeState(
            when {
                allowed -> ReminderAuthorization.Allowed
                !permission && permissionMayBeRequested && activity != null -> ReminderAuthorization.Requestable
                else -> ReminderAuthorization.Blocked
            },
            limited = channel != null && channel.importance < NotificationManager.IMPORTANCE_DEFAULT,
            pending = ReminderPending.Unknown,
        )
    }

    private fun alarmIntent(flags: Int, schedule: ReminderSchedule? = null, target: Long = 0): PendingIntent? {
        val intent = Intent(context, PracticeReminderReceiver::class.java).setAction(REMINDER_ACTION)
        if (schedule != null) {
            intent.putExtra("minutes", schedule.minutesOfDay)
            intent.putExtra("target", target)
            intent.putExtra("language", schedule.content.languageCode)
        }
        return PendingIntent.getBroadcast(context, ALARM_REQUEST, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    override suspend fun replace(schedule: ReminderSchedule) {
        check(inspect().authorization == ReminderAuthorization.Allowed)
        val copy = reminderResourceContent(schedule.content.languageCode)
        if (Build.VERSION.SDK_INT >= 26) {
            notifications.createNotificationChannel(
                NotificationChannel(CHANNEL, copy.channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = copy.channelDescription
                },
            )
            check(notifications.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)
        }
        val target = nextReminderTime(schedule.minutesOfDay, System.currentTimeMillis())
        alarms.set(
            AlarmManager.RTC_WAKEUP,
            target,
            requireNotNull(alarmIntent(PendingIntent.FLAG_UPDATE_CURRENT, schedule, target)),
        )
        ReminderDiagnostics.record(context, "scheduled", "target=$target minutes=${schedule.minutesOfDay}")
    }

    override suspend fun cancel() {
        alarmIntent(PendingIntent.FLAG_NO_CREATE)?.let {
            alarms.cancel(it)
            it.cancel()
        }
        cancelReminderNotification(
            cancel = { notifications.cancel(CHANNEL, NOTIFICATION_ID) },
            activeNotifications = { notifications.activeNotifications.map { it.id to it.tag } },
        )
        ReminderDiagnostics.record(context, "cancelled", "owned")
    }

    fun postIfCurrent(intent: Intent, schedule: ReminderSchedule) {
        val target = intent.getLongExtra("target", 0)
        val now = System.currentTimeMillis()
        val targetDay = Calendar.getInstance().apply { timeInMillis = target }
        val nowDay = Calendar.getInstance().apply { timeInMillis = now }
        val sameDate =
            targetDay.get(Calendar.ERA) == nowDay.get(Calendar.ERA) &&
                targetDay.get(Calendar.YEAR) == nowDay.get(Calendar.YEAR) &&
                targetDay.get(Calendar.DAY_OF_YEAR) == nowDay.get(Calendar.DAY_OF_YEAR)
        val current =
            intent.getIntExtra("minutes", -1) == schedule.minutesOfDay &&
                intent.getStringExtra("language") == schedule.content.languageCode
        ReminderDiagnostics.record(context, "dispatch", "foreground=$foreground current=$current sameDate=$sameDate")
        if (foreground || !current || !sameDate || target > now) return
        val launch =
            requireNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
                .setAction(
                    REMINDER_HOME_ACTION,
                ).putExtra(
                    "reminderOpenToken",
                    java.util.UUID.randomUUID().toString(),
                ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val open =
            PendingIntent.getActivity(
                context,
                OPEN_REQUEST,
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val builder =
            if (Build.VERSION.SDK_INT >= 26) {
                Notification.Builder(
                    context,
                    CHANNEL,
                )
            } else {
                Notification.Builder(context)
            }
        val notification =
            builder.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(schedule.content.title).setContentText(schedule.content.body)
                .setStyle(Notification.BigTextStyle().bigText(schedule.content.body))
                .setContentIntent(open).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER).build()
        notifications.notify(CHANNEL, NOTIFICATION_ID, notification)
        ReminderDiagnostics.record(context, "posted", "id=$NOTIFICATION_ID")
    }

    override suspend fun requestPermission() =
        withContext(Dispatchers.Main.immediate) {
            if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return@withContext
            }
            val host = requireNotNull(activity) { "No active permission host" }
            if (host.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) return@withContext
            val result = CompletableDeferred<Unit>()
            permissionResult = result
            try {
                host.launchReminderPermission()
                result.await()
            } finally {
                if (permissionResult === result) permissionResult = null
            }
        }

    override suspend fun chooseTime(initialMinutes: Int?, labels: ReminderPickerLabels): Int? =
        withContext(Dispatchers.Main.immediate) {
            val host = requireNotNull(activity) { "No active picker host" }
            val now = Calendar.getInstance()
            val minutes = initialMinutes ?: (now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE))
            val result = CompletableDeferred<Int?>()
            pickerResult = result
            val dialog =
                TimePickerDialog(
                    host,
                    { _, hour, minute -> result.complete(hour * 60 + minute) },
                    minutes / 60,
                    minutes % 60,
                    DateFormat.is24HourFormat(host),
                )
            picker = dialog
            dialog.setTitle(labels.title)
            dialog.setMessage(labels.helper)
            dialog.setButton(TimePickerDialog.BUTTON_POSITIVE, labels.confirm, dialog)
            dialog.setButton(TimePickerDialog.BUTTON_NEGATIVE, labels.cancel) { _, _ -> result.complete(null) }
            dialog.setOnCancelListener { result.complete(null) }
            dialog.setOnDismissListener { result.complete(null) }
            try {
                dialog.show()
                result.await()
            } finally {
                dialog.dismiss()
                if (picker === dialog) picker = null
                if (pickerResult === result) pickerResult = null
            }
        }

    override suspend fun openSystemSettings() {
        val intent =
            if (Build.VERSION.SDK_INT >= 26) {
                Intent(
                    Settings.ACTION_APP_NOTIFICATION_SETTINGS,
                ).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            }
        (activity ?: context).startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
