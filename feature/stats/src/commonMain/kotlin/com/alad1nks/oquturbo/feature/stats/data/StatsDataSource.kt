package com.alad1nks.oquturbo.feature.stats.data

import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriodSnapshot

/** Replace this contract with a product data implementation when activity persistence is available. */
internal interface StatsDataSource {
    fun getSnapshot(period: StatsPeriod): StatsPeriodSnapshot
}
