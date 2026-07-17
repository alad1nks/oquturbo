package com.alad1nks.oquturbo.feature.profile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.data.model.AppLanguage
import com.alad1nks.oquturbo.core.ui.component.AppTopBar
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileEditRouteContent(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember(uiState.displayName) { mutableStateOf(uiState.displayName.orEmpty()) }
    var selectedAvatar by
        remember(uiState.personalization) {
            mutableStateOf(
                uiState.personalization
                    .firstOrNull { it.category == PersonalizationCategory.Avatar && it.isSelected }
                    ?.id ?: PersonalizationId.DefaultAvatar,
            )
        }
    ProfileDetailScaffold(
        title = AppResource.String.profile_edit_title,
        onBackClick = onBackClick,
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(104.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = selectedAvatar.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(52.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        text = stringResource(AppResource.String.profile_edit_avatar_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.personalization
                            .filter { it.category == PersonalizationCategory.Avatar && it.isUnlocked }
                            .forEach { avatarItem ->
                                val avatar = avatarItem.id
                                Surface(
                                    onClick = { selectedAvatar = avatar },
                                    modifier = Modifier.size(54.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    color =
                                        if (selectedAvatar == avatar) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                    border =
                                        BorderStroke(
                                            1.dp,
                                            if (selectedAvatar == avatar) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                        ),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = avatar.icon(),
                                            contentDescription = stringResource(avatar.titleResource()),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(AppResource.String.profile_edit_name_label)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
        }
        item {
            Button(
                onClick = {
                    viewModel.updateIdentity(
                        displayName = name,
                        avatarId = selectedAvatar,
                        onSaved = onBackClick,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(stringResource(AppResource.String.profile_save))
            }
        }
    }
}

@Composable
internal fun ProfileRanksRouteContent(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    ProfileDetailScaffold(
        title = AppResource.String.profile_ranks_title,
        onBackClick = onBackClick,
    ) {
        item {
            Text(
                text = stringResource(AppResource.String.profile_ranks_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        uiState.ranks.forEachIndexed { index, rank ->
            item {
                RankListItem(
                    rank = rank,
                    firstLevel = index * ProfileUiState.LEVELS_PER_RANK + 1,
                    lastLevel = (index + 1) * ProfileUiState.LEVELS_PER_RANK,
                    status =
                        when {
                            index < uiState.rankIndex -> RankStatus.Unlocked
                            index == uiState.rankIndex -> RankStatus.Current
                            else -> RankStatus.Locked
                        },
                )
            }
        }
    }
}

@Composable
private fun RankListItem(
    rank: ProfileUiState.Rank,
    firstLevel: Int,
    lastLevel: Int,
    status: RankStatus,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (status == RankStatus.Current) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (status == RankStatus.Locked) Icons.Filled.Lock else Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(AppResource.String.profile_rank_neutral_format, rank.number),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(AppResource.String.profile_rank_range_format, firstLevel, lastLevel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text =
                    stringResource(
                        when (status) {
                            RankStatus.Current -> AppResource.String.profile_rank_current
                            RankStatus.Unlocked -> AppResource.String.profile_rank_unlocked
                            RankStatus.Locked -> AppResource.String.profile_rank_locked
                        },
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private enum class RankStatus {
    Current,
    Unlocked,
    Locked,
}

@Composable
internal fun ProfileAchievementsRouteContent(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    ProfileDetailScaffold(
        title = AppResource.String.profile_all_achievements,
        onBackClick = onBackClick,
    ) {
        uiState.achievements.forEach { achievement ->
            item { AchievementCard(achievement = achievement) }
        }
    }
}

@Composable
internal fun ProfileTitlesRouteContent(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    ProfileDetailScaffold(
        title = AppResource.String.profile_titles_title,
        onBackClick = onBackClick,
    ) {
        item {
            Text(
                text = stringResource(AppResource.String.profile_titles_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = stringResource(AppResource.String.profile_displayed_title),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text =
                                uiState.selectedTitle?.let { stringResource(it.titleResource()) }
                                    ?: stringResource(AppResource.String.profile_no_titles),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
        uiState.titles.forEach { title ->
            item {
                Card(
                    onClick = { viewModel.selectTitle(title.id) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isUnlocked,
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (title.isUnlocked) Icons.Filled.AutoAwesome else Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(title.id.titleResource()),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (uiState.selectedTitle == title.id) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(AppResource.String.profile_title_selected),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Text(
                                text =
                                    stringResource(
                                        if (title.isUnlocked) {
                                            AppResource.String.profile_select
                                        } else {
                                            AppResource.String.profile_locked
                                        },
                                    ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfilePersonalizationRouteContent(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    ProfileDetailScaffold(
        title = AppResource.String.profile_personalization_title,
        onBackClick = onBackClick,
    ) {
        PersonalizationCategory.entries.forEach { category ->
            item {
                Text(
                    text = stringResource(category.titleResource()),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            uiState.personalization.filter { it.category == category }.forEach { item ->
                item {
                    PersonalizationListItem(
                        item = item,
                        onClick = { viewModel.selectPersonalization(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalizationListItem(
    item: ProfileUiState.PersonalizationItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = item.isUnlocked,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (item.isUnlocked) item.id.icon() else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(item.id.titleResource()),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = personalizationAvailabilityText(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(AppResource.String.profile_unlocked),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun personalizationAvailabilityText(item: ProfileUiState.PersonalizationItem): String {
    if (item.isUnlocked) return stringResource(AppResource.String.profile_unlocked)
    return when (val requirement = item.unlockRequirement) {
        is ProfileUiState.UnlockRequirement.Level ->
            stringResource(AppResource.String.profile_unlock_level_format, requirement.value)

        is ProfileUiState.UnlockRequirement.Trainings ->
            pluralStringResource(
                AppResource.Plural.profile_unlock_training_format,
                requirement.value,
                requirement.value,
            )

        null -> stringResource(AppResource.String.profile_locked)
    }
}

@Composable
internal fun ProfileSettingsRouteContent(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit,
) {
    val settings by viewModel.settingsUiState.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            selectedLanguage = settings.language,
            onLanguageSelect = { language ->
                showLanguageDialog = false
                viewModel.setLanguage(language)
            },
            onDismissRequest = { showLanguageDialog = false },
        )
    }

    ProfileDetailScaffold(
        title = AppResource.String.profile_settings_title,
        onBackClick = onBackClick,
    ) {
        item {
            SettingsValueRow(
                icon = Icons.Filled.Language,
                title = AppResource.String.profile_settings_language,
                value = settings.language.titleResource(),
                onClick = { showLanguageDialog = true },
            )
        }
        item {
            SettingsSwitchRow(
                icon = Icons.Filled.DarkMode,
                title = AppResource.String.profile_settings_theme,
                checked = settings.darkThemeEnabled,
                onCheckedChange = viewModel::setDarkTheme,
            )
        }
        item {
            SettingsSwitchRow(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = AppResource.String.profile_settings_sound,
                checked = settings.soundEnabled,
                onCheckedChange = viewModel::setSoundEnabled,
            )
        }
        item {
            SettingsSwitchRow(
                icon = Icons.Filled.Smartphone,
                title = AppResource.String.profile_settings_vibration,
                checked = settings.vibrationEnabled,
                onCheckedChange = viewModel::setVibrationEnabled,
            )
        }
        item {
            SettingsSwitchRow(
                icon = Icons.Filled.Notifications,
                title = AppResource.String.profile_settings_reminders,
                checked = settings.remindersEnabled,
                onCheckedChange = viewModel::setRemindersEnabled,
            )
        }
        item {
            SettingsInfoRow(
                icon = Icons.Filled.PrivacyTip,
                title = AppResource.String.profile_settings_privacy,
                description = AppResource.String.profile_privacy_description,
            )
        }
        item {
            SettingsInfoRow(
                icon = Icons.Filled.Info,
                title = AppResource.String.profile_settings_about,
                description = AppResource.String.profile_about_description,
            )
        }
    }
}

@Composable
private fun SettingsValueRow(
    icon: ImageVector,
    title: StringResource,
    value: StringResource,
    onClick: () -> Unit,
) {
    SettingsCard(onClick = onClick) {
        ListItem(
            headlineContent = { Text(stringResource(title)) },
            supportingContent = { Text(stringResource(value)) },
            leadingContent = { SettingsIcon(icon) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
    }
}

@Composable
private fun LanguageSelectionDialog(
    selectedLanguage: AppLanguage,
    onLanguageSelect: (AppLanguage) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(AppResource.String.profile_settings_language)) },
        text = {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppLanguage.entries.forEach { language ->
                    ListItem(
                        headlineContent = { Text(stringResource(language.titleResource())) },
                        leadingContent = {
                            RadioButton(
                                selected = language == selectedLanguage,
                                onClick = null,
                            )
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = language == selectedLanguage,
                                    onClick = { onLanguageSelect(language) },
                                    role = Role.RadioButton,
                                ),
                        colors =
                            ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(AppResource.String.profile_settings_language_cancel))
            }
        },
    )
}

private fun AppLanguage.titleResource(): StringResource =
    when (this) {
        AppLanguage.System -> AppResource.String.profile_settings_system_default
        AppLanguage.English -> AppResource.String.profile_settings_language_english
        AppLanguage.Russian -> AppResource.String.profile_settings_language_russian
        AppLanguage.Kazakh -> AppResource.String.profile_settings_language_kazakh
    }

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: StringResource,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsCard {
        ListItem(
            headlineContent = { Text(stringResource(title)) },
            leadingContent = { SettingsIcon(icon) },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                )
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: StringResource,
    description: StringResource,
) {
    SettingsCard {
        ListItem(
            headlineContent = { Text(stringResource(title)) },
            supportingContent = { Text(stringResource(description)) },
            leadingContent = { SettingsIcon(icon) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingsCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val modifier = Modifier.fillMaxWidth()
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            border = border,
            content = { content() },
        )
    } else {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            border = border,
            content = { content() },
        )
    }
}

@Composable
private fun ProfileDetailScaffold(
    title: StringResource,
    onBackClick: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().appBackground()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = stringResource(title),
                onBackClick = onBackClick,
            )
            LazyColumn(
                modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 760.dp).fillMaxWidth(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    }
}
