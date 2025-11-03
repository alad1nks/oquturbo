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
    override val rememberNumberRecord: Flow<Int>
        get() =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .map { preferences ->
                    preferences[REMEMBER_NUMBER_RECORD] ?: 0
                }

    override suspend fun setRememberNumberRecord(value: Int) {
        dataStore.edit { preferences ->
            preferences[REMEMBER_NUMBER_RECORD] = value
        }
    }

    private companion object {
        val REMEMBER_NUMBER_RECORD = intPreferencesKey("remember_number_record")
    }
}
