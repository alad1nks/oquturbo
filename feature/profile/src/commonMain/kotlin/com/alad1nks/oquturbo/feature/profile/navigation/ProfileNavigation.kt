package com.alad1nks.oquturbo.feature.profile.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.profile.ui.ProfileAchievementsRouteContent
import com.alad1nks.oquturbo.feature.profile.ui.ProfileEditRouteContent
import com.alad1nks.oquturbo.feature.profile.ui.ProfilePersonalizationRouteContent
import com.alad1nks.oquturbo.feature.profile.ui.ProfileRanksRouteContent
import com.alad1nks.oquturbo.feature.profile.ui.ProfileRouteContent
import com.alad1nks.oquturbo.feature.profile.ui.ProfileSettingsRouteContent
import com.alad1nks.oquturbo.feature.profile.ui.ProfileTitlesRouteContent
import com.alad1nks.oquturbo.feature.profile.ui.ProfileViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object ProfileRoute

@Serializable data object EditProfileRoute

@Serializable data object ProfileRanksRoute

@Serializable data object ProfileAchievementsRoute

@Serializable data object ProfileTitlesRoute

@Serializable data object ProfilePersonalizationRoute

@Serializable data object ProfileSettingsRoute

fun NavController.navigateToProfile(navOptions: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route = ProfileRoute, builder = navOptions)
}

fun NavController.navigateToEditProfile() = navigate(EditProfileRoute)

fun NavController.navigateToProfileRanks() = navigate(ProfileRanksRoute)

fun NavController.navigateToProfileAchievements() = navigate(ProfileAchievementsRoute)

fun NavController.navigateToProfileTitles() = navigate(ProfileTitlesRoute)

fun NavController.navigateToProfilePersonalization() = navigate(ProfilePersonalizationRoute)

fun NavController.navigateToProfileSettings() = navigate(ProfileSettingsRoute)

fun NavGraphBuilder.profileScreen(
    onEditProfileClick: () -> Unit,
    onRanksClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onTitlesClick: () -> Unit,
    onPersonalizationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit,
) {
    composable<ProfileRoute> {
        val viewModel = koinViewModel<ProfileViewModel>()
        ProfileRouteContent(
            viewModel = viewModel,
            onEditProfileClick = onEditProfileClick,
            onRanksClick = onRanksClick,
            onAchievementsClick = onAchievementsClick,
            onTitlesClick = onTitlesClick,
            onPersonalizationClick = onPersonalizationClick,
            onSettingsClick = onSettingsClick,
            onStatsClick = onStatsClick,
        )
    }
}

fun NavGraphBuilder.profileEditScreen(onBackClick: () -> Unit) {
    composable<EditProfileRoute> {
        ProfileEditRouteContent(
            viewModel = koinViewModel(),
            onBackClick = onBackClick,
        )
    }
}

fun NavGraphBuilder.profileRanksScreen(onBackClick: () -> Unit) {
    composable<ProfileRanksRoute> {
        ProfileRanksRouteContent(
            viewModel = koinViewModel(),
            onBackClick = onBackClick,
        )
    }
}

fun NavGraphBuilder.profileAchievementsScreen(onBackClick: () -> Unit) {
    composable<ProfileAchievementsRoute> {
        ProfileAchievementsRouteContent(
            viewModel = koinViewModel(),
            onBackClick = onBackClick,
        )
    }
}

fun NavGraphBuilder.profileTitlesScreen(onBackClick: () -> Unit) {
    composable<ProfileTitlesRoute> {
        ProfileTitlesRouteContent(
            viewModel = koinViewModel(),
            onBackClick = onBackClick,
        )
    }
}

fun NavGraphBuilder.profilePersonalizationScreen(onBackClick: () -> Unit) {
    composable<ProfilePersonalizationRoute> {
        ProfilePersonalizationRouteContent(
            viewModel = koinViewModel(),
            onBackClick = onBackClick,
        )
    }
}

fun NavGraphBuilder.profileSettingsScreen(onBackClick: () -> Unit) {
    composable<ProfileSettingsRoute> {
        ProfileSettingsRouteContent(
            viewModel = koinViewModel(),
            onBackClick = onBackClick,
        )
    }
}
