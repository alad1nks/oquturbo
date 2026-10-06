package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.data.model.AppLanguage
import com.alad1nks.oquturbo.core.data.model.ReminderSchedule
import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SettingsRepository(
    private val storage: Storage,
) {
    private val reminderMutex = Mutex()

    fun observeReminderSchedule(): Flow<ReminderSchedule?> =
        storage.getRemindersScheduleJson().map(
            ::decodeReminderSchedule,
        )

    /** Opaque snapshot for an explicit picker replacement, including malformed-but-readable data. */
    suspend fun readReminderScheduleSnapshot(): String? = storage.getRemindersScheduleJson().first()

    suspend fun readReminderSchedule(): ReminderSchedule? = observeReminderSchedule().first()

    suspend fun readRemindersDesired(): Boolean = storage.getRemindersEnabled().first() ?: false

    suspend fun saveReminderSchedule(schedule: ReminderSchedule, enable: Boolean = false) =
        reminderMutex.withLock {
            val payload = schedule.encodeReminder()
            storage.setRemindersScheduleJson(payload)
            if (enable) storage.setRemindersEnabled(true)
        }

    suspend fun enableSavedReminder() =
        reminderMutex.withLock {
            check(readReminderSchedule() != null) { "Reminder time is not configured" }
            storage.setRemindersEnabled(true)
        }

    fun getDarkTheme(): Flow<Boolean?> {
        return storage.getDarkTheme()
    }

    fun getLanguage(): Flow<AppLanguage> {
        return storage.getLanguageCode().map { languageCode ->
            AppLanguage.entries.firstOrNull { it.code == languageCode } ?: AppLanguage.System
        }
    }

    fun getSoundEnabled(): Flow<Boolean?> {
        return storage.getSoundEnabled()
    }

    fun getVibrationEnabled(): Flow<Boolean?> {
        return storage.getVibrationEnabled()
    }

    fun getRemindersEnabled(): Flow<Boolean?> {
        return storage.getRemindersEnabled()
    }

    suspend fun setDarkTheme(value: Boolean) {
        storage.setDarkTheme(value)
    }

    suspend fun setLanguage(value: AppLanguage) {
        storage.setLanguageCode(value.code ?: SYSTEM_LANGUAGE_CODE)
    }

    suspend fun setSoundEnabled(value: Boolean) {
        storage.setSoundEnabled(value)
    }

    suspend fun setVibrationEnabled(value: Boolean) {
        storage.setVibrationEnabled(value)
    }

    suspend fun setRemindersEnabled(value: Boolean) =
        reminderMutex.withLock {
            storage.setRemindersEnabled(value)
        }

    private companion object {
        const val SYSTEM_LANGUAGE_CODE = "system"
    }
}
