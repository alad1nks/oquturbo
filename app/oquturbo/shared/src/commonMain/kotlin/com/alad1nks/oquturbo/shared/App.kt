package com.alad1nks.oquturbo.shared

import androidx.compose.runtime.Composable
import com.alad1nks.oquturbo.feature.baspagame.navigation.baspaGameScreen
import com.alad1nks.oquturbo.feature.baspagame.navigation.navigateToBaspaGame
import com.alad1nks.oquturbo.feature.baspagamemenu.navigation.BaspaGameMenuRoute
import com.alad1nks.oquturbo.feature.baspagamemenu.navigation.baspaGameMenuScreen
import com.alad1nks.oquturbo.feature.baspagamemenu.navigation.navigateToBaspaGameMenu
import com.alad1nks.oquturbo.feature.games.model.TrainingGame
import com.alad1nks.oquturbo.feature.games.navigation.GamesRoute
import com.alad1nks.oquturbo.feature.games.navigation.gamesScreen
import com.alad1nks.oquturbo.feature.home.navigation.HomeRoute
import com.alad1nks.oquturbo.feature.home.navigation.homeScreen
import com.alad1nks.oquturbo.feature.kenkozgame.navigation.kenKozGameScreen
import com.alad1nks.oquturbo.feature.kenkozgame.navigation.navigateToKenKozGame
import com.alad1nks.oquturbo.feature.kenkozgamemenu.navigation.KenKozGameMenuRoute
import com.alad1nks.oquturbo.feature.kenkozgamemenu.navigation.kenKozGameMenuScreen
import com.alad1nks.oquturbo.feature.kenkozgamemenu.navigation.navigateToKenKozGameMenu
import com.alad1nks.oquturbo.feature.main.ui.MainScreen
import com.alad1nks.oquturbo.feature.profile.navigation.navigateToEditProfile
import com.alad1nks.oquturbo.feature.profile.navigation.navigateToProfileAchievements
import com.alad1nks.oquturbo.feature.profile.navigation.navigateToProfilePersonalization
import com.alad1nks.oquturbo.feature.profile.navigation.navigateToProfileRanks
import com.alad1nks.oquturbo.feature.profile.navigation.navigateToProfileSettings
import com.alad1nks.oquturbo.feature.profile.navigation.navigateToProfileTitles
import com.alad1nks.oquturbo.feature.profile.navigation.profileAchievementsScreen
import com.alad1nks.oquturbo.feature.profile.navigation.profileEditScreen
import com.alad1nks.oquturbo.feature.profile.navigation.profilePersonalizationScreen
import com.alad1nks.oquturbo.feature.profile.navigation.profileRanksScreen
import com.alad1nks.oquturbo.feature.profile.navigation.profileScreen
import com.alad1nks.oquturbo.feature.profile.navigation.profileSettingsScreen
import com.alad1nks.oquturbo.feature.profile.navigation.profileTitlesScreen
import com.alad1nks.oquturbo.feature.remembernumber.navigation.navigateToRememberNumber
import com.alad1nks.oquturbo.feature.remembernumber.navigation.rememberNumberScreen
import com.alad1nks.oquturbo.feature.remembernumbermenu.navigation.RememberNumberMenuRoute
import com.alad1nks.oquturbo.feature.remembernumbermenu.navigation.navigateToRememberNumberMenu
import com.alad1nks.oquturbo.feature.remembernumbermenu.navigation.rememberNumberMenuScreen
import com.alad1nks.oquturbo.feature.stats.navigation.navigateToStatsGame
import com.alad1nks.oquturbo.feature.stats.navigation.navigateToStatsMode
import com.alad1nks.oquturbo.feature.stats.navigation.statsGameDetailScreen
import com.alad1nks.oquturbo.feature.stats.navigation.statsModeDetailScreen
import com.alad1nks.oquturbo.feature.stats.navigation.statsScreen
import com.alad1nks.oquturbo.shared.navigation.OquTurboTopLevelDestination
import com.alad1nks.oquturbo.shared.ui.OquTurboNavigationBar
import com.alad1nks.oquturbo.shared.ui.rememberOquTurboAppState

