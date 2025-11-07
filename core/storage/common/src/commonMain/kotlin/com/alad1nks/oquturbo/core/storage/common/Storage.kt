package com.alad1nks.oquturbo.core.storage.common

import kotlinx.coroutines.flow.Flow

interface Storage {
    fun getRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
    ): Flow<Int>

    suspend fun setRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
        record: Int,
    )
}
