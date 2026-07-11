package com.alad1nks.oquturbo.core.storage.common

import kotlinx.coroutines.flow.Flow

internal class StorageImpl(
    private val appPreferences: AppPreferences,
) : Storage {
    override fun getDarkTheme(): Flow<Boolean?> {
        return appPreferences.getBoolean(DARK_THEME)
    }

    override fun getKenKozGameRecord(mode: String): Flow<Int?> {
        return appPreferences.getInt("${KENKOZ_GAME_RECORD}_$mode")
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

    override suspend fun setKenKozGameRecord(
        mode: String,
        record: Int,
    ) {
        appPreferences.setInt("${KENKOZ_GAME_RECORD}_$mode", record)
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
        const val KENKOZ_GAME_RECORD = "kenkoz_game_record"
        const val REMEMBER_NUMBER_RECORD = "remember_number_record"
    }
}
