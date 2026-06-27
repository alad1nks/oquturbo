package com.alad1nks.oquturbo.core.storage.common

import kotlinx.coroutines.flow.Flow

interface AppPreferences {
    fun getBoolean(key: String): Flow<Boolean?>

    fun getInt(key: String): Flow<Int?>

    fun getString(key: String): Flow<String?>

    suspend fun setBoolean(key: String, value: Boolean)

    suspend fun setString(key: String, value: String)

    suspend fun setInt(key: String, value: Int)
}
