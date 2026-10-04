package com.alad1nks.oquturbo.core.data.model

/** Confirmed local facts; the first tracking day has only partial coverage. */
class DayHistory(
    val trackingStartedEpochDay: Long,
    completedEpochDays: Collection<Long>,
) {
    private val completedDays = completedEpochDays.toSet()

    val completedEpochDays: Set<Long>
        get() = completedDays.toSet()

    fun statusOn(epochDay: Long, todayEpochDay: Long): DayCompletionStatus =
        when {
            epochDay > todayEpochDay -> DayCompletionStatus.Unknown
            epochDay in completedDays -> DayCompletionStatus.Completed
            epochDay <= trackingStartedEpochDay -> DayCompletionStatus.Unknown
            else -> DayCompletionStatus.NoCompletionRecorded
        }

    internal fun withCompletedDay(epochDay: Long): DayHistory =
        DayHistory(trackingStartedEpochDay, completedDays + epochDay)

    override fun equals(other: Any?): Boolean =
        other is DayHistory &&
            trackingStartedEpochDay == other.trackingStartedEpochDay && completedDays == other.completedDays

    override fun hashCode(): Int = 31 * trackingStartedEpochDay.hashCode() + completedDays.hashCode()

    override fun toString(): String = "DayHistory(start=$trackingStartedEpochDay, completed=$completedDays)"
}

enum class DayCompletionStatus {
    Completed,
    NoCompletionRecorded,
    Unknown,
}
