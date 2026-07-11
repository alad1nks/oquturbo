package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.flow.Flow

class KenKozGameRepository(
    private val storage: Storage,
) {
    fun getRecord(mode: String): Flow<Int?> {
        return storage.getKenKozGameRecord(mode)
    }

    suspend fun setRecord(
        mode: String,
        record: Int,
    ) {
        storage.setKenKozGameRecord(mode, record)
    }
}
