package com.alad1nks.oquturbo.feature.remembernumber.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameSession
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.data.repository.RememberNumberRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class RememberNumberViewModel(
    val maxLength: Int,
    private val availableDigits: String,
    private val trainingEntryId: String?,
    val trainingRequiredScore: Int?,
    private val rememberNumberRepository: RememberNumberRepository,
    private val gameActivityRepository: GameActivityRepository,
    private val dailyTrainingRepository: DailyTrainingRepository,
) : ViewModel() {
    private var score: Int = 0
    private var waitingNumber: String = ""
    private var delay: Long = 1000
    private var isFirstNumber = true
    private var sessionStartMark: TimeMark? = null
    private var nextTrainingEntry: DailyTrainingEntry? = null
    private var hasContinuedTraining = false
    private var currentAttemptId = 0L

    private val _focusEvent = MutableStateFlow<Unit?>(null)
    val focusEvent = _focusEvent.asStateFlow()

    private val _record = MutableStateFlow(0)
    val record = _record.asStateFlow()

    private val _uiState = MutableStateFlow<RememberNumberUiState>(RememberNumberUiState.Initial())
    val uiState = _uiState.asStateFlow()

    init {
        require((trainingEntryId == null) == (trainingRequiredScore == null)) {
            "Number Sprint training id and required score must be provided together"
        }
        require(trainingRequiredScore == null || trainingRequiredScore > 0) {
            "Number Sprint training required score must be positive"
        }
        viewModelScope.launch {
            _record.value =
                rememberNumberRepository.getRememberNumberRecord(
                    maxLength = maxLength,
                    availableDigits = availableDigits,
                ).first() ?: 0
        }
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
                                val completedAttemptId = currentAttemptId
                                val completedScore = score
                                val correctText = waitingNumber
                                val sessionDurationMillis = finishSessionTelemetry()
                                delay = 1000
                                val storageRecord =
                                    rememberNumberRepository.getRememberNumberRecord(
                                        maxLength = maxLength,
                                        availableDigits = availableDigits,
                                    ).first() ?: 0
                                val currentRecord = maxOf(storageRecord, completedScore)
                                _record.value = maxOf(_record.value, currentRecord)
                                if (completedAttemptId == currentAttemptId) {
                                    _uiState.value =
                                        RememberNumberUiState.Mistake(
                                            text = text,
                                            score = completedScore,
                                            correctText = correctText,
                                            record = currentRecord,
                                            isTrainingResultReady = trainingEntryId == null,
                                        )
                                }
                                viewModelScope.launch {
                                    withContext(NonCancellable) {
                                        sessionDurationMillis?.let { durationMillis ->
                                            val recordedSession =
                                                recordCompletedSession(
                                                    score = completedScore,
                                                    durationMillis = durationMillis,
                                                    isNewRecord = currentRecord > storageRecord,
                                                )
                                            updateCompletedAttempt(completedAttemptId) {
                                                it.copy(isNewRecord = recordedSession.isNewRecord)
                                            }
                                            trainingEntryId?.let { entryId ->
                                                val trainingPlan =
                                                    dailyTrainingRepository.completeEntry(
                                                        entryId = entryId,
                                                        score = completedScore,
                                                    )
                                                if (completedAttemptId == currentAttemptId) {
                                                    nextTrainingEntry = trainingPlan.nextEntry
                                                }
                                                updateCompletedAttempt(completedAttemptId) {
                                                    it.copy(isTrainingResultReady = true)
                                                }
                                            }
                                        }
                                        if (currentRecord > storageRecord) {
                                            rememberNumberRepository.setRememberNumberRecord(
                                                maxLength,
                                                availableDigits,
                                                completedScore,
                                            )
                                        }
                                    }
                                }
                            }
                            _focusEvent.value = null
                        }
                    }
                    is RememberNumberUiState.Reading -> {
                        val readingAttemptId = currentAttemptId
                        delay(delay.milliseconds)
                        if (isFirstNumber) {
                            delay(700.milliseconds)
                            isFirstNumber = false
                        }
                        if (readingAttemptId != currentAttemptId || _uiState.value != uiState) return@collect
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
        currentAttemptId++
        score = 0
        delay = 1000
        isFirstNumber = true
        nextTrainingEntry = null
        hasContinuedTraining = false
        sessionStartMark = TimeSource.Monotonic.markNow()
        waitingNumber = generateNumber()
        _focusEvent.value = null
        _uiState.value =
            RememberNumberUiState.Reading(
                text = waitingNumber,
                score = score,
            )
    }

    fun writeText(value: String) {
        val uiState = uiState.value
        if (uiState is RememberNumberUiState.Writing) {
            _uiState.value = uiState.copy(text = value)
        }
    }

    fun continueTraining(onContinue: (DailyTrainingEntry?) -> Unit) {
        val mistake = _uiState.value as? RememberNumberUiState.Mistake ?: return
        val requiredScore = trainingRequiredScore ?: return
        if (!mistake.isTrainingResultReady || mistake.score < requiredScore || hasContinuedTraining) return

        hasContinuedTraining = true
        onContinue(nextTrainingEntry)
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
    ): GameSession {
        val (mode, variantId) = sessionMode()
        return gameActivityRepository.recordCompletedSession(
            game = GameId.NumberSprint,
            mode = mode,
            variantId = variantId,
            score = score,
            correctAnswers = score,
            durationMillis = durationMillis,
            isNewRecord = isNewRecord,
        )
    }

    private inline fun updateCompletedAttempt(
        attemptId: Long,
        transform: (RememberNumberUiState.Mistake) -> RememberNumberUiState.Mistake,
    ) {
        _uiState.update { state ->
            if (attemptId == currentAttemptId && state is RememberNumberUiState.Mistake) {
                transform(state)
            } else {
                state
            }
        }
    }

    private fun finishSessionTelemetry(): Long? {
        val startMark = sessionStartMark ?: return null
        sessionStartMark = null
        return startMark.elapsedNow().inWholeMilliseconds
    }

    private fun sessionMode(): Pair<GameModeId, String?> {
        val digits = normalizedDigits()
        return when (maxLength) {
            4 if digits == ALL_DIGITS -> GameModeId.NumberSprintClassic to null
            4 if digits == BINARY_DIGITS -> GameModeId.NumberSprintBinary to null
            else -> GameModeId.NumberSprintCustom to "length:$maxLength;digits:$digits"
        }
    }

    private fun normalizedDigits(): String = availableDigits.toSet().sorted().joinToString(separator = "")

    private companion object {
        const val ALL_DIGITS = "0123456789"
        const val BINARY_DIGITS = "01"
    }
}
