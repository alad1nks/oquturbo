package com.alad1nks.oquturbo.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.PageHeader
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileRouteContent(
    viewModel: ProfileViewModel,
    onEditProfileClick: () -> Unit,
    onRanksClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onTitlesClick: () -> Unit,
    onPersonalizationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    ProfileScreen(
        uiState = uiState,
        onEditProfileClick = onEditProfileClick,
        onRanksClick = onRanksClick,
        onAchievementsClick = onAchievementsClick,
        onTitlesClick = onTitlesClick,
        onPersonalizationClick = onPersonalizationClick,
        onSettingsClick = onSettingsClick,
        onStatsClick = onStatsClick,
        modifier = modifier,
    )
}

@Composable
internal fun ProfileScreen(
    uiState: ProfileUiState,
    onEditProfileClick: () -> Unit,
    onRanksClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onTitlesClick: () -> Unit,
    onPersonalizationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().appBackground()) {
        LazyColumn(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .statusBarsPadding(),
            contentPadding = PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                ProfileHeader(onSettingsClick = onSettingsClick)
            }
            if (uiState.showNewRankBanner) {
                item { NewRankBanner() }
            }
            item {
                ProfileHeroCard(
                    uiState = uiState,
                    onEditProfileClick = onEditProfileClick,
                    onRanksClick = onRanksClick,
                )
            }
            if (!uiState.hasGameActivity) {
                item { NewUserHint() }
            }
            item {
                ProfileSummarySection(
                    uiState = uiState,
                    onStatsClick = onStatsClick,
                    onAchievementsClick = onAchievementsClick,
                )
            }
            item {
                ProfileAchievementsSection(
                    achievements = uiState.achievements,
                    onAllAchievementsClick = onAchievementsClick,
                )
            }
            item {
                ProfileTitleSection(
                    selectedTitle = uiState.selectedTitle,
                    onClick = onTitlesClick,
                )
            }
            item {
                ProfilePersonalizationSection(
                    items = uiState.personalization,
                    onClick = onPersonalizationClick,
                )
            }
            item {
                ProfileRecentlyUnlockedSection(recentUnlocks = uiState.recentUnlocks)
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        PageHeader(
            title = stringResource(AppResource.String.profile_title),
            modifier = Modifier.weight(1f),
        )
        FilledTonalIconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(AppResource.String.profile_settings_content_description),
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ProfilePreviewContent(
    uiState: ProfileUiState,
    darkTheme: Boolean = false,
) {
    OquTurboTheme(darkTheme = darkTheme) {
        ProfileScreen(
            uiState = uiState,
            onEditProfileClick = {},
            onRanksClick = {},
            onAchievementsClick = {},
            onTitlesClick = {},
            onPersonalizationClick = {},
            onSettingsClick = {},
            onStatsClick = {},
        )
    }
}

@Preview(name = "New user")
@Composable
private fun ProfileNewUserPreview() {
    ProfilePreviewContent(uiState = ProfileDemoData.newUser)
}

@Preview(
    name = "Mid rank tablet",
    widthDp = 800,
    heightDp = 1100,
)
@Composable
private fun ProfileMidRankPreview() {
    ProfilePreviewContent(uiState = ProfileDemoData.midRank)
}

@Preview(name = "Near next rank")
@Composable
private fun ProfileNearRankPreview() {
    ProfilePreviewContent(uiState = ProfileDemoData.nearNextRank)
}

@Preview(name = "New rank")
@Composable
private fun ProfileNewRankPreview() {
    ProfilePreviewContent(uiState = ProfileDemoData.newRank)
}

@Preview(name = "Mixed achievements")
@Composable
private fun ProfileAchievementsPreview() {
    ProfilePreviewContent(uiState = ProfileDemoData.mixedAchievements)
}

@Preview(name = "Starter title only")
@Composable
private fun ProfileStarterTitlePreview() {
    ProfilePreviewContent(uiState = ProfileDemoData.noExtraTitles)
}

@Preview(
    name = "Customized compact dark",
    widthDp = 320,
    heightDp = 760,
)
@Composable
private fun ProfileCustomizedDarkPreview() {
    ProfilePreviewContent(
        uiState = ProfileDemoData.customized,
        darkTheme = true,
    )
}
