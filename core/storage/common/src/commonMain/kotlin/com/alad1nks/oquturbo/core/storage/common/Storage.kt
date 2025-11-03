package com.alad1nks.oquturbo.core.storage.common

import kotlinx.coroutines.flow.Flow

interface Storage {
    val rememberNumberRecord: Flow<Int>

    suspend fun setRememberNumberRecord(value: Int)
}
