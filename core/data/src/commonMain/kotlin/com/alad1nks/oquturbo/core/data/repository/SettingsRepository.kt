package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.flow.Flow

class SettingsRepository(
    private val storage: Storage,
) {
    fun getDarkTheme(): Flow<Boolean?> {
        return storage.getDarkTheme()
    }

    suspend fun setDarkTheme(value: Boolean) {
        storage.setDarkTheme(value)
    }
}
