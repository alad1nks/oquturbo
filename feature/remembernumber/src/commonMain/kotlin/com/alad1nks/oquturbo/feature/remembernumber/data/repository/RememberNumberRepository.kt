package com.alad1nks.oquturbo.feature.remembernumber.data.repository

import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.flow.Flow

internal class RememberNumberRepository(
    private val storage: Storage,
) {
    fun getRememberNumberRecord(): Flow<Int> {
        return storage.rememberNumberRecord
    }

    suspend fun setRememberNumberRecord(value: Int) {
        storage.setRememberNumberRecord(value)
    }
}
