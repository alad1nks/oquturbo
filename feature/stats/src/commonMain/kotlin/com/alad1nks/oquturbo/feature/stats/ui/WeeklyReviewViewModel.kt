package com.alad1nks.oquturbo.feature.stats.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.practice.calculatePracticeRhythm
import com.alad1nks.oquturbo.core.data.progress.calculateProgressComparison
import com.alad1nks.oquturbo.feature.stats.data.WeeklyReviewDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class WeeklyReviewViewModel(
    dataSource: WeeklyReviewDataSource,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(WeeklyReviewUiState(today()))
    val uiState = mutableUiState.asStateFlow()
    private val practice = Source(dataSource::observePractice)
    private val training = Source(dataSource::observeTraining)
    private val sessions = Source(dataSource::observeSessions)

    init {
        viewModelScope.launch {
            mutableUiState.subscriptionCount.map { it > 0 }.distinctUntilChanged().collectLatest { subscribed ->
                if (subscribed) {
                    practice.start()
                    training.start()
                    sessions.start()
                } else {
                    delay(STOP_TIMEOUT_MILLIS)
                    practice.stop()
                    training.stop()
                    sessions.stop()
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                if (today() != uiState.value.todayEpochDay) render()
                delay(DAY_POLL_MILLIS)
            }
        }
    }

    fun retryPractice() = practice.retry()

    fun retryTraining() = training.retry()

    fun retrySessions() = sessions.retry()

    fun refreshReview() {
        practice.refresh()
        training.refresh()
        sessions.refresh()
        render()
    }

    private fun today(): Long = clock.now().toEpochMilliseconds() / 86_400_000L

    private fun render() {
        // One sampled day and one state publication for all ranges and projections, even when
        // only one source emits at midnight. No cached ready value survives a source refresh.
        val day = today()
        mutableUiState.value =
            WeeklyReviewUiState(
                todayEpochDay = day,
                practice = practice.state.project { calculatePracticeRhythm(it, day) },
                training = training.state.project { calculatePracticeRhythm(it, day) },
                comparison = sessions.state.project { calculateProgressComparison(it, day) },
            )
    }

    private inner class Source<T>(private val observe: () -> Flow<T>) {
        var state: WeeklySource<T> = WeeklySource.Loading
            private set
        private val reload = MutableStateFlow(0L)
        private var job: Job? = null
        private var loading = false

        fun start() {
            if (job?.isActive == true) return
            showLoading()
            job =
                viewModelScope.launch {
                    reload.collectLatest {
                        showLoading()
                        observe().retryWhen { error, _ ->
                            if (error is CancellationException) return@retryWhen false
                            loading = false
                            state = WeeklySource.Error
                            render()
                            delay(RETRY_MILLIS)
                            true
                        }.collect { value ->
                            loading = false
                            state = WeeklySource.Ready(value)
                            render()
                        }
                    }
                }
        }

        suspend fun stop() {
            showLoading()
            job?.cancelAndJoin()
            job = null
        }

        fun retry() {
            if (state != WeeklySource.Error || loading) return
            refresh()
        }

        fun refresh() {
            if (loading) return
            showLoading()
            reload.value++
        }

        private fun showLoading() {
            loading = true
            state = WeeklySource.Loading
            render()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val RETRY_MILLIS = 5_000L
        const val DAY_POLL_MILLIS = 1_000L
    }
}

private inline fun <T, R> WeeklySource<T>.project(transform: (T) -> R): WeeklySource<R> =
    when (this) {
        WeeklySource.Loading -> WeeklySource.Loading
        WeeklySource.Error -> WeeklySource.Error
        is WeeklySource.Ready -> WeeklySource.Ready(transform(value))
    }
