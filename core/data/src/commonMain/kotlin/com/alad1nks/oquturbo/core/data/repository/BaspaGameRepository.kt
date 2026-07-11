package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.flow.Flow

class BaspaGameRepository(private val storage: Storage) {
    fun getRecord(mode: String): Flow<Int?> = storage.getBaspaGameRecord(mode)

    suspend fun setRecord(mode: String, record: Int) {
        storage.setBaspaGameRecord(mode, record)
    }
}
