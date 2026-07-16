package com.alad1nks.oquturbo.feature.remembernumbermenu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RememberNumberMenuCustomOptionsDialog(
    digitsAvailability: List<Boolean>,
    onDigitSelect: (Int, Boolean) -> Unit,
    length: Int,
    onLengthChange: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    onPlayClick: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
    lengthMin: Int = 2,
    lengthMax: Int = 10,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Surface(
            modifier =
                Modifier
                    .padding(16.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = stringResource(AppResource.String.remember_number_menu_item_custom_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text =
                            stringResource(
                                resource = AppResource.String.remember_number_menu_item_custom_dialog_available_digits,
                            ),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                space = 8.dp,
                                alignment = Alignment.CenterHorizontally,
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        digitsAvailability.forEachIndexed { index, isAvailable ->
                            FilterChip(
                                selected = isAvailable,
                                onClick = { onDigitSelect(index, !isAvailable) },
                                label = { Text(text = index.toString()) },
                                leadingIcon =
                                    if (isAvailable) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                            )
                                        }
                                    } else {
                                        null
                                    },
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(AppResource.String.remember_number_menu_item_custom_dialog_length),
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            Text(
                                text = length.toString(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Slider(
                        value = length.toFloat(),
                        onValueChange = { onLengthChange(it.toInt()) },
                        valueRange = lengthMin.toFloat()..lengthMax.toFloat(),
                        steps = lengthMax - lengthMin - 1,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = lengthMin.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = lengthMax.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                    ) {
                        Text(
                            text =
                                stringResource(
                                    resource = AppResource.String.remember_number_menu_item_custom_dialog_cancel_button,
                                ),
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        enabled = digitsAvailability.count { it } > 1,
                        onClick = {
                            onDismissRequest()
                            onPlayClick(length, digitsAvailability.toAvailableDigits())
                        },
                    ) {
                        Text(
                            text =
                                stringResource(
                                    resource = AppResource.String.remember_number_menu_item_custom_dialog_play_button,
                                ),
                        )
                    }
                }
            }
        }
    }
}

private fun List<Boolean>.toAvailableDigits(): String {
    val builder = StringBuilder()

    this.forEachIndexed { index, isAvailable ->
        if (isAvailable) builder.append(index)
    }

    return builder.toString()
}

@Preview
@Composable
private fun RememberNumberMenuCustomOptionsDialogPreview() {
    var maxLength by remember { mutableIntStateOf(4) }
    val digitsAvailability = remember { SnapshotStateList(10) { true } }

    OquTurboTheme {
        RememberNumberMenuCustomOptionsDialog(
            length = maxLength,
            onLengthChange = { maxLength = it },
            digitsAvailability = digitsAvailability,
            onDigitSelect = { index, isAvailable -> digitsAvailability[index] = isAvailable },
            onDismissRequest = {},
            onPlayClick = { _, _ -> },
        )
    }
}
