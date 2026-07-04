package com.alad1nks.oquturbo.core.storage.common

import kotlinx.coroutines.flow.Flow

internal class StorageImpl(
    private val appPreferences: AppPreferences,
) : Storage {
    override fun getDarkTheme(): Flow<Boolean?> {
        return appPreferences.getBoolean(DARK_THEME)
    }

    override fun getRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
    ): Flow<Int?> {
        return appPreferences.getInt("${REMEMBER_NUMBER_RECORD}_${maxLength}_$availableDigits")
    }

    override suspend fun setDarkTheme(value: Boolean) {
        appPreferences.setBoolean(DARK_THEME, value)
    }

    override suspend fun setRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
        record: Int,
    ) {
        appPreferences.setInt("${REMEMBER_NUMBER_RECORD}_${maxLength}_$availableDigits", record)
    }

    private companion object {
        const val DARK_THEME = "dark_theme"
        const val REMEMBER_NUMBER_RECORD = "remember_number_record"
    }
}
