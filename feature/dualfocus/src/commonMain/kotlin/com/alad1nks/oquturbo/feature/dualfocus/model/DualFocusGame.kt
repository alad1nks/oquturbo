package com.alad1nks.oquturbo.feature.dualfocus.model

enum class DualFocusShape { Circle, Square, Triangle, Diamond }

enum class DualFocusLane { One, Two }

enum class DualFocusPhase { Ready, Active, Paused, Result }

enum class DualFocusFailure { WrongTap, MissedTarget }

data class DualFocusCard(
    val id: Long,
    val lane: DualFocusLane,
    val shownShape: DualFocusShape,
    val targetShape: DualFocusShape,
    val appearedAtMillis: Long,
    val expiresAtMillis: Long,
) {
    val isTarget: Boolean get() = shownShape == targetShape
}

data class DualFocusResult(
    val failure: DualFocusFailure,
    val lane: DualFocusLane,
    val targetShape: DualFocusShape,
    val shownShape: DualFocusShape?,
)

data class DualFocusState(
    val phase: DualFocusPhase = DualFocusPhase.Ready,
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val targets: Map<DualFocusLane, DualFocusShape> = emptyMap(),
    val cards: Map<DualFocusLane, DualFocusCard> = emptyMap(),
    val nextEventsAtMillis: Map<DualFocusLane, Long> = emptyMap(),
    val result: DualFocusResult? = null,
    val nowMillis: Long = 0,
)

interface DualFocusRandom {
    fun nextInt(until: Int): Int
}

object DefaultDualFocusRandom : DualFocusRandom {
    override fun nextInt(until: Int): Int = kotlin.random.Random.nextInt(until)
}

