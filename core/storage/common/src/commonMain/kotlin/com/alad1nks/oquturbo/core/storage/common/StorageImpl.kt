package com.alad1nks.oquturbo.core.storage.common

import kotlinx.coroutines.flow.Flow

internal class StorageImpl(
    private val appPreferences: AppPreferences,
) : Storage {
    override fun getDarkTheme(): Flow<Boolean?> {
        return appPreferences.getBoolean(DARK_THEME)
    }

    override fun getLanguageCode(): Flow<String?> {
        return appPreferences.getString(LANGUAGE)
    }

    override fun getSoundEnabled(): Flow<Boolean?> {
        return appPreferences.getBoolean(SOUND_ENABLED)
    }

    override fun getVibrationEnabled(): Flow<Boolean?> {
        return appPreferences.getBoolean(VIBRATION_ENABLED)
    }

    override fun getRemindersEnabled(): Flow<Boolean?> {
        return appPreferences.getBoolean(REMINDERS_ENABLED)
    }

    override fun getGameSessionsJson(): Flow<String?> {
        return appPreferences.getString(GAME_SESSIONS_V1)
    }

    override fun getProfilePreferencesJson(): Flow<String?> {
        return appPreferences.getString(PROFILE_PREFERENCES_V1)
    }

    override fun getBaspaGameRecord(mode: String): Flow<Int?> {
        return appPreferences.getInt("${BASPA_GAME_RECORD}_$mode")
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

    override suspend fun setLanguageCode(value: String) {
        appPreferences.setString(LANGUAGE, value)
    }

    override suspend fun setSoundEnabled(value: Boolean) {
        appPreferences.setBoolean(SOUND_ENABLED, value)
    }

    override suspend fun setVibrationEnabled(value: Boolean) {
        appPreferences.setBoolean(VIBRATION_ENABLED, value)
    }

    override suspend fun setRemindersEnabled(value: Boolean) {
        appPreferences.setBoolean(REMINDERS_ENABLED, value)
    }

    override suspend fun setGameSessionsJson(value: String) {
        appPreferences.setString(GAME_SESSIONS_V1, value)
    }

    override suspend fun setProfilePreferencesJson(value: String) {
        appPreferences.setString(PROFILE_PREFERENCES_V1, value)
    }

    override suspend fun setBaspaGameRecord(
        mode: String,
        record: Int,
    ) {
        appPreferences.setInt("${BASPA_GAME_RECORD}_$mode", record)
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
        const val BASPA_GAME_RECORD = "baspa_game_record"
        const val DARK_THEME = "dark_theme"
        const val GAME_SESSIONS_V1 = "game_sessions_v1"
        const val KENKOZ_GAME_RECORD = "kenkoz_game_record"
        const val LANGUAGE = "language"
        const val PROFILE_PREFERENCES_V1 = "profile_preferences_v1"
        const val REMEMBER_NUMBER_RECORD = "remember_number_record"
        const val REMINDERS_ENABLED = "reminders_enabled"
        const val SOUND_ENABLED = "sound_enabled"
        const val VIBRATION_ENABLED = "vibration_enabled"
    }
}
