package com.alad1nks.oquturbo.core.storage.web

import com.alad1nks.oquturbo.core.storage.common.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

internal class AppPreferencesImpl : AppPreferences {
    private val stringPreferences = MutableStateFlow(emptyMap<String, String?>())
    private val booleanPreferences = MutableStateFlow(emptyMap<String, Boolean?>())
    private val intPreferences = MutableStateFlow(emptyMap<String, Int?>())

    override fun getString(key: String): Flow<String?> {
        return stringPreferences
            .onStart { refreshString(key) }
            .map { it[key] }
    }

    override fun getBoolean(key: String): Flow<Boolean?> {
        return booleanPreferences
            .onStart { refreshBoolean(key) }
            .map { it[key] }
    }

    override fun getInt(key: String): Flow<Int?> {
        return intPreferences
            .onStart { refreshInt(key) }
            .map { it[key] }
    }

    override suspend fun setBoolean(key: String, value: Boolean) {
        val booleanPreferencesCopy = booleanPreferences.value.toMutableMap()
        booleanPreferencesCopy[key] = value
        booleanPreferences.value = booleanPreferencesCopy.toMap()

        localStorage.setItem(key, value.toString())
    }

    override suspend fun setString(key: String, value: String) {
        val stringPreferencesCopy = stringPreferences.value.toMutableMap()
        stringPreferencesCopy[key] = value
        stringPreferences.value = stringPreferencesCopy.toMap()

        localStorage.setItem(key, value)
    }

    override suspend fun setInt(key: String, value: Int) {
        val intPreferencesCopy = intPreferences.value.toMutableMap()
        intPreferencesCopy[key] = value
        intPreferences.value = intPreferencesCopy.toMap()

        localStorage.setItem(key, value.toString())
    }

    private fun refreshString(key: String) {
        stringPreferences.value = stringPreferences.value + (key to localStorage.getItem(key))
    }

    private fun refreshBoolean(key: String) {
        booleanPreferences.value = booleanPreferences.value + (key to localStorage.getItem(key)?.toBoolean())
    }

    private fun refreshInt(key: String) {
        intPreferences.value = intPreferences.value + (key to localStorage.getItem(key)?.toInt())
    }
}
