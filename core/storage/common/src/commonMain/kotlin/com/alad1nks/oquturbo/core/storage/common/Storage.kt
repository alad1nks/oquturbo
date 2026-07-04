package com.alad1nks.oquturbo.core.storage.common

import kotlinx.coroutines.flow.Flow

interface Storage {
    fun getDarkTheme(): Flow<Boolean?>

    fun getRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
    ): Flow<Int?>

    suspend fun setDarkTheme(value: Boolean)

    suspend fun setRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
        record: Int,
    )
}
