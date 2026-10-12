package com.alad1nks.oquturbo.feature.profile.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import com.alad1nks.oquturbo.core.data.reminders.ReminderActionObserver
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

@Composable
internal fun rememberReminderActionObservation(
    observer: ReminderActionObserver?,
    scrollObservation: () -> Map<String, String>,
): ReminderActionObservation? {
    if (observer == null) return null
    val latestObserver = rememberUpdatedState(observer)
    val latestScroll = rememberUpdatedState(scrollObservation)
    val observation = remember { ReminderActionObservation({ latestObserver.value }, { latestScroll.value() }) }
    observation.density = LocalDensity.current.density
    LaunchedEffect(observation) {
        observation.source.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> observation.press(interaction)
                is PressInteraction.Release -> observation.finish(interaction.press, "picker-ui-release")
                is PressInteraction.Cancel -> observation.finish(interaction.press, "picker-ui-cancel")
            }
        }
    }
    LaunchedEffect(observation) { observation.sampleLayouts() }
    return observation
}

/** Plain diagnostic fields: layout observations must never cause recomposition or change input handling. */
internal class ReminderActionObservation(
    private val observer: () -> ReminderActionObserver,
    private val scrollObservation: () -> Map<String, String>,
) {
    val source = MutableInteractionSource()
    var bounds = Rect.Zero
    var density = 1f
    private val identity = Random.nextLong().toString()
    private var nextPress = 0
    private val presses = mutableMapOf<PressInteraction.Press, Int>()
    private var remainingEvents = 128
    private var layoutSamples = 0
    private var lastLayout: Map<String, String>? = null
    private var layoutTruncated = false

    suspend fun sampleLayouts() {
        while (currentCoroutineContext().isActive && !layoutTruncated) {
            sampleLayout()
            delay(250)
        }
    }

    fun sampleLayout() {
        // Independent of the input-event budget: layout evidence must never suppress a press or click.
        if (layoutTruncated) return
        if (layoutSamples == 256) {
            layoutTruncated = true
            runCatching {
                observer().record(
                    "picker-ui-layout-truncated",
                    mapOf("observation" to identity, "samples" to layoutSamples.toString()),
                )
            }
            return
        }
        runCatching {
            val fields = currentFields()
            if (fields == lastLayout) return@runCatching
            lastLayout = fields
            layoutSamples++
            observer().record("picker-ui-layout", fields + ("sample" to layoutSamples.toString()))
        }
    }

    fun press(press: PressInteraction.Press) {
        if (remainingEvents == 0) return
        val id = ++nextPress
        presses[press] = id
        record(
            "picker-ui-press",
            mapOf(
                "press" to id.toString(),
                "localX" to press.pressPosition.x.toString(),
                "localY" to press.pressPosition.y.toString(),
            ),
        )
    }

    fun finish(press: PressInteraction.Press, event: String) {
        val id = presses.remove(press)
        record(event, mapOf("press" to (id?.toString() ?: "unobserved")))
    }

    fun record(event: String, fields: Map<String, String> = emptyMap()) {
        if (remainingEvents == 0) return
        remainingEvents--
        // No real callback is inside this guard. Diagnostic providers and sinks may fail independently.
        runCatching {
            observer().record(
                event,
                currentFields() + fields,
            )
        }
    }

    private fun currentFields(): Map<String, String> =
        mapOf(
            "observation" to identity,
            "density" to density.toString(),
            "left" to bounds.left.toString(),
            "top" to bounds.top.toString(),
            "right" to bounds.right.toString(),
            "bottom" to bounds.bottom.toString(),
        ) + scrollObservation()
}

internal fun ReminderActionObservation?.actionBounds(): Modifier =
    if (this == null) Modifier else Modifier.onGloballyPositioned { bounds = it.boundsInWindow() }
