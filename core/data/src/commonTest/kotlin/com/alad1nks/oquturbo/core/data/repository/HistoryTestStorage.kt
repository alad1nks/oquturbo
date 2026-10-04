package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

internal const val DAY_MILLIS = 86_400_000L

@OptIn(ExperimentalTime::class)
internal class HistoryClock(var day: Long = 100) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(day * DAY_MILLIS)
}

internal class HistoryTestStorage : Storage {
    val reminderSchedule = MutableStateFlow<String?>(null)
    val reminderEnabled = MutableStateFlow<Boolean?>(null)
    var beforeReminderWrite: suspend () -> Unit = {}
    var afterReminderWrite: suspend () -> Unit = {}
    var beforeReminderEnabledWrite: suspend () -> Unit = {}
    var afterReminderEnabledWrite: suspend () -> Unit = {}
    val focus = MutableStateFlow<String?>(null)
    var focusWrites = 0
    var beforeFocusRead: suspend () -> Unit = {}
    var beforeFocusWrite: suspend (String) -> Unit = {}
    var afterFocusWrite: suspend () -> Unit = {}
    val activity = MutableStateFlow<String?>(null)
    val progress = MutableStateFlow<String?>(null)
    val plan = MutableStateFlow<String?>(null)
    var activityWrites = 0
    var progressWrites = 0
    var planWrites = 0
    var beforeActivityRead: suspend () -> Unit = {}
    var beforeProgressRead: suspend () -> Unit = {}
    var beforeActivityWrite: suspend () -> Unit = {}
    var beforeProgressWrite: suspend () -> Unit = {}
    var beforePlanWrite: suspend () -> Unit = {}

    override fun getGameSessionsJson(): Flow<String?> =
        flow {
            beforeActivityRead()
            emitAll(activity)
        }

    override fun getDailyTrainingProgressJson(): Flow<String?> =
        flow {
            beforeProgressRead()
            emitAll(progress)
        }

    override fun getDailyTrainingJson(): Flow<String?> = plan

    override suspend fun setGameSessionsJson(value: String) {
        beforeActivityWrite()
        activity.value = value
        activityWrites++
    }

    override suspend fun setDailyTrainingProgressJson(value: String) {
        beforeProgressWrite()
        progress.value = value
        progressWrites++
    }

    override suspend fun setDailyTrainingJson(value: String) {
        beforePlanWrite()
        plan.value = value
        planWrites++
    }

    override fun getWeeklyFocusJson(): Flow<String?> =
        flow {
            beforeFocusRead()
            emitAll(focus)
        }

    override suspend fun setWeeklyFocusJson(value: String) {
        beforeFocusWrite(value)
        focus.value = value
        focusWrites++
        afterFocusWrite()
    }

    override fun getDarkTheme(): Flow<Boolean?> = flowOf(null)

    override fun getLanguageCode(): Flow<String?> = flowOf(null)

    override fun getSoundEnabled(): Flow<Boolean?> = flowOf(null)

    override fun getVibrationEnabled(): Flow<Boolean?> = flowOf(null)

    override fun getRemindersScheduleJson(): Flow<String?> = reminderSchedule

    override suspend fun setRemindersScheduleJson(value: String) {
        beforeReminderWrite()
        reminderSchedule.value = value
        afterReminderWrite()
    }

    override fun getRemindersEnabled(): Flow<Boolean?> = reminderEnabled

    override fun getProfilePreferencesJson(): Flow<String?> = flowOf(null)

    override fun getBaspaGameRecord(mode: String): Flow<Int?> = flowOf(null)

    override fun getKenKozGameRecord(mode: String): Flow<Int?> = flowOf(null)

    override fun getRememberNumberRecord(maxLength: Int, availableDigits: String): Flow<Int?> = flowOf(null)

    override suspend fun setDarkTheme(value: Boolean) = Unit

    override suspend fun setLanguageCode(value: String) = Unit

    override suspend fun setSoundEnabled(value: Boolean) = Unit

    override suspend fun setVibrationEnabled(value: Boolean) = Unit

    override suspend fun setRemindersEnabled(value: Boolean) {
        beforeReminderEnabledWrite()
        reminderEnabled.value = value
        afterReminderEnabledWrite()
    }

    override suspend fun setProfilePreferencesJson(value: String) = Unit

    override suspend fun setBaspaGameRecord(mode: String, record: Int) = Unit

    override suspend fun setKenKozGameRecord(mode: String, record: Int) = Unit

    override suspend fun setRememberNumberRecord(maxLength: Int, availableDigits: String, record: Int) = Unit
}
