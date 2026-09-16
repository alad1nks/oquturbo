package com.alad1nks.oquturbo.feature.ruleswitch.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.alad1nks.oquturbo.feature.ruleswitch.ui.RuleSwitchRoute
import com.alad1nks.oquturbo.feature.ruleswitch.ui.RuleSwitchViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object RuleSwitchRoute

fun NavController.navigateToRuleSwitch(
    navOptions: NavOptionsBuilder.() -> Unit = {
    },
) = navigate(RuleSwitchRoute, navOptions)

fun NavGraphBuilder.ruleSwitchScreen(onBackClick: (() -> Unit)? = null) {
    composable<RuleSwitchRoute> { RuleSwitchRoute(koinViewModel<RuleSwitchViewModel>(), onBackClick) }
}
