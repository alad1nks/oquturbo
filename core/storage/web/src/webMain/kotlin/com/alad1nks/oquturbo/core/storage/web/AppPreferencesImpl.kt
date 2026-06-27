package com.alad1nks.oquturbo.core.storage.web

import com.alad1nks.oquturbo.core.storage.common.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.collections.set

internal class AppPreferencesImpl : AppPreferences {
    private val stringPreferences = MutableStateFlow(emptyMap<String, String?>())
    private val booleanPreferences = MutableStateFlow(emptyMap<String, Boolean?>())
    private val intPreferences = MutableStateFlow(emptyMap<String, Int?>())

    override fun getString(key: String): Flow<String?> {
        if (key !in stringPreferences.value) {
            val stringPreferencesCopy = stringPreferences.value.toMutableMap()
            stringPreferencesCopy[key] = localStorage.getItem(key)
            stringPreferences.value = stringPreferencesCopy.toMap()
        }

        return stringPreferences.map { it[key] }
    }

    override fun getBoolean(key: String): Flow<Boolean?> {
        if (key !in booleanPreferences.value) {
            val booleanPreferencesCopy = booleanPreferences.value.toMutableMap()
            booleanPreferencesCopy[key] = localStorage.getItem(key)?.toBoolean()
            booleanPreferences.value = booleanPreferencesCopy.toMap()
        }

        return booleanPreferences.map { it[key] }
    }

    override fun getInt(key: String): Flow<Int?> {
        if (key !in intPreferences.value) {
            val intPreferencesCopy = intPreferences.value.toMutableMap()
            intPreferencesCopy[key] = localStorage.getItem(key)?.toInt()
            intPreferences.value = intPreferencesCopy.toMap()
        }

        return intPreferences.map { it[key] }
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
}
