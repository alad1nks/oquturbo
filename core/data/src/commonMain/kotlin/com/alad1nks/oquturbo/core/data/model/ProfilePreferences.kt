package com.alad1nks.oquturbo.core.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfilePreferences(
    val displayName: String? = null,
    val selectedAvatar: String = "DefaultAvatar",
    val selectedTitle: String = "Starter",
    val selectedFrame: String = "DefaultFrame",
    val selectedBackground: String = "DefaultBackground",
)
