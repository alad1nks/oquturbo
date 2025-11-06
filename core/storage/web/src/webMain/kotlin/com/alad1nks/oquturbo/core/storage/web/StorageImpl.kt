package com.alad1nks.oquturbo.core.storage.web

import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

external interface BrowserStorage {
    fun getItem(key: String): String?

    fun setItem(key: String, value: String)

    fun removeItem(key: String)

    fun clear()
}

external val localStorage: BrowserStorage

internal class StorageImpl : Storage {
    override fun getRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
    ): Flow<Int> {
        return flowOf(localStorage.getItem("${REMEMBER_NUMBER_RECORD}_${maxLength}_$availableDigits")?.toInt() ?: 0)
    }

    override suspend fun setRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
        record: Int,
    ) {
        localStorage.setItem("${REMEMBER_NUMBER_RECORD}_${maxLength}_$availableDigits", record.toString())
    }

    private companion object {
        const val REMEMBER_NUMBER_RECORD = "remember_number_record"
    }
}
