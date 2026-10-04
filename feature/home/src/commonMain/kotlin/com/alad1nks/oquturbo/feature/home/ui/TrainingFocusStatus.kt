package com.alad1nks.oquturbo.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusPhase
import com.alad1nks.oquturbo.core.data.practice.practiceDate
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TrainingFocusStatus(state: HomeFocusState) {
    if (state is HomeFocusState.Ready && state.focus.selection == null) return
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        var height by rememberSaveable { mutableIntStateOf(0) }
        var width by rememberSaveable { mutableIntStateOf(0) }
        var scale by rememberSaveable { mutableFloatStateOf(0f) }
        var fontScale by rememberSaveable { mutableFloatStateOf(0f) }
        val reserve =
            state == HomeFocusState.Loading && width == constraints.maxWidth &&
                scale == density.density && fontScale == density.fontScale
        Column(
            Modifier.fillMaxWidth().heightIn(min = if (reserve) with(density) { height.toDp() } else 0.dp)
                .onSizeChanged {
                    if (state is HomeFocusState.Ready) {
                        height = it.height
                        width = it.width
                        scale = density.density
                        fontScale = density.fontScale
                    }
                },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val color = MaterialTheme.colorScheme.onPrimaryContainer
            Text(
                stringResource(AppResource.String.focus_setting_title),
                style = MaterialTheme.typography.bodyMedium,
                color = color,
            )
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                when (state) {
                    HomeFocusState.Loading -> Text(stringResource(AppResource.String.focus_loading), color = color)
                    HomeFocusState.Error -> {
                        Text(stringResource(AppResource.String.focus_error), color = color)
                        Text(
                            stringResource(AppResource.String.focus_home_recovery),
                            style = MaterialTheme.typography.bodyMedium,
                            color = color,
                        )
                    }
                    is HomeFocusState.Ready -> {
                        val selection = requireNotNull(state.focus.selection)
                        Text(
                            stringResource(
                                when (state.focus.phaseOn(state.todayEpochDay)) {
                                    WeeklyFocusPhase.Scheduled -> AppResource.String.focus_scheduled
                                    WeeklyFocusPhase.Active -> AppResource.String.focus_active
                                    WeeklyFocusPhase.Expired -> AppResource.String.focus_expired
                                    WeeklyFocusPhase.Off -> error("Off has no subsection")
                                },
                            ),
                            color = color,
                        )
                        Text(stringResource(HomeUiState.Game.NumberSprint.titleResource()), color = color)
                        Text(stringResource(HomeUiState.Mode.Classic.titleResource()), color = color)
                        Text(
                            stringResource(
                                AppResource.String.focus_saved_dates,
                                focusDate(selection.startEpochDay),
                                focusDate(selection.lastEpochDay),
                            ),
                            color = color,
                        )
                        Text(
                            stringResource(AppResource.String.focus_home_scope),
                            style = MaterialTheme.typography.bodyMedium,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun focusDate(day: Long): String =
    practiceDate(day).let {
        stringResource(AppResource.String.practice_date_format, it.yearText, it.monthText, it.dayText)
    }
