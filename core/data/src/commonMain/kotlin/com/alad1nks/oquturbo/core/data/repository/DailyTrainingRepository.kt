package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry
import com.alad1nks.oquturbo.core.data.model.DailyTrainingPlan
import com.alad1nks.oquturbo.core.data.model.DailyTrainingProgress
import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.WeeklyFocus
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusPhase
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusSelection
import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DailyTrainingRepository(
    private val storage: Storage,
    private val clock: Clock = Clock.System,
) {
    private val writeMutex = Mutex()
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    fun observeWeeklyFocus(): Flow<WeeklyFocus> =
        storage.getWeeklyFocusJson().map(::decodeWeeklyFocus).distinctUntilChanged()

    suspend fun selectWeeklyFocus(): WeeklyFocus =
        writeMutex.withLock {
            val current = readFocus()
            val day = currentEpochDay()
            if (current.phaseOn(day) in setOf(WeeklyFocusPhase.Scheduled, WeeklyFocusPhase.Active)) {
                current
            } else {
                writeFocus(WeeklyFocus(WeeklyFocusSelection.after(day)))
            }
        }

    suspend fun disableWeeklyFocus(): WeeklyFocus =
        writeMutex.withLock {
            val current = readFocus()
            if (current.selection == null) current else writeFocus(WeeklyFocus())
        }

    /** Explicit recovery changes only focus; it does not depend on decoding the old setting. */
    suspend fun resetWeeklyFocus(): WeeklyFocus = writeMutex.withLock { writeFocus(WeeklyFocus()) }

    private suspend fun readFocus(): WeeklyFocus =
        decodeWeeklyFocus(withStorageRetry { storage.getWeeklyFocusJson().first() })

    private suspend fun writeFocus(focus: WeeklyFocus): WeeklyFocus {
        // Keep one sampled interval and the exact same bytes through all write attempts.
        val payload = focus.encodeFocus()
        withStorageRetry { storage.setWeeklyFocusJson(payload) }
        return focus
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

    fun observeCompletionHistory(): Flow<DayHistory> =
        storage.getDailyTrainingProgressJson()
            .onStart { initializeCompletionHistory() }
            .map { requireNotNull(decodeDayHistory(decodeStoredProgress(it).completionHistory)) }
            .distinctUntilChanged()

    private suspend fun initializeCompletionHistory() {
        writeMutex.withLock {
            val stored = readStoredProgress()
            if (decodeDayHistory(stored.completionHistory) == null) {
                val history = stored.initialHistory()
                storage.setDailyTrainingProgressJson(
                    encodeProgress(stored.copy(completionHistory = history.encodeHistory())),
                )
            }
        }
    }

    suspend fun ensureTodayTraining(): DailyTrainingPlan =
        writeMutex.withLock {
            var result: DailyTrainingPlan? = null
            while (result == null) {
                val currentDay = currentEpochDay()
                val storedPlan = readPlan()?.takeIf { it.epochDay == currentDay }
                val progress = readStoredProgress()
                if (currentEpochDay() != currentDay) continue
                if (storedPlan != null) {
                    val reconciled = reconcileReceipt(storedPlan, progress)
                    if (reconciled != storedPlan) writePlan(reconciled)
                    if (currentEpochDay() == currentDay) result = reconciled
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
                val storedPlan = readPlan()?.takeIf { it.epochDay == currentDay }
                val progress = readStoredProgress()
                if (currentEpochDay() != currentDay) continue
                val plan = storedPlan?.let { reconcileReceipt(it, progress) } ?: createPlan(currentDay)

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
                    incrementCompletedTrainings(currentDay, progress)
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

    private suspend fun readStoredProgress(): StoredProgress {
        val value = withStorageRetry { storage.getDailyTrainingProgressJson().first() }
        return decodeStoredProgress(value).also { decodeDayHistory(it.completionHistory) }
    }

    private fun reconcileReceipt(plan: DailyTrainingPlan, stored: StoredProgress): DailyTrainingPlan =
        if (!plan.isCompleted && stored.hasReceipt(plan.epochDay)) {
            plan.copy(entries = plan.entries.map { it.copy(isCompleted = true) })
        } else {
            plan
        }

    private fun StoredProgress.hasReceipt(epochDay: Long): Boolean =
        progress.lastCompletedEpochDay == epochDay ||
            epochDay in decodeDayHistory(completionHistory)?.completedEpochDays.orEmpty()

    private fun StoredProgress.initialHistory(): DayHistory {
        val start = currentEpochDay()
        return DayHistory(start, listOfNotNull(progress.lastCompletedEpochDay?.takeIf { it == start }))
    }

    private suspend fun incrementCompletedTrainings(epochDay: Long, stored: StoredProgress) {
        if (stored.hasReceipt(epochDay)) return
        val history = (decodeDayHistory(stored.completionHistory) ?: stored.initialHistory()).withCompletedDay(epochDay)
        writeProgress(
            stored.copy(
                progress =
                    stored.progress.copy(
                        totalCompletedTrainings = stored.progress.totalCompletedTrainings + 1,
                        lastCompletedEpochDay = epochDay,
                    ),
                completionHistory = history.encodeHistory(),
            ),
        )
    }

    private suspend fun writePlan(plan: DailyTrainingPlan) {
        withStorageRetry {
            storage.setDailyTrainingJson(json.encodeToString(plan))
        }
    }

    private suspend fun writeProgress(progress: StoredProgress) {
        withStorageRetry {
            storage.setDailyTrainingProgressJson(encodeProgress(progress))
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

    private data class StoredProgress(
        val progress: DailyTrainingProgress = DailyTrainingProgress(),
        val completionHistory: JsonElement? = null,
    )

    private fun decodeStoredProgress(value: String?): StoredProgress {
        if (value.isNullOrBlank()) return StoredProgress()
        val progress = json.decodeFromString<DailyTrainingProgress>(value)
        check(progress.isValid()) { "Stored training progress is malformed or unsupported" }
        return StoredProgress(progress, json.parseToJsonElement(value).jsonObject["completionHistory"])
    }

    private fun encodeProgress(stored: StoredProgress): String {
        val fields = json.encodeToJsonElement(stored.progress).jsonObject
        return JsonObject(
            fields + (
                stored.completionHistory?.let {
                    mapOf("completionHistory" to it)
                } ?: emptyMap()
            ),
        ).toString()
    }

    private suspend fun createPlan(epochDay: Long): DailyTrainingPlan {
        val focus = readFocus()
        val baseline = createBaselinePlan(epochDay)
        if (focus.phaseOn(epochDay) != WeeklyFocusPhase.Active) return baseline
        val classic =
            baseline.entries.single { it.game == GameId.NumberSprint }.copy(
                id = trainingEntryId(epochDay, GameId.NumberSprint, GameModeId.NumberSprintClassic),
                mode = GameModeId.NumberSprintClassic,
                requiredScore = GameId.NumberSprint.requiredTrainingScore(),
            )
        return baseline.copy(entries = listOf(classic) + baseline.entries.filter { it.game != GameId.NumberSprint })
    }

    private fun createBaselinePlan(epochDay: Long): DailyTrainingPlan {
        val random = Random(epochDay.toRandomSeed())
        return DailyTrainingPlan(
            epochDay = epochDay,
            entries =
                DAILY_TRAINING_GAMES.shuffled(random).take(DAILY_TRAINING_GAME_COUNT).map { game ->
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
            GameId.MemoryGrid -> emptyList()
            GameId.WordFlow -> emptyList()
            GameId.DualFocus -> emptyList()
            GameId.RotationMatch -> emptyList()
            GameId.NumberTrail -> emptyList()
            GameId.SymbolCount -> emptyList()
            GameId.RuleSwitch -> emptyList()
        }

    private fun GameId.requiredTrainingScore(): Int =
        when (this) {
            GameId.NumberSprint -> 5
            GameId.WideEye -> 5
            GameId.DontTap -> 8
            GameId.MemoryGrid -> error("Memory Grid is not balanced for daily training")
            GameId.WordFlow -> error("Word Flow is not balanced for daily training")
            GameId.DualFocus -> error("Dual Focus is not balanced for daily training")
            GameId.RotationMatch -> error("Rotation Match is not available in daily training")
            GameId.NumberTrail -> error("Number Trail is not available in daily training")
            GameId.SymbolCount -> error("Symbol Count is not available in daily training")
            GameId.RuleSwitch -> error("Rule Switch is not available in daily training")
        }

    @OptIn(ExperimentalTime::class)
    private fun currentEpochDay(): Long = clock.now().toEpochMilliseconds() / MILLIS_PER_DAY

    private companion object {
        const val DAILY_TRAINING_RANDOM_SEED_SALT = 0x4F515554
        const val DAILY_TRAINING_GAME_COUNT = 3
        const val INITIAL_STORAGE_RETRY_DELAY_MILLIS = 100L
        const val MAX_STORAGE_RETRY_DELAY_MILLIS = 1_000L
        const val MILLIS_PER_DAY = 86_400_000L
        const val STORAGE_RETRY_ATTEMPTS = 4
        const val STORAGE_RETRY_DELAY_MULTIPLIER = 2
        val DAILY_TRAINING_GAMES = listOf(GameId.NumberSprint, GameId.WideEye, GameId.DontTap)
    }
}
