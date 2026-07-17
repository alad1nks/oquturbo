package com.alad1nks.oquturbo.feature.stats.data

import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriodSnapshot
import kotlinx.coroutines.flow.Flow

internal interface StatsDataSource {
    fun observeSnapshot(period: StatsPeriod): Flow<StatsPeriodSnapshot>
}
