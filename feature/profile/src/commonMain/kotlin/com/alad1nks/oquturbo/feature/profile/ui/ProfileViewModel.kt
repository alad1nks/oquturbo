package com.alad1nks.oquturbo.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.PlayerProgress
import com.alad1nks.oquturbo.core.data.model.ProfilePreferences
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.data.repository.ProfileRepository
import com.alad1nks.oquturbo.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class ProfileViewModel(
    private val activityRepository: GameActivityRepository,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private var isSavingIdentity = false

    val uiState: StateFlow<ProfileUiState> =
        combine(
            activityRepository.observeProgress(),
            activityRepository.observeRecords(),
            profileRepository.observePreferences(),
            activityRepository.observeSessions(),
        ) { progress, records, preferences, sessions ->
            createProfileUiState(progress, records, preferences, sessions)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue =
                createProfileUiState(
                    emptyPlayerProgress(),
                    emptyList(),
                    ProfilePreferences(),
                    emptyList(),
                ),
        )

    val settingsUiState: StateFlow<ProfileSettingsUiState> =
        combine(
            settingsRepository.getDarkTheme(),
            settingsRepository.getSoundEnabled(),
            settingsRepository.getVibrationEnabled(),
            settingsRepository.getRemindersEnabled(),
        ) { darkTheme, sound, vibration, reminders ->
            ProfileSettingsUiState(
                darkThemeEnabled = darkTheme == true,
                soundEnabled = sound ?: true,
                vibrationEnabled = vibration ?: true,
                remindersEnabled = reminders ?: false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ProfileSettingsUiState(),
        )

    fun updateIdentity(
        displayName: String,
        avatarId: PersonalizationId,
        onSaved: () -> Unit,
    ) {
        if (isSavingIdentity) return
        val avatar = uiState.value.personalization.firstOrNull { it.id == avatarId && it.isUnlocked } ?: return
        isSavingIdentity = true
        viewModelScope.launch {
            try {
                profileRepository.updateIdentity(
                    displayName = displayName,
                    selectedAvatar = avatar.id.name,
                )
                onSaved()
            } finally {
                isSavingIdentity = false
            }
        }
    }

    fun selectTitle(titleId: TitleId) {
        if (uiState.value.titles.none { it.id == titleId && it.isUnlocked }) return
        viewModelScope.launch { profileRepository.selectTitle(titleId.name) }
    }

    fun selectPersonalization(itemId: PersonalizationId) {
        val item = uiState.value.personalization.firstOrNull { it.id == itemId && it.isUnlocked } ?: return
        viewModelScope.launch {
            when (item.category) {
                PersonalizationCategory.Avatar -> profileRepository.selectAvatar(item.id.name)
                PersonalizationCategory.AvatarFrame -> profileRepository.selectFrame(item.id.name)
                PersonalizationCategory.CardBackground -> profileRepository.selectBackground(item.id.name)
            }
        }
    }

    fun setDarkTheme(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(value)
        }
    }

    fun setSoundEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setSoundEnabled(value) }
    }

    fun setVibrationEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setVibrationEnabled(value) }
    }

    fun setRemindersEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setRemindersEnabled(value) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

internal data class ProfileSettingsUiState(
    val darkThemeEnabled: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val remindersEnabled: Boolean = false,
)

private fun emptyPlayerProgress() =
    PlayerProgress(
        totalCorrectAnswers = 0,
        totalXp = 0,
        level = 1,
        currentLevelXp = 0,
    )
