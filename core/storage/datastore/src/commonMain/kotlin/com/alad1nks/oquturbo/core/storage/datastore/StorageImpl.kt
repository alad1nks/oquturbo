package com.alad1nks.oquturbo.core.storage.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal class StorageImpl(
    private val dataStore: DataStore<Preferences>,
) : Storage {
    override fun getRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
    ): Flow<Int> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[intPreferencesKey("${REMEMBER_NUMBER_RECORD}_${maxLength}_$availableDigits")] ?: 0
            }
    }

    override suspend fun setRememberNumberRecord(
        maxLength: Int,
        availableDigits: String,
        record: Int,
    ) {
        dataStore.edit { preferences ->
            preferences[intPreferencesKey("${REMEMBER_NUMBER_RECORD}_${maxLength}_$availableDigits")] = record
        }
    }

    private companion object {
        const val REMEMBER_NUMBER_RECORD = "remember_number_record"
    }
}
