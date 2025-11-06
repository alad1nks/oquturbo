package com.alad1nks.oquturbo.feature.remembernumber.data.repository

import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.flow.Flow

internal class RememberNumberRepository(
    private val storage: Storage,
) {
    fun getRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
    ): Flow<Int> {
        return storage.getRememberNumberRecord(maxLength, availableDigits)
    }

    suspend fun setRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
        record: Int,
    ) {
        storage.setRememberNumberRecord(maxLength, availableDigits, record)
    }
}
