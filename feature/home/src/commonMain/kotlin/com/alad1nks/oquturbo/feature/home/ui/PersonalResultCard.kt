package com.alad1nks.oquturbo.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.data.model.GameSeriesKey
import com.alad1nks.oquturbo.core.data.model.ProgressComparison
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboLayout
import com.alad1nks.oquturbo.core.ui.component.AppCard
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PersonalResultCard(
    state: PersonalResultState,
    onRetryClick: () -> Unit,
    onModeStatisticsClick: (GameSeriesKey) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        var loadedHeight by rememberSaveable { mutableIntStateOf(0) }
        var loadedWidth by rememberSaveable { mutableIntStateOf(0) }
        var loadedDensity by rememberSaveable { mutableFloatStateOf(0f) }
        var loadedFontScale by rememberSaveable { mutableFloatStateOf(0f) }
        // A shorter loading placeholder would clamp a restored LazyColumn offset near its end.
        // Reserve only measured space; no previous result values or actions survive a fresh read.
        val sameLayout =
            loadedWidth == constraints.maxWidth &&
                loadedDensity == density.density && loadedFontScale == density.fontScale
        val minimumHeight =
            if (state == PersonalResultState.Loading && sameLayout) {
                with(density) { loadedHeight.toDp() }
            } else {
                0.dp
            }
        AppCard(
            modifier =
                Modifier.fillMaxWidth().heightIn(min = minimumHeight).onSizeChanged {
                    if (state is PersonalResultState.Loaded) {
                        loadedHeight = it.height
                        loadedWidth = it.width
                        loadedDensity = density.density
                        loadedFontScale = density.fontScale
                    }
                },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(OquTurboLayout.cardInset),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(AppResource.String.home_personal_result_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                when (state) {
                    PersonalResultState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(32.dp))
                        ResultExplanation(AppResource.String.home_personal_result_loading)
                    }
                    PersonalResultState.Error -> {
                        ResultExplanation(AppResource.String.home_personal_result_error)
                        ResultAction(AppResource.String.home_personal_result_retry, onRetryClick)
                    }
                    is PersonalResultState.Loaded ->
                        when (val comparison = state.comparison) {
                            is ProgressComparison.NoRecentSessions -> {
                                ResultExplanation(AppResource.String.home_personal_result_empty)
                                ResultExplanation(AppResource.String.home_personal_result_empty_hint)
                            }
                            is ProgressComparison.InsufficientData -> {
                                ResultSeries(comparison.series)
                                ResultExplanation(AppResource.String.home_personal_result_window)
                                Text(
                                    text =
                                        stringResource(
                                            AppResource.String.home_personal_result_count,
                                            comparison.availableCount,
                                        ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                ResultExplanation(AppResource.String.home_personal_result_insufficient_hint)
                                ResultAction(AppResource.String.home_personal_result_statistics) {
                                    onModeStatisticsClick(comparison.series)
                                }
                            }
                            is ProgressComparison.Compared -> {
                                ResultSeries(comparison.series)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    ResultExplanation(AppResource.String.home_personal_result_window)
                                    ResultExplanation(AppResource.String.home_personal_result_source)
                                }
                                Text(
                                    stringResource(AppResource.String.home_personal_result_median),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                ResultValue(
                                    AppResource.String.home_personal_result_previous,
                                    comparison.previousMedian.toString(),
                                )
                                ResultValue(
                                    AppResource.String.home_personal_result_current,
                                    comparison.currentMedian.toString(),
                                )
                                ResultValue(
                                    AppResource.String.home_personal_result_change,
                                    comparison.absoluteChange.signedResultChange(),
                                )
                                ResultAction(AppResource.String.home_personal_result_statistics) {
                                    onModeStatisticsClick(comparison.series)
                                }
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun ResultSeries(series: GameSeriesKey) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(series.game.toHomeGame().titleResource()),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        ResultExplanation(series.mode.toHomeMode().titleResource())
        when (val metadata = series.personalResultMetadata()) {
            PersonalResultMetadata.None -> Unit
            is PersonalResultMetadata.Custom -> {
                ResultDetail(
                    stringResource(AppResource.String.remember_number_menu_item_custom_dialog_length),
                    metadata.length.toString(),
                )
                ResultDetail(
                    stringResource(AppResource.String.remember_number_menu_item_custom_dialog_available_digits),
                    metadata.digits,
                )
            }
            is PersonalResultMetadata.Language -> ResultExplanation(metadata.resource)
            PersonalResultMetadata.UnknownSettings ->
                ResultExplanation(
                    AppResource.String.home_personal_result_unknown_settings,
                )
            PersonalResultMetadata.UnknownLanguage ->
                ResultExplanation(
                    AppResource.String.home_personal_result_unknown_language,
                )
            PersonalResultMetadata.UnknownVariant ->
                ResultExplanation(
                    AppResource.String.home_personal_result_unknown_variant,
                )
        }
    }
}

@Composable
private fun ResultDetail(label: String, value: String) {
    Text(
        "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ResultExplanation(resource: StringResource) {
    Text(
        stringResource(resource),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ResultValue(label: StringResource, value: String) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ResultExplanation(label)
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ResultAction(label: StringResource, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 56.dp),
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(stringResource(label), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}
