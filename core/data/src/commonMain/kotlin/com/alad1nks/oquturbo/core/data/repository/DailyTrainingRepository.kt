package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry
import com.alad1nks.oquturbo.core.data.model.DailyTrainingPlan
import com.alad1nks.oquturbo.core.data.model.DailyTrainingProgress
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

class DailyTrainingRepository(
    private val storage: Storage,
) {
    private val writeMutex = Mutex()
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    fun observeTodayTraining(): Flow<DailyTrainingPlan?> =
        storage
            .getDailyTrainingJson()
            .retryWhen { error, _ ->
                if (error is CancellationException) return@retryWhen false
                delay(MAX_STORAGE_RETRY_DELAY_MILLIS.milliseconds)
                true
            }
            .map(::decodePlan)
            .map { plan -> plan?.takeIf { it.epochDay == currentEpochDay() } }
            .distinctUntilChanged()

    fun observeProgress(): Flow<DailyTrainingProgress> =
        storage
            .getDailyTrainingProgressJson()
            .retryWhen { error, _ ->
                if (error is CancellationException) return@retryWhen false
                delay(MAX_STORAGE_RETRY_DELAY_MILLIS.milliseconds)
                true
            }
            .map(::decodeProgress)
            .distinctUntilChanged()

    suspend fun ensureTodayTraining(): DailyTrainingPlan =
        writeMutex.withLock {
            var result: DailyTrainingPlan? = null
            while (result == null) {
                val currentDay = currentEpochDay()
                val storedPlan = readPlan()?.takeIf { it.epochDay == currentDay }
                if (currentEpochDay() != currentDay) continue
                if (storedPlan != null) {
                    result = storedPlan
                    continue
                }

                val newPlan = createPlan(currentDay)
                if (currentEpochDay() != currentDay) continue
                writePlan(newPlan)
                if (currentEpochDay() == currentDay) result = newPlan
            }
            result
        }

    suspend fun completeEntry(
        entryId: String,
        score: Int,
    ): DailyTrainingPlan =
        writeMutex.withLock {
            var result: DailyTrainingPlan? = null
            while (result == null) {
                val currentDay = currentEpochDay()
                val plan = readPlan()?.takeIf { it.epochDay == currentDay } ?: createPlan(currentDay)
                if (currentEpochDay() != currentDay) continue

                val currentEntry = plan.nextEntry
                val updatedPlan =
                    if (currentEntry?.id == entryId && score >= currentEntry.requiredScore) {
                        plan.copy(
                            entries =
                                plan.entries.map { entry ->
                                    if (entry.id == entryId) entry.copy(isCompleted = true) else entry
                                },
                        )
                    } else {
                        plan
                    }

                if (currentEpochDay() != currentDay) continue
                if (!plan.isCompleted && updatedPlan.isCompleted) {
                    incrementCompletedTrainings(currentDay)
                }
                writePlan(updatedPlan)
                if (currentEpochDay() == currentDay) result = updatedPlan
            }
            result
        }

    private suspend fun readPlan(): DailyTrainingPlan? =
        withStorageRetry {
            storage.getDailyTrainingJson().first()?.let(::decodePlan)
        }

    private suspend fun readProgress(): DailyTrainingProgress =
        withStorageRetry {
            decodeProgress(storage.getDailyTrainingProgressJson().first())
        }

    private suspend fun incrementCompletedTrainings(epochDay: Long) {
        val progress = readProgress()
        if (progress.lastCompletedEpochDay == epochDay) return
        writeProgress(
            progress.copy(
                totalCompletedTrainings = progress.totalCompletedTrainings + 1,
                lastCompletedEpochDay = epochDay,
            ),
        )
    }

    private suspend fun writePlan(plan: DailyTrainingPlan) {
        withStorageRetry {
            storage.setDailyTrainingJson(json.encodeToString(plan))
        }
    }

    private suspend fun writeProgress(progress: DailyTrainingProgress) {
        withStorageRetry {
            storage.setDailyTrainingProgressJson(json.encodeToString(progress))
        }
    }

    private suspend fun <T> withStorageRetry(block: suspend () -> T): T {
        var retryDelayMillis = INITIAL_STORAGE_RETRY_DELAY_MILLIS
        repeat(STORAGE_RETRY_ATTEMPTS - 1) {
            try {
                return block()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                delay(retryDelayMillis.milliseconds)
                retryDelayMillis =
                    (retryDelayMillis * STORAGE_RETRY_DELAY_MULTIPLIER)
                        .coerceAtMost(MAX_STORAGE_RETRY_DELAY_MILLIS)
            }
        }
        return block()
    }

    private fun decodePlan(value: String?): DailyTrainingPlan? =
        value
            ?.takeIf(String::isNotBlank)
            ?.let { runCatching { json.decodeFromString<DailyTrainingPlan>(it) }.getOrNull() }
            ?.takeIf { it.isValid() }

    private fun decodeProgress(value: String?): DailyTrainingProgress =
        value
            ?.takeIf(String::isNotBlank)
            ?.let { runCatching { json.decodeFromString<DailyTrainingProgress>(it) }.getOrNull() }
            ?.takeIf { it.isValid() }
            ?: DailyTrainingProgress()

    private fun createPlan(epochDay: Long): DailyTrainingPlan {
        val random = Random(epochDay.toRandomSeed())
        return DailyTrainingPlan(
            epochDay = epochDay,
            entries =
                GameId.entries.shuffled(random).take(DAILY_TRAINING_GAME_COUNT).map { game ->
                    val mode = game.trainingModes().random(random)
                    DailyTrainingEntry(
                        id = trainingEntryId(epochDay, game, mode),
                        game = game,
                        mode = mode,
                        requiredScore = game.requiredTrainingScore(),
                    )
                },
        )
    }

    private fun DailyTrainingPlan.isValid(): Boolean =
        version == DailyTrainingPlan.CURRENT_VERSION &&
            entries.size == DAILY_TRAINING_GAME_COUNT &&
            entries.map(DailyTrainingEntry::game).toSet().size == DAILY_TRAINING_GAME_COUNT &&
            entries.map(DailyTrainingEntry::id).toSet().size == entries.size &&
            entries.all { entry ->
                entry.id == trainingEntryId(epochDay, entry.game, entry.mode) &&
                    entry.mode in entry.game.trainingModes() &&
                    entry.requiredScore > 0
            } &&
            entries.dropWhile(DailyTrainingEntry::isCompleted).none(DailyTrainingEntry::isCompleted)

    private fun DailyTrainingProgress.isValid(): Boolean =
        version == DailyTrainingProgress.CURRENT_VERSION &&
            totalCompletedTrainings >= 0

    private fun trainingEntryId(
        epochDay: Long,
        game: GameId,
        mode: GameModeId,
    ): String = "$epochDay:${game.name}:${mode.name}"

    private fun Long.toRandomSeed(): Int =
        toInt() xor (this ushr Int.SIZE_BITS).toInt() xor DAILY_TRAINING_RANDOM_SEED_SALT

    private fun GameId.trainingModes(): List<GameModeId> =
        when (this) {
            GameId.NumberSprint ->
                listOf(
                    GameModeId.NumberSprintClassic,
                    GameModeId.NumberSprintBinary,
                )
            GameId.WideEye ->
                listOf(
                    GameModeId.WideEyeCharacters,
                    GameModeId.WideEyeWords,
                    GameModeId.WideEyeFindDifference,
                    GameModeId.WideEyeWideLine,
                )
            GameId.DontTap ->
                listOf(
                    GameModeId.DontTapCategories,
                    GameModeId.DontTapLetter,
                    GameModeId.DontTapWordLength,
                    GameModeId.DontTapTextColor,
                    GameModeId.DontTapTrueFalse,
                    GameModeId.DontTapMath,
                    GameModeId.DontTapSpeedReading,
                )
        }

    private fun GameId.requiredTrainingScore(): Int =
        when (this) {
            GameId.NumberSprint -> 5
            GameId.WideEye -> 5
            GameId.DontTap -> 8
        }

    @OptIn(ExperimentalTime::class)
    private fun currentEpochDay(): Long = Clock.System.now().toEpochMilliseconds() / MILLIS_PER_DAY

    private companion object {
        const val DAILY_TRAINING_RANDOM_SEED_SALT = 0x4F515554
        const val DAILY_TRAINING_GAME_COUNT = 3
        const val INITIAL_STORAGE_RETRY_DELAY_MILLIS = 100L
        const val MAX_STORAGE_RETRY_DELAY_MILLIS = 1_000L
        const val MILLIS_PER_DAY = 86_400_000L
        const val STORAGE_RETRY_ATTEMPTS = 4
        const val STORAGE_RETRY_DELAY_MULTIPLIER = 2
    }
}
