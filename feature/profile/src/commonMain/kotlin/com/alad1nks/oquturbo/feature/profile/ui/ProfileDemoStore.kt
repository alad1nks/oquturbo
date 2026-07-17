package com.alad1nks.oquturbo.feature.profile.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class ProfileDemoStore {
    private val _uiState = MutableStateFlow(ProfileDemoData.appDemo)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun updateIdentity(
        displayName: String,
        avatarId: PersonalizationId,
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                displayName = displayName.trim().ifEmpty { null },
                personalization =
                    currentState.personalization.map { item ->
                        if (item.category == PersonalizationCategory.Avatar) {
                            item.copy(isSelected = item.id == avatarId)
                        } else {
                            item
                        }
                    },
            )
        }
    }

    fun selectTitle(titleId: TitleId) {
        _uiState.update { currentState ->
            if (currentState.titles.any { it.id == titleId && it.isUnlocked }) {
                currentState.copy(selectedTitle = titleId)
            } else {
                currentState
            }
        }
    }

    fun selectPersonalization(itemId: PersonalizationId) {
        _uiState.update { currentState ->
            val selectedItem = currentState.personalization.firstOrNull { it.id == itemId && it.isUnlocked }
            if (selectedItem == null) {
                currentState
            } else {
                currentState.copy(
                    personalization =
                        currentState.personalization.map { item ->
                            if (item.category == selectedItem.category) {
                                item.copy(isSelected = item.id == itemId)
                            } else {
                                item
                            }
                        },
                )
            }
        }
    }
}
