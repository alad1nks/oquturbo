package com.alad1nks.oquturbo.feature.remembernumber

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class RememberNumberViewModel(
    val maxLength: Int,
) : ViewModel() {
    private var score: Int = 0
    private var waitingNumber: String = ""
    private var delay: Long = 1000
    private var isFirstNumber = true

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
                                _uiState.value = RememberNumberUiState.Reading(
                                    text = waitingNumber,
                                    score = score.toString(),
                                )
                                delay = (delay * 95) / 100
                            } else {
                                _uiState.value = RememberNumberUiState.Mistake(
                                    text = text,
                                    score = score.toString(),
                                    correctText = waitingNumber,
                                )
                                delay = 1000
                            }
                            _focusEvent.value = null
                        }
                    }
                    is RememberNumberUiState.Reading -> {
                        delay(delay)
                        if (isFirstNumber) {
                            delay(700)
                            isFirstNumber = false
                        }
                        _uiState.value = RememberNumberUiState.Writing(
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
            isFirstNumber = true
            waitingNumber = generateNumber()
            _uiState.value = RememberNumberUiState.Reading(
                text = waitingNumber,
                score = "0",
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
            stringBuilder.append((0..9).random())
        }

        return stringBuilder.toString()
    }
}