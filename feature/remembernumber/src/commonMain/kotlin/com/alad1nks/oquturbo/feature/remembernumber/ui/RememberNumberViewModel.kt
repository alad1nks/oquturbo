package com.alad1nks.oquturbo.feature.remembernumber.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.data.repository.RememberNumberRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class RememberNumberViewModel(
    val maxLength: Int,
    private val availableDigits: String,
    private val rememberNumberRepository: RememberNumberRepository,
    private val gameActivityRepository: GameActivityRepository,
) : ViewModel() {
    private var score: Int = 0
    private var waitingNumber: String = ""
    private var delay: Long = 1000
    private var isFirstNumber = true
    private var sessionStartMark: TimeMark? = null

    private val _focusEvent = MutableStateFlow<Unit?>(null)
    val focusEvent = _focusEvent.asStateFlow()

    private val _uiState = MutableStateFlow<RememberNumberUiState>(RememberNumberUiState.Initial())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            uiState.collect { uiState ->
                when (uiState) {
                    is RememberNumberUiState.Initial -> {}
                    is RememberNumberUiState.Writing -> {
                        val text = uiState.text
                        if (text.length == maxLength) {
                            if (text == waitingNumber) {
                                waitingNumber = generateNumber()
                                score++
                                _uiState.value =
                                    RememberNumberUiState.Reading(
                                        text = waitingNumber,
                                        score = score,
                                    )
                                delay = (delay * 95) / 100
                            } else {
                                val sessionDurationMillis = finishSessionTelemetry()
                                withContext(NonCancellable) {
                                    val storageRecord =
                                        rememberNumberRepository.getRememberNumberRecord(
                                            maxLength = maxLength,
                                            availableDigits = availableDigits,
                                        ).first() ?: 0
                                    val currentRecord = maxOf(storageRecord, score)
                                    _uiState.value =
                                        RememberNumberUiState.Mistake(
                                            text = text,
                                            score = score,
                                            correctText = waitingNumber,
                                            record = currentRecord,
                                        )
                                    delay = 1000
                                    sessionDurationMillis?.let { durationMillis ->
                                        recordCompletedSession(
                                            score = score,
                                            durationMillis = durationMillis,
                                            isNewRecord = currentRecord > storageRecord,
                                        )
                                    }
                                    if (currentRecord > storageRecord) {
                                        rememberNumberRepository.setRememberNumberRecord(
                                            maxLength,
                                            availableDigits,
                                            score,
                                        )
                                    }
                                }
                            }
                            _focusEvent.value = null
                        }
                    }
                    is RememberNumberUiState.Reading -> {
                        delay(delay.milliseconds)
                        if (isFirstNumber) {
                            delay(700.milliseconds)
                            isFirstNumber = false
                        }
                        _uiState.value =
                            RememberNumberUiState.Writing(
                                text = "",
                                score = uiState.score,
                            )
                        _focusEvent.value = Unit
                    }
                    is RememberNumberUiState.Mistake -> {}
                }
            }
        }
    }

    fun start() {
        viewModelScope.launch {
            score = 0
            isFirstNumber = true
            sessionStartMark = TimeSource.Monotonic.markNow()
            waitingNumber = generateNumber()
            _uiState.value =
                RememberNumberUiState.Reading(
                    text = waitingNumber,
                    score = score,
                )
        }
    }

    fun writeText(value: String) {
        val uiState = uiState.value
        if (uiState is RememberNumberUiState.Writing) {
            _uiState.value = uiState.copy(text = value)
        }
    }

    private fun generateNumber(): String {
        val stringBuilder = StringBuilder()

        repeat(maxLength) {
            stringBuilder.append(availableDigits.random())
        }

        return stringBuilder.toString()
    }

    private suspend fun recordCompletedSession(
        score: Int,
        durationMillis: Long,
        isNewRecord: Boolean,
    ) {
        val (mode, variantId) = sessionMode()
        gameActivityRepository.recordCompletedSession(
            game = GameId.NumberSprint,
            mode = mode,
            variantId = variantId,
            score = score,
            correctAnswers = score,
            durationMillis = durationMillis,
            isNewRecord = isNewRecord,
        )
    }

    private fun finishSessionTelemetry(): Long? {
        val startMark = sessionStartMark ?: return null
        sessionStartMark = null
        return startMark.elapsedNow().inWholeMilliseconds
    }

    private fun sessionMode(): Pair<GameModeId, String?> {
        val digits = normalizedDigits()
        return when {
            maxLength == 4 && digits == ALL_DIGITS -> GameModeId.NumberSprintClassic to null
            maxLength == 4 && digits == BINARY_DIGITS -> GameModeId.NumberSprintBinary to null
            else ->
                GameModeId.NumberSprintCustom to
                    "length:$maxLength;digits:$digits"
        }
    }

    private fun normalizedDigits(): String = availableDigits.toSet().sorted().joinToString(separator = "")

    private companion object {
        const val ALL_DIGITS = "0123456789"
        const val BINARY_DIGITS = "01"
    }
}
