package com.alad1nks.oquturbo.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class ProfileViewModel(
    private val demoStore: ProfileDemoStore,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<ProfileUiState> = demoStore.uiState
    val darkTheme = settingsRepository.getDarkTheme().map { it == true }

    fun updateIdentity(
        displayName: String,
        avatarId: PersonalizationId,
    ) {
        demoStore.updateIdentity(
            displayName = displayName,
            avatarId = avatarId,
        )
    }

    fun selectTitle(titleId: TitleId) {
        demoStore.selectTitle(titleId)
    }

    fun selectPersonalization(itemId: PersonalizationId) {
        demoStore.selectPersonalization(itemId)
    }

    fun setDarkTheme(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(value)
        }
    }
}
