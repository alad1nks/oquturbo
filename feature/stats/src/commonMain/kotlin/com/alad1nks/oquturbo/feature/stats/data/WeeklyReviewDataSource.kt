package com.alad1nks.oquturbo.feature.stats.data

import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.model.GameSession
import com.alad1nks.oquturbo.core.data.model.WeeklyFocus
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import kotlinx.coroutines.flow.Flow

/** Independent repository streams, without dashboard aggregation or a selectable period. */
internal interface WeeklyReviewDataSource {
    fun observeFocus(): Flow<WeeklyFocus>

    suspend fun selectFocus(): WeeklyFocus

    suspend fun disableFocus(): WeeklyFocus

    suspend fun resetFocus(): WeeklyFocus

    fun observePractice(): Flow<DayHistory>

    fun observeTraining(): Flow<DayHistory>

    fun observeSessions(): Flow<List<GameSession>>
}

internal class RepositoryWeeklyReviewDataSource(
    private val activity: GameActivityRepository,
    private val training: DailyTrainingRepository,
) : WeeklyReviewDataSource {
    override fun observeFocus() = training.observeWeeklyFocus()

    override suspend fun selectFocus() = training.selectWeeklyFocus()

    override suspend fun disableFocus() = training.disableWeeklyFocus()

    override suspend fun resetFocus() = training.resetWeeklyFocus()

    override fun observePractice() = activity.observePracticeHistory()

    override fun observeTraining() = training.observeCompletionHistory()

    override fun observeSessions() = activity.observeSessions()
}
