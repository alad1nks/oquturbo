package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.data.model.AppLanguage
import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val storage: Storage,
) {
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

    suspend fun setRemindersEnabled(value: Boolean) {
        storage.setRemindersEnabled(value)
    }

    private companion object {
        const val SYSTEM_LANGUAGE_CODE = "system"
    }
}
