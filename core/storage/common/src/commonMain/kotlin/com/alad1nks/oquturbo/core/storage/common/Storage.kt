package com.alad1nks.oquturbo.core.storage.common

import kotlinx.coroutines.flow.Flow

interface Storage {
    fun getDarkTheme(): Flow<Boolean?>

    fun getLanguageCode(): Flow<String?>

    fun getSoundEnabled(): Flow<Boolean?>

    fun getVibrationEnabled(): Flow<Boolean?>

    fun getRemindersEnabled(): Flow<Boolean?>

    fun getGameSessionsJson(): Flow<String?>

    fun getDailyTrainingJson(): Flow<String?>

    fun getProfilePreferencesJson(): Flow<String?>

    fun getBaspaGameRecord(mode: String): Flow<Int?>

    fun getKenKozGameRecord(mode: String): Flow<Int?>

    fun getRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
    ): Flow<Int?>

    suspend fun setDarkTheme(value: Boolean)

    suspend fun setLanguageCode(value: String)

    suspend fun setSoundEnabled(value: Boolean)

    suspend fun setVibrationEnabled(value: Boolean)

    suspend fun setRemindersEnabled(value: Boolean)

    suspend fun setGameSessionsJson(value: String)

    suspend fun setDailyTrainingJson(value: String)

    suspend fun setProfilePreferencesJson(value: String)

    suspend fun setBaspaGameRecord(
        mode: String,
        record: Int,
    )

    suspend fun setKenKozGameRecord(
        mode: String,
        record: Int,
    )

    suspend fun setRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
        record: Int,
    )
}
