package com.alad1nks.oquturbo.feature.stats.data

import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.model.GameSession
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import kotlinx.coroutines.flow.Flow

/** Independent repository streams, without dashboard aggregation or a selectable period. */
internal interface WeeklyReviewDataSource {
    fun observePractice(): Flow<DayHistory>

    fun observeTraining(): Flow<DayHistory>

    fun observeSessions(): Flow<List<GameSession>>
}

internal class RepositoryWeeklyReviewDataSource(
    private val activity: GameActivityRepository,
    private val training: DailyTrainingRepository,
) : WeeklyReviewDataSource {
    override fun observePractice() = activity.observePracticeHistory()

    override fun observeTraining() = training.observeCompletionHistory()

    override fun observeSessions() = activity.observeSessions()
}
