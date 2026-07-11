package com.alad1nks.oquturbo.feature.kenkozgame.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import com.alad1nks.oquturbo.feature.kenkozgame.ui.KenKozGameRoute
import com.alad1nks.oquturbo.feature.kenkozgame.ui.KenKozGameViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class KenKozGameRoute(
    val mode: KenKozGameMode,
)

fun NavController.navigateToKenKozGame(
    mode: KenKozGameMode,
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(KenKozGameRoute(mode), navOptions)
}

fun NavGraphBuilder.kenKozGameScreen(
    onBackClick: () -> Unit,
) {
    composable<KenKozGameRoute> { entry ->
        val mode = entry.toRoute<KenKozGameRoute>().mode
        val viewModel =
            koinViewModel<KenKozGameViewModel>(
                parameters = { parametersOf(mode) },
            )
        KenKozGameRoute(
            viewModel = viewModel,
            onBackClick = onBackClick,
        )
    }
}
