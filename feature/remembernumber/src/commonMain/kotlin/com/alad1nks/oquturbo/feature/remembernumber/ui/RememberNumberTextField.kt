package com.alad1nks.oquturbo.feature.remembernumber.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
internal fun RememberNumberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    maxLength: Int,
    backgroundColor: @Composable (Int) -> Color,
    borderColor: @Composable (Int) -> Color,
    showFocusedPlaceholder: Boolean,
    modifier: Modifier = Modifier,
) {
    var textFieldValue by remember(value) {
        mutableStateOf(
            value =
                TextFieldValue(
                    text = value,
                    selection = TextRange(value.length),
                ),
        )
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newText ->
            if (value.length >= maxLength) {
                return@BasicTextField
            }

            val filtered = newText.text.filter { it.isDigit() }.take(maxLength)
            onValueChange(filtered)
        },
        modifier = modifier,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
        decorationBox = {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val itemsPerRow = if (maxLength <= 5) maxLength else (maxLength + 1) / 2
                val horizontalSpacing = if (maxLength <= 5) 12.dp else 8.dp
                val availableTileWidth =
                    (maxWidth - horizontalSpacing * (itemsPerRow - 1)) / itemsPerRow
                val tileSize = minOf(availableTileWidth, 72.dp)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            space = horizontalSpacing,
                            alignment = Alignment.CenterHorizontally,
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = itemsPerRow,
                ) {
                    repeat(maxLength) { index ->
                        val char = value.getOrNull(index)
                        val isFocused = index == value.length
                        val shape = RoundedCornerShape(16.dp)

                        Box(
                            modifier =
                                Modifier
                                    .size(tileSize)
                                    .clip(shape)
                                    .background(backgroundColor(index))
                                    .border(
                                        width = 2.dp,
                                        color = borderColor(index),
                                        shape = shape,
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (char != null) {
                                Text(
                                    text = char.toString(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else if (isFocused && showFocusedPlaceholder) {
                                FocusedPlaceHolder()
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun FocusedPlaceHolder(modifier: Modifier = Modifier) {
    val animateFloatAsState by animateFloatAsState(
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = 1000
                        0f at 0 using LinearEasing
                        1f at 500 using LinearEasing
                        0f at 1000 using LinearEasing
                    },
                repeatMode = RepeatMode.Reverse,
            ),
    )

    AnimatedVisibility(
        visible = animateFloatAsState > 0.5f,
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier
                    .width(2.dp)
                    .height(32.dp)
                    .background(MaterialTheme.colorScheme.onSurface),
        )
    }
}
