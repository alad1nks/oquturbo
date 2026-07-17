package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.data.model.ProfilePreferences
import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ProfileRepository(
    private val storage: Storage,
) {
    private val writeMutex = Mutex()
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    fun observePreferences(): Flow<ProfilePreferences> =
        storage.getProfilePreferencesJson()
            .map(::decodePreferences)
            .distinctUntilChanged()

    suspend fun updateIdentity(
        displayName: String?,
        selectedAvatar: String,
    ) {
        updatePreferences { preferences ->
            preferences.copy(
                displayName = displayName?.trim()?.ifEmpty { null },
                selectedAvatar = selectedAvatar,
            )
        }
    }

    suspend fun selectTitle(value: String) {
        updatePreferences { it.copy(selectedTitle = value) }
    }

    suspend fun selectAvatar(value: String) {
        updatePreferences { it.copy(selectedAvatar = value) }
    }

    suspend fun selectFrame(value: String) {
        updatePreferences { it.copy(selectedFrame = value) }
    }

    suspend fun selectBackground(value: String) {
        updatePreferences { it.copy(selectedBackground = value) }
    }

    private suspend fun updatePreferences(transform: (ProfilePreferences) -> ProfilePreferences) {
        writeMutex.withLock {
            val preferences = decodePreferences(storage.getProfilePreferencesJson().first())
            storage.setProfilePreferencesJson(
                json.encodeToString(
                    ProfilePreferencesPayload(preferences = transform(preferences)),
                ),
            )
        }
    }

    private fun decodePreferences(value: String?): ProfilePreferences {
        if (value.isNullOrBlank()) return ProfilePreferences()
        val payload =
            runCatching {
                json.decodeFromString<ProfilePreferencesPayload>(value)
            }.getOrElse { cause ->
                throw IllegalStateException("Stored profile preferences payload is malformed", cause)
            }
        check(payload.version == STORAGE_VERSION) {
            "Unsupported profile preferences payload version: ${payload.version}"
        }
        return payload.preferences
    }

    @Serializable
    private data class ProfilePreferencesPayload(
        val version: Int = STORAGE_VERSION,
        val preferences: ProfilePreferences = ProfilePreferences(),
    )

    private companion object {
        const val STORAGE_VERSION = 1
    }
}
