package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.flow.Flow

class SettingsRepository(
    private val storage: Storage,
) {
    fun getDarkTheme(): Flow<Boolean?> {
        return storage.getDarkTheme()
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

    suspend fun setSoundEnabled(value: Boolean) {
        storage.setSoundEnabled(value)
    }

    suspend fun setVibrationEnabled(value: Boolean) {
        storage.setVibrationEnabled(value)
    }

    suspend fun setRemindersEnabled(value: Boolean) {
        storage.setRemindersEnabled(value)
    }
}
