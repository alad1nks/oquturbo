package com.alad1nks.oquturbo.feature.kenkozgame.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.alad1nks.oquturbo.core.ui.navigation.enumNavType
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import com.alad1nks.oquturbo.feature.kenkozgame.ui.KenKozGameRoute
import com.alad1nks.oquturbo.feature.kenkozgame.ui.KenKozGameViewModel
import com.alad1nks.oquturbo.resources.AppResource
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.reflect.typeOf

@Serializable
data class KenKozGameRoute(
    val mode: KenKozGameMode,
)

private val kenKozGameTypeMap =
    mapOf(typeOf<KenKozGameMode>() to enumNavType<KenKozGameMode>())

fun NavController.navigateToKenKozGame(
    mode: KenKozGameMode,
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(KenKozGameRoute(mode), navOptions)
}

fun NavGraphBuilder.kenKozGameScreen(
    onBackClick: () -> Unit,
) {
    composable<KenKozGameRoute>(typeMap = kenKozGameTypeMap) { entry ->
        val mode = entry.toRoute<KenKozGameRoute>().mode
        val characters = stringResource(AppResource.String.kenkoz_game_characters).map(Char::toString)
        val words = stringArrayResource(AppResource.Array.kenkoz_game_words)
        val differencePairs =
            stringArrayResource(AppResource.Array.kenkoz_game_difference_pairs).map { item ->
                val parts = item.split(',').map(String::trim)
                require(parts.size == 2 && parts.all(String::isNotEmpty)) {
                    "Invalid KenKoz difference pair: $item"
                }
                parts[0] to parts[1]
            }
        val viewModel =
            koinViewModel<KenKozGameViewModel>(
                parameters = { parametersOf(mode, characters, words, differencePairs) },
            )
        KenKozGameRoute(
            viewModel = viewModel,
            onBackClick = onBackClick,
        )
    }
}
