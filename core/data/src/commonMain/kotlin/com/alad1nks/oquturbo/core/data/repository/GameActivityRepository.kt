package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.data.model.GameActivityTotals
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameRecord
import com.alad1nks.oquturbo.core.data.model.GameSeriesTotals
import com.alad1nks.oquturbo.core.data.model.GameSession
import com.alad1nks.oquturbo.core.data.model.PlayerProgress
import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class GameActivityRepository(
    private val storage: Storage,
) {
    private val writeMutex = Mutex()
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    fun observeSessions(): Flow<List<GameSession>> =
        observePayload()
            .map { payload -> payload.sessions }
            .distinctUntilChanged()

    fun observeRecords(): Flow<List<GameRecord>> =
        combine(
            observePayload(),
            observeLegacyRecords(),
        ) { payload, legacyRecords ->
            mergeRecords(payload.records + legacyRecords)
        }.distinctUntilChanged()

    fun observeProgress(): Flow<PlayerProgress> =
        observePayload()
            .map { payload -> requireNotNull(payload.totals).correctAnswers.toPlayerProgress() }
            .distinctUntilChanged()

    fun observeTotals(): Flow<GameActivityTotals> =
        observePayload()
            .map { payload -> requireNotNull(payload.totals) }
            .distinctUntilChanged()

    @OptIn(ExperimentalTime::class)
    suspend fun recordCompletedSession(
        game: GameId,
        mode: GameModeId,
        variantId: String? = null,
        score: Int,
        correctAnswers: Int = score,
        durationMillis: Long,
        isNewRecord: Boolean,
    ) =
        withContext(NonCancellable) {
            val completedAtEpochMillis = Clock.System.now().toEpochMilliseconds()
            val session =
                GameSession(
                    game = game,
                    mode = mode,
                    variantId = variantId?.takeIf(String::isNotBlank),
                    score = score.coerceAtLeast(0),
                    correctAnswers = correctAnswers.coerceAtLeast(0),
                    durationMillis = durationMillis.coerceAtLeast(0),
                    completedAtEpochMillis = completedAtEpochMillis,
                    completedEpochDay = completedAtEpochMillis / MILLIS_PER_DAY,
                    isNewRecord = isNewRecord,
                )

            writeMutex.withLock {
                val payload = decodePayload(storage.getGameSessionsJson().first())
                val previousStoredRecord =
                    payload.records
                        .filter { it.key() == session.key() }
                        .maxOfOrNull(GameRecord::score) ?: 0
                val verifiedSession =
                    session.copy(
                        isNewRecord = session.isNewRecord && session.score > previousStoredRecord,
                    )
                val updatedSessions = (payload.sessions + verifiedSession).takeLast(MAX_STORED_SESSIONS)
                val updatedTotals = requireNotNull(payload.totals).withSession(verifiedSession)
                val updatedRecords =
                    if (verifiedSession.score > 0) {
                        mergeRecords(
                            payload.records +
                                GameRecord(
                                    game = verifiedSession.game,
                                    mode = verifiedSession.mode,
                                    variantId = verifiedSession.variantId,
                                    score = verifiedSession.score,
                                ),
                        )
                    } else {
                        payload.records
                    }
                storage.setGameSessionsJson(
                    json.encodeToString(
                        GameSessionsPayload(
                            sessions = updatedSessions,
                            records = updatedRecords,
                            totalCorrectAnswers = updatedTotals.correctAnswers,
                            totals = updatedTotals,
                        ),
                    ),
                )
            }
        }

    private fun observePayload(): Flow<GameSessionsPayload> =
        storage.getGameSessionsJson()
            .map(::decodePayload)
            .distinctUntilChanged()

    private fun decodePayload(value: String?): GameSessionsPayload {
        if (value.isNullOrBlank()) return GameSessionsPayload().normalized()
        val payload =
            runCatching {
                json.decodeFromString<GameSessionsPayload>(value)
            }.getOrElse { cause ->
                throw IllegalStateException("Stored game activity payload is malformed", cause)
            }
        check(payload.version == STORAGE_VERSION) {
            "Unsupported game activity payload version: ${payload.version}"
        }
        return payload.normalized()
    }

    private fun GameSessionsPayload.normalized(): GameSessionsPayload {
        val derivedTotals = sessions.toTotals()
        val normalizedTotals =
            (totals ?: derivedTotals).let { storedTotals ->
                storedTotals.copy(
                    sessionCount = maxOf(storedTotals.sessionCount.coerceAtLeast(0), derivedTotals.sessionCount),
                    durationMillis = maxOf(storedTotals.durationMillis.coerceAtLeast(0), derivedTotals.durationMillis),
                    correctAnswers =
                        maxOf(
                            storedTotals.correctAnswers.coerceAtLeast(0),
                            totalCorrectAnswers.coerceAtLeast(0),
                            derivedTotals.correctAnswers,
                        ),
                    series = mergeSeriesTotals(storedTotals.series, derivedTotals.series),
                )
            }
        return copy(
            totalCorrectAnswers = normalizedTotals.correctAnswers,
            totals = normalizedTotals,
        )
    }

    private fun List<GameSession>.toTotals(): GameActivityTotals {
        val series =
            groupBy { session -> RecordKey(session.game, session.mode, session.variantId) }
                .map { (key, sessions) ->
                    GameSeriesTotals(
                        game = key.game,
                        mode = key.mode,
                        variantId = key.variantId,
                        sessionCount = sessions.size.toLong(),
                        durationMillis = sessions.fold(0L) { total, session -> total.add(session.durationMillis) },
                        correctAnswers =
                            sessions.fold(0L) { total, session ->
                                total.add(session.correctAnswers.toLong())
                            },
                        scoreTotal = sessions.fold(0L) { total, session -> total.add(session.score.toLong()) },
                    )
                }.sorted()
        return GameActivityTotals(
            sessionCount = size.toLong(),
            durationMillis = fold(0L) { total, session -> total.add(session.durationMillis) },
            correctAnswers = fold(0L) { total, session -> total.add(session.correctAnswers.toLong()) },
            series = series,
        )
    }

    private fun GameActivityTotals.withSession(session: GameSession): GameActivityTotals {
        val key = RecordKey(session.game, session.mode, session.variantId)
        val existingSeries = series.firstOrNull { it.key() == key }
        val updatedSeries =
            GameSeriesTotals(
                game = session.game,
                mode = session.mode,
                variantId = session.variantId,
                sessionCount = existingSeries?.sessionCount.orZero().add(1),
                durationMillis = existingSeries?.durationMillis.orZero().add(session.durationMillis),
                correctAnswers = existingSeries?.correctAnswers.orZero().add(session.correctAnswers.toLong()),
                scoreTotal = existingSeries?.scoreTotal.orZero().add(session.score.toLong()),
            )
        return copy(
            sessionCount = sessionCount.add(1),
            durationMillis = durationMillis.add(session.durationMillis),
            correctAnswers = correctAnswers.add(session.correctAnswers.toLong()),
            series = (series.filterNot { it.key() == key } + updatedSeries).sorted(),
        )
    }

    private fun GameSeriesTotals.key() = RecordKey(game, mode, variantId)

    private fun GameRecord.key() = RecordKey(game, mode, variantId)

    private fun GameSession.key() = RecordKey(game, mode, variantId)

    private fun mergeSeriesTotals(
        stored: List<GameSeriesTotals>,
        derived: List<GameSeriesTotals>,
    ): List<GameSeriesTotals> =
        (stored + derived)
            .groupBy { it.key() }
            .map { (key, values) ->
                GameSeriesTotals(
                    game = key.game,
                    mode = key.mode,
                    variantId = key.variantId,
                    sessionCount = values.maxOf { it.sessionCount.coerceAtLeast(0) },
                    durationMillis = values.maxOf { it.durationMillis.coerceAtLeast(0) },
                    correctAnswers = values.maxOf { it.correctAnswers.coerceAtLeast(0) },
                    scoreTotal = values.maxOf { it.scoreTotal.coerceAtLeast(0) },
                )
            }.sorted()

    private fun List<GameSeriesTotals>.sorted(): List<GameSeriesTotals> =
        sortedWith(
            compareBy<GameSeriesTotals>({ it.game.ordinal }, { it.mode.ordinal }, { it.variantId.orEmpty() }),
        )

    private fun Long?.orZero(): Long = this?.coerceAtLeast(0) ?: 0

    private fun Long.add(value: Long): Long {
        val currentValue = coerceAtLeast(0)
        val addedValue = value.coerceAtLeast(0)
        return if (currentValue > Long.MAX_VALUE - addedValue) Long.MAX_VALUE else currentValue + addedValue
    }

    private fun observeLegacyRecords(): Flow<List<GameRecord>> {
        val sources =
            listOf(
                LegacyRecordSource(
                    game = GameId.NumberSprint,
                    mode = GameModeId.NumberSprintClassic,
                    score = storage.getRememberNumberRecord(maxLength = 4, availableDigits = "0123456789"),
                ),
                LegacyRecordSource(
                    game = GameId.NumberSprint,
                    mode = GameModeId.NumberSprintBinary,
                    score = storage.getRememberNumberRecord(maxLength = 4, availableDigits = "01"),
                ),
                LegacyRecordSource(
                    GameId.WideEye,
                    GameModeId.WideEyeCharacters,
                    storage.getKenKozGameRecord("Characters"),
                ),
                LegacyRecordSource(GameId.WideEye, GameModeId.WideEyeWords, storage.getKenKozGameRecord("Words")),
                LegacyRecordSource(
                    GameId.WideEye,
                    GameModeId.WideEyeFindDifference,
                    storage.getKenKozGameRecord("FindDifference"),
                ),
                LegacyRecordSource(GameId.WideEye, GameModeId.WideEyeWideLine, storage.getKenKozGameRecord("WideLine")),
                LegacyRecordSource(
                    GameId.DontTap,
                    GameModeId.DontTapCategories,
                    storage.getBaspaGameRecord("Categories"),
                ),
                LegacyRecordSource(GameId.DontTap, GameModeId.DontTapLetter, storage.getBaspaGameRecord("Letter")),
                LegacyRecordSource(
                    GameId.DontTap,
                    GameModeId.DontTapWordLength,
                    storage.getBaspaGameRecord("WordLength"),
                ),
                LegacyRecordSource(
                    GameId.DontTap,
                    GameModeId.DontTapTextColor,
                    storage.getBaspaGameRecord("TextColor"),
                ),
                LegacyRecordSource(
                    GameId.DontTap,
                    GameModeId.DontTapTrueFalse,
                    storage.getBaspaGameRecord("TrueFalse"),
                ),
                LegacyRecordSource(GameId.DontTap, GameModeId.DontTapMath, storage.getBaspaGameRecord("Math")),
                LegacyRecordSource(
                    GameId.DontTap,
                    GameModeId.DontTapSpeedReading,
                    storage.getBaspaGameRecord("SpeedReading"),
                ),
            )

        return combine(sources.map { it.score }) { scores ->
            scores.mapIndexedNotNull { index, score ->
                score?.takeIf { it > 0 }?.let {
                    val source = sources[index]
                    GameRecord(
                        game = source.game,
                        mode = source.mode,
                        score = it,
                    )
                }
            }
        }
    }

    private fun mergeRecords(records: List<GameRecord>): List<GameRecord> =
        records
            .groupBy { RecordKey(it.game, it.mode, it.variantId) }
            .map { (key, matchingRecords) ->
                GameRecord(
                    game = key.game,
                    mode = key.mode,
                    variantId = key.variantId,
                    score = matchingRecords.maxOf(GameRecord::score),
                )
            }.sortedWith(
                compareBy<GameRecord>({ it.game.ordinal }, { it.mode.ordinal }, { it.variantId.orEmpty() }),
            )

    private fun Long.toPlayerProgress(): PlayerProgress {
        val totalCorrectAnswers = coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        val totalXp = totalCorrectAnswers
        return PlayerProgress(
            totalCorrectAnswers = totalCorrectAnswers,
            totalXp = totalXp,
            level = totalXp / XP_PER_LEVEL + 1,
            currentLevelXp = totalXp % XP_PER_LEVEL,
            xpPerLevel = XP_PER_LEVEL,
        )
    }

    @Serializable
    private data class GameSessionsPayload(
        val version: Int = STORAGE_VERSION,
        val sessions: List<GameSession> = emptyList(),
        val records: List<GameRecord> = emptyList(),
        val totalCorrectAnswers: Long = 0,
        val totals: GameActivityTotals? = null,
    )

    private data class LegacyRecordSource(
        val game: GameId,
        val mode: GameModeId,
        val score: Flow<Int?>,
    )

    private data class RecordKey(
        val game: GameId,
        val mode: GameModeId,
        val variantId: String?,
    )

    private companion object {
        const val MAX_STORED_SESSIONS = 1_000
        const val MILLIS_PER_DAY = 86_400_000L
        const val STORAGE_VERSION = 1
        const val XP_PER_LEVEL = 500
    }
}