class DualFocusGame(
    private val random: DualFocusRandom = DefaultDualFocusRandom,
) {
    var state: DualFocusState = DualFocusState()
        private set

    private var nextCardId = 1L
    private val eventsSinceTarget = mutableMapOf(DualFocusLane.One to 0, DualFocusLane.Two to 0)

    fun prepare() {
        if (state.phase != DualFocusPhase.Ready || state.targets.isNotEmpty()) return
        state = state.copy(targets = chooseTargets())
    }

    fun start(nowMillis: Long = 0) {
        val targets = state.targets.takeIf { state.phase == DualFocusPhase.Ready && it.size == 2 } ?: chooseTargets()
        nextCardId = 1
        eventsSinceTarget[DualFocusLane.One] = 0
        eventsSinceTarget[DualFocusLane.Two] = 0
        state =
            DualFocusState(
                phase = DualFocusPhase.Active,
                targets = targets,
                nextEventsAtMillis =
                    mapOf(
                        DualFocusLane.One to nowMillis,
                        DualFocusLane.Two to nowMillis + LANE_TWO_OFFSET_MILLIS,
                    ),
                nowMillis = nowMillis,
            )
        advanceTo(nowMillis)
    }

    private fun chooseTargets(): Map<DualFocusLane, DualFocusShape> {
        val shapes = DualFocusShape.entries.shuffledBy(random)
        return mapOf(DualFocusLane.One to shapes[0], DualFocusLane.Two to shapes[1])
    }

    fun tap(
        lane: DualFocusLane,
        cardId: Long,
        nowMillis: Long,
    ) {
        advanceTo(nowMillis)
        val current = state
        if (current.phase != DualFocusPhase.Active) return
        val card = current.cards[lane] ?: return
        if (card.id != cardId || nowMillis >= card.expiresAtMillis) return
        if (card.isTarget) {
            state =
                current.copy(
                    score = current.score + 1,
                    correctAnswers = current.correctAnswers + 1,
                    cards = current.cards - lane,
                )
        } else {
            fail(DualFocusFailure.WrongTap, card, card.shownShape)
        }
    }

    fun pause() {
        if (state.phase != DualFocusPhase.Active) return
        state = state.copy(phase = DualFocusPhase.Paused)
    }

    fun resume() {
        if (state.phase != DualFocusPhase.Paused) return
        state = state.copy(phase = DualFocusPhase.Active)
    }

    fun advanceTo(nowMillis: Long) {
        if (state.phase != DualFocusPhase.Active || nowMillis < state.nowMillis) return
        while (state.phase == DualFocusPhase.Active) {
            val expiry = state.cards.values.minByOrNull(DualFocusCard::expiresAtMillis)
            val dueLane =
                DualFocusLane.entries
                    .filter { state.cards[it] == null }
                    .minByOrNull { state.nextEventsAtMillis[it] ?: Long.MAX_VALUE }
            val dueAt = dueLane?.let(state.nextEventsAtMillis::get)
            val nextAt = listOfNotNull(expiry?.expiresAtMillis, dueAt).minOrNull() ?: break
            if (nextAt > nowMillis) break
            if (expiry != null && expiry.expiresAtMillis <= (dueAt ?: Long.MAX_VALUE)) {
                state = state.copy(cards = state.cards - expiry.lane, nowMillis = expiry.expiresAtMillis)
                if (expiry.isTarget) {
                    fail(DualFocusFailure.MissedTarget, expiry, shownShape = null)
                }
            } else if (dueLane != null && dueAt != null) {
                state = state.copy(nowMillis = dueAt)
                scheduleCard(dueLane, dueAt)
            }
        }
        if (state.phase == DualFocusPhase.Active) state = state.copy(nowMillis = nowMillis)
    }

    private fun scheduleCard(
        lane: DualFocusLane,
        appearedAtMillis: Long,
    ) {
        val target = state.targets.getValue(lane)
        val count = eventsSinceTarget.getValue(lane)
        val otherTarget = state.cards.values.firstOrNull { it.lane != lane && it.isTarget }
        val targetRequired = count >= MAX_EVENTS_BETWEEN_TARGETS - 1
        if (targetRequired && otherTarget != null) {
            state =
                state.copy(
                    nextEventsAtMillis =
                        state.nextEventsAtMillis +
                            (lane to maxOf(appearedAtMillis + 1, otherTarget.expiresAtMillis)),
                )
            return
        }
        val useTarget = otherTarget == null && (targetRequired || random.nextInt(TARGET_CHANCE_DIVISOR) == 0)
        val shownShape =
            if (useTarget) {
                target
            } else {
                DualFocusShape.entries.filterNot { it == target }[random.nextInt(DISTRACTOR_COUNT)]
            }
        eventsSinceTarget[lane] = if (useTarget) 0 else count + 1
        val timing = timingFor(state.score)
        val card =
            DualFocusCard(
                id = nextCardId++,
                lane = lane,
                shownShape = shownShape,
                targetShape = target,
                appearedAtMillis = appearedAtMillis,
                expiresAtMillis = appearedAtMillis + timing.windowMillis,
            )
        state =
            state.copy(
                cards = state.cards + (lane to card),
                nextEventsAtMillis =
                    state.nextEventsAtMillis +
                        (lane to (appearedAtMillis + timing.intervalMillis)),
            )
    }

    private fun fail(
        failure: DualFocusFailure,
        card: DualFocusCard,
        shownShape: DualFocusShape?,
    ) {
        state =
            state.copy(
                phase = DualFocusPhase.Result,
                cards = emptyMap(),
                nextEventsAtMillis = emptyMap(),
                result = DualFocusResult(failure, card.lane, card.targetShape, shownShape),
            )
    }

    data class Timing(
        val intervalMillis: Long,
        val windowMillis: Long,
    )

    companion object {
        const val LANE_TWO_OFFSET_MILLIS = 700L
        const val MAX_EVENTS_BETWEEN_TARGETS = 4
        private const val TARGET_CHANCE_DIVISOR = 3
        private const val DISTRACTOR_COUNT = 3

        fun timingFor(score: Int): Timing =
            when {
                score < 5 -> Timing(intervalMillis = 1_400, windowMillis = 1_100)
                score < 15 -> Timing(intervalMillis = 1_150, windowMillis = 850)
                else -> Timing(intervalMillis = 900, windowMillis = 650)
            }
    }
}

private fun <T> List<T>.shuffledBy(random: DualFocusRandom): List<T> {
    val values = toMutableList()
    for (index in values.lastIndex downTo 1) {
        val swapIndex = random.nextInt(index + 1)
        val value = values[index]
        values[index] = values[swapIndex]
        values[swapIndex] = value
    }
    return values
}
