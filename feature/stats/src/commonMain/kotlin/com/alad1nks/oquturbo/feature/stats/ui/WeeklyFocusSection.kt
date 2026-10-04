package com.alad1nks.oquturbo.feature.stats.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusPhase
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusSelection
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WeeklyFocusSection(
    state: WeeklyFocusUiState,
    today: Long,
    onSelect: () -> Unit,
    onDisable: () -> Unit,
    onRetry: () -> Unit,
    onReset: () -> Unit,
) {
    val game = stringResource(StatsGame.NumberSprint.titleResource())
    val mode = stringResource(StatsMode.Classic.titleResource())
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(AppResource.String.focus_optional_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FocusText(game)
            FocusText(mode)
        }
        when (state) {
            WeeklyFocusUiState.Loading -> FocusLoading(AppResource.String.focus_loading)
            WeeklyFocusUiState.Saving -> FocusLoading(AppResource.String.focus_saving)
            WeeklyFocusUiState.Checking -> {
                FocusText(AppResource.String.focus_save_unconfirmed)
                FocusLoading(AppResource.String.focus_checking)
            }
            WeeklyFocusUiState.Error, WeeklyFocusUiState.Unconfirmed -> {
                val unconfirmed = state == WeeklyFocusUiState.Unconfirmed
                FocusText(
                    if (unconfirmed) AppResource.String.focus_save_unconfirmed else AppResource.String.focus_error,
                )
                FocusAction(
                    if (unconfirmed) AppResource.String.focus_check else AppResource.String.focus_retry,
                    onRetry,
                )
                FocusText(AppResource.String.focus_reset_explanation)
                FocusAction(AppResource.String.focus_reset, onReset)
            }
            is WeeklyFocusUiState.Ready -> {
                val phase = state.focus.phaseOn(today)
                if (phase != WeeklyFocusPhase.Off) {
                    Text(
                        stringResource(
                            when (phase) {
                                WeeklyFocusPhase.Scheduled -> AppResource.String.focus_scheduled
                                WeeklyFocusPhase.Active -> AppResource.String.focus_active
                                WeeklyFocusPhase.Expired -> AppResource.String.focus_expired
                                WeeklyFocusPhase.Off -> error("Off has no saved period")
                            },
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val selection = requireNotNull(state.focus.selection)
                    FocusText(
                        stringResource(
                            if (phase == WeeklyFocusPhase.Expired) {
                                AppResource.String.focus_finished_dates
                            } else {
                                AppResource.String.focus_saved_dates
                            },
                            weeklyDate(selection.startEpochDay),
                            weeklyDate(selection.lastEpochDay),
                        ),
                    )
                }
                if (phase != WeeklyFocusPhase.Expired) {
                    FocusText(stringResource(AppResource.String.focus_new_plans, "$game · $mode"))
                }
                FocusText(AppResource.String.focus_today_preserved)
                if (phase == WeeklyFocusPhase.Off || phase == WeeklyFocusPhase.Expired) {
                    val preview = WeeklyFocusSelection.after(today)
                    FocusText(
                        stringResource(
                            AppResource.String.focus_preview_dates,
                            weeklyDate(preview.startEpochDay),
                            weeklyDate(preview.lastEpochDay),
                        ),
                    )
                    FocusText(AppResource.String.focus_dates_confirmation)
                    FocusAction(
                        if (phase == WeeklyFocusPhase.Off) {
                            AppResource.String.focus_select
                        } else {
                            AppResource.String.focus_reselect
                        },
                        onSelect,
                    )
                } else {
                    FocusAction(
                        if (phase == WeeklyFocusPhase.Scheduled) {
                            AppResource.String.focus_cancel
                        } else {
                            AppResource.String.focus_disable
                        },
                        onDisable,
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusText(resource: StringResource) = FocusText(stringResource(resource))

@Composable
private fun FocusText(value: String) {
    Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FocusLoading(label: StringResource) {
    CircularProgressIndicator(Modifier.size(32.dp))
    FocusText(label)
}

@Composable
private fun FocusAction(label: StringResource, onClick: () -> Unit) {
    TextButton(
        onClick,
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            stringResource(label),
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}
