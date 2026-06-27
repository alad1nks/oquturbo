package com.alad1nks.oquturbo.core.storage.common

import kotlinx.coroutines.flow.Flow

internal class StorageImpl(
    private val appPreferences: AppPreferences,
) : Storage {
    override fun getRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
    ): Flow<Int?> {
        return appPreferences.getInt("${REMEMBER_NUMBER_RECORD}_${maxLength}_$availableDigits")
    }

    override suspend fun setRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
        record: Int,
    ) {
        appPreferences.setInt("${REMEMBER_NUMBER_RECORD}_${maxLength}_$availableDigits", record)
    }

    private companion object {
        const val REMEMBER_NUMBER_RECORD = "remember_number_record"
    }
}
