package com.alad1nks.oquturbo.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.AppLanguage
import com.alad1nks.oquturbo.core.data.model.DailyTrainingProgress
import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.model.PlayerProgress
import com.alad1nks.oquturbo.core.data.model.ProfilePreferences
import com.alad1nks.oquturbo.core.data.practice.calculatePracticeRhythm
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.data.repository.ProfileRepository
import com.alad1nks.oquturbo.core.data.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class ProfileViewModel(
    private val activityRepository: GameActivityRepository,
    private val dailyTrainingRepository: DailyTrainingRepository,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    private var isSavingIdentity = false

    private var baseState =
        createProfileUiState(
            emptyPlayerProgress(),
            emptyList(),
            ProfilePreferences(),
            emptyList(),
            null,
            DailyTrainingProgress(),
        )
    private var historyState: ProfilePracticeState = ProfilePracticeState.Loading
    private val mutableUiState = MutableStateFlow(baseState)
    val uiState = mutableUiState.asStateFlow()
    private var baseJob: Job? = null
    private var historyJob: Job? = null
    private val historyReload = MutableStateFlow(0L)
    private var historyLoading = false
    private var lastHistory: DayHistory? = null
    private var historyEpochDay = currentEpochDay()

    private val baseFlow =
        combine(
            activityRepository.observeProgress(),
            activityRepository.observeRecords(),
            profileRepository.observePreferences(),
            activityRepository.observeSessions(),
            combine(
                dailyTrainingRepository.observeTodayTraining(),
                dailyTrainingRepository.observeProgress(),
            ) { todayTraining, trainingProgress ->
                todayTraining to trainingProgress
            },
        ) { progress, records, preferences, sessions, trainingState ->
            createProfileUiState(
                progress = progress,
                records = records,
                preferences = preferences,
                sessions = sessions,
                todayTraining = trainingState.first,
                trainingProgress = trainingState.second,
            )
        }

    init {
        viewModelScope.launch {
            mutableUiState.subscriptionCount.map { it > 0 }.distinctUntilChanged().collectLatest { subscribed ->
                if (subscribed) {
                    if (baseJob?.isActive != true) {
                        baseJob =
                            viewModelScope.launch {
                                baseFlow.retryWhen { error, _ ->
                                    if (error is CancellationException) return@retryWhen false
                                    delay(1_000)
                                    true
                                }.collect {
                                    baseState = it
                                    renderHistory()
                                }
                            }
                    }
                    if (historyJob?.isActive != true) observeHistory()
                } else {
                    delay(STOP_TIMEOUT_MILLIS)
                    showHistoryLoading()
                    historyJob?.cancelAndJoin()
                    historyJob = null
                    baseJob?.cancelAndJoin()
                    baseJob = null
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                refreshPracticeDay()
                delay(1_000)
            }
        }
    }

    private fun observeHistory() {
        showHistoryLoading()
        historyJob =
            viewModelScope.launch {
                historyReload.collectLatest {
                    showHistoryLoading()
                    activityRepository.observePracticeHistory().retryWhen { error, _ ->
                        if (error is CancellationException) return@retryWhen false
                        historyLoading = false
                        historyState = ProfilePracticeState.Error
                        renderHistory()
                        delay(1_000)
                        true
                    }.collect { history ->
                        historyLoading = false
                        lastHistory = history
                        historyEpochDay = currentEpochDay()
                        historyState = ProfilePracticeState.Ready(calculatePracticeRhythm(history, historyEpochDay))
                        renderHistory()
                    }
                }
            }
    }

    private fun renderHistory() {
        mutableUiState.value = baseState.withPracticeHistory(historyState)
    }

    private fun showHistoryLoading() {
        historyLoading = true
        historyState = ProfilePracticeState.Loading
        renderHistory()
    }

    fun retryPracticeHistory() {
        if (historyLoading || historyState != ProfilePracticeState.Error) return
        showHistoryLoading()
        historyReload.value++
    }

    fun refreshProfile() {
        if (!historyLoading) {
            showHistoryLoading()
            historyReload.value++
        }
        refreshPracticeDay()
    }

    private fun refreshPracticeDay() {
        val today = currentEpochDay()
        if (today == historyEpochDay) return
        historyEpochDay = today
        if (historyState is ProfilePracticeState.Ready) {
            lastHistory?.let {
                historyState = ProfilePracticeState.Ready(calculatePracticeRhythm(it, today))
                renderHistory()
            }
        }
    }

    private fun currentEpochDay(): Long = clock.now().toEpochMilliseconds() / 86_400_000L

    val settingsUiState: StateFlow<ProfileSettingsUiState> =
        combine(
            settingsRepository.getDarkTheme(),
            settingsRepository.getSoundEnabled(),
            settingsRepository.getVibrationEnabled(),
            settingsRepository.getRemindersEnabled(),
            settingsRepository.getLanguage(),
        ) { darkTheme, sound, vibration, reminders, language ->
            ProfileSettingsUiState(
                darkThemeEnabled = darkTheme == true,
                soundEnabled = sound ?: true,
                vibrationEnabled = vibration ?: true,
                remindersEnabled = reminders ?: false,
                language = language,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ProfileSettingsUiState(),
        )

    fun updateIdentity(
        displayName: String,
        avatarId: PersonalizationId,
        onSaved: () -> Unit,
    ) {
        if (isSavingIdentity) return
        val avatar = uiState.value.personalization.firstOrNull { it.id == avatarId && it.isUnlocked } ?: return
        isSavingIdentity = true
        viewModelScope.launch {
            try {
                profileRepository.updateIdentity(
                    displayName = displayName,
                    selectedAvatar = avatar.id.name,
                )
                onSaved()
            } finally {
                isSavingIdentity = false
            }
        }
    }

    fun selectTitle(titleId: TitleId) {
        if (uiState.value.titles.none { it.id == titleId && it.isUnlocked }) return
        viewModelScope.launch { profileRepository.selectTitle(titleId.name) }
    }

    fun selectPersonalization(itemId: PersonalizationId) {
        val item = uiState.value.personalization.firstOrNull { it.id == itemId && it.isUnlocked } ?: return
        viewModelScope.launch {
            when (item.category) {
                PersonalizationCategory.Avatar -> profileRepository.selectAvatar(item.id.name)
                PersonalizationCategory.AvatarFrame -> profileRepository.selectFrame(item.id.name)
                PersonalizationCategory.CardBackground -> profileRepository.selectBackground(item.id.name)
            }
        }
    }

    fun setDarkTheme(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(value)
        }
    }

    fun setLanguage(value: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(value) }
    }

    fun setSoundEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setSoundEnabled(value) }
    }

    fun setVibrationEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setVibrationEnabled(value) }
    }

    fun setRemindersEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setRemindersEnabled(value) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

internal data class ProfileSettingsUiState(
    val darkThemeEnabled: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val remindersEnabled: Boolean = false,
    val language: AppLanguage = AppLanguage.System,
)

private fun emptyPlayerProgress() =
    PlayerProgress(
        totalCorrectAnswers = 0,
        totalXp = 0,
        level = 1,
        currentLevelXp = 0,
    )