@Composable
fun App() {
    val appState = rememberOquTurboAppState()

    MainScreen(
        commonModules = getCommonModules(),
        platformModules = getPlatformModules(),
        startDestination = HomeRoute,
        navController = appState.navController,
        bottomBar = {
            if (appState.shouldShowNavigationBar) {
                OquTurboNavigationBar(
                    destinations = appState.topLevelDestinations,
                    currentDestination = appState.currentDestination,
                    onNavigateToDestination = appState::navigateToTopLevelDestination,
                )
            }
        },
    ) {
        homeScreen(
            onStartTrainingClick = {
                appState.navigateToTopLevelDestination(OquTurboTopLevelDestination.GAMES)
            },
        )
        gamesScreen(
            onGameClick = { game ->
                when (game) {
                    TrainingGame.NumberSprint -> appState.navController.navigateToRememberNumberMenu()
                    TrainingGame.WideEye -> appState.navController.navigateToKenKozGameMenu()
                    TrainingGame.DontTap -> appState.navController.navigateToBaspaGameMenu()
                }
            },
        )
        rememberNumberMenuScreen(
            showBackButton = true,
            onBackClick = {
                appState.navController.popBackStack(route = GamesRoute, inclusive = false)
            },
            onPlayClick = appState.navController::navigateToRememberNumber,
        )
        kenKozGameMenuScreen(
            onModeClick = appState.navController::navigateToKenKozGame,
            showBackButton = true,
            onBackClick = {
                appState.navController.popBackStack(route = GamesRoute, inclusive = false)
            },
        )
        baspaGameMenuScreen(
            onModeClick = appState.navController::navigateToBaspaGame,
            showBackButton = true,
            onBackClick = {
                appState.navController.popBackStack(route = GamesRoute, inclusive = false)
            },
        )
        statsScreen(
            onGamesClick = {
                appState.navigateToTopLevelDestination(OquTurboTopLevelDestination.GAMES)
            },
            onGameClick = { game, period -> appState.navController.navigateToStatsGame(game, period) },
            onActivityClick = { game, mode, period ->
                if (mode == null) {
                    appState.navController.navigateToStatsGame(game, period)
                } else {
                    appState.navController.navigateToStatsMode(game, mode, period)
                }
            },
        )
        statsGameDetailScreen(
            onBackClick = { appState.navController.popBackStack() },
            onModeClick = { game, mode, period ->
                appState.navController.navigateToStatsMode(game, mode, period)
            },
        )
        statsModeDetailScreen(onBackClick = { appState.navController.popBackStack() })
        profileScreen(
            onEditProfileClick = appState.navController::navigateToEditProfile,
            onRanksClick = appState.navController::navigateToProfileRanks,
            onAchievementsClick = appState.navController::navigateToProfileAchievements,
            onTitlesClick = appState.navController::navigateToProfileTitles,
            onPersonalizationClick = appState.navController::navigateToProfilePersonalization,
            onSettingsClick = appState.navController::navigateToProfileSettings,
            onStatsClick = {
                appState.navigateToTopLevelDestination(OquTurboTopLevelDestination.STATS)
            },
        )
        profileEditScreen(onBackClick = { appState.navController.popBackStack() })
        profileRanksScreen(onBackClick = { appState.navController.popBackStack() })
        profileAchievementsScreen(onBackClick = { appState.navController.popBackStack() })
        profileTitlesScreen(onBackClick = { appState.navController.popBackStack() })
        profilePersonalizationScreen(onBackClick = { appState.navController.popBackStack() })
        profileSettingsScreen(onBackClick = { appState.navController.popBackStack() })
        rememberNumberScreen {
            appState.navController.popBackStack(route = RememberNumberMenuRoute, inclusive = false)
        }
        kenKozGameScreen {
            appState.navController.popBackStack(route = KenKozGameMenuRoute, inclusive = false)
        }
        baspaGameScreen {
            appState.navController.popBackStack(route = BaspaGameMenuRoute, inclusive = false)
        }
    }
}
