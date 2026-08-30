package com.alad1nks.oquturbo.feature.dualfocus.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DualFocusGameTest {
    @Test
    fun prepareExposesDistinctReadyTargetsAndStartRetainsThem() {
        val game = DualFocusGame(FakeRandom(0, 0, 0))

        game.prepare()
        val readyTargets = game.state.targets
        game.start()

        assertEquals(DualFocusPhase.Active, game.state.phase)
        assertEquals(2, readyTargets.values.toSet().size)
        assertEquals(readyTargets, game.state.targets)
    }

    @Test
    fun startChoosesDistinctTargetsAndOffsetsSecondLane() {
        val game = DualFocusGame(FakeRandom())
        game.start(nowMillis = 100)

        assertNotEquals(game.state.targets[DualFocusLane.One], game.state.targets[DualFocusLane.Two])
        assertEquals(800, game.state.nextEventsAtMillis[DualFocusLane.Two])
    }

    @Test
    fun exactScoreTiersAreStableAtBoundaries() {
        assertEquals(DualFocusGame.Timing(1_400, 1_100), DualFocusGame.timingFor(0))
        assertEquals(DualFocusGame.Timing(1_400, 1_100), DualFocusGame.timingFor(4))
        assertEquals(DualFocusGame.Timing(1_150, 850), DualFocusGame.timingFor(5))
        assertEquals(DualFocusGame.Timing(1_150, 850), DualFocusGame.timingFor(14))
        assertEquals(DualFocusGame.Timing(900, 650), DualFocusGame.timingFor(15))
    }

    @Test
    fun targetTapScoresOnceAndStaleTapIsIgnored() {
        val game = DualFocusGame(FakeRandom(0, 0, 0, 0))
        game.start()
        val card = game.state.cards.getValue(DualFocusLane.One)
        assertTrue(card.isTarget)

        game.tap(DualFocusLane.One, card.id, nowMillis = 100)
        game.tap(DualFocusLane.One, card.id, nowMillis = 100)

        assertEquals(1, game.state.score)
        assertEquals(1, game.state.correctAnswers)
        assertEquals(DualFocusPhase.Active, game.state.phase)
    }

    @Test
    fun distractorTapFailsAndFirstResolutionWins() {
        val game = DualFocusGame(FakeRandom(0, 0, 2, 1))
        game.start()
        val card = game.state.cards.getValue(DualFocusLane.One)
        assertTrue(!card.isTarget)

        game.tap(DualFocusLane.One, card.id, nowMillis = 10)
        game.advanceTo(nowMillis = 10_000)

        assertEquals(DualFocusFailure.WrongTap, game.state.result?.failure)
        assertEquals(card.shownShape, game.state.result?.shownShape)
    }

    @Test
    fun emptyResolvedAndStaleInputAreIgnored() {
        val game = DualFocusGame(FakeRandom(0, 0, 0, 0))
        game.tap(DualFocusLane.One, cardId = 1, nowMillis = 0)
        assertEquals(DualFocusPhase.Ready, game.state.phase)
        game.start()
        val card = game.state.cards.getValue(DualFocusLane.One)
        game.tap(DualFocusLane.One, card.id, nowMillis = 100)

        game.tap(DualFocusLane.One, card.id + 1, nowMillis = 100)
        game.tap(DualFocusLane.One, card.id, nowMillis = 100)

        assertEquals(1, game.state.score)
    }

    @Test
    fun distractorExpiryIsSafeButTargetExpiryFails() {
        val game = DualFocusGame(FakeRandom(0, 0, 2, 1, 0, 1, 0, 0))
        game.start()
        val distractor = game.state.cards.getValue(DualFocusLane.One)
        assertTrue(!distractor.isTarget)

        game.advanceTo(distractor.expiresAtMillis)
        assertEquals(DualFocusPhase.Active, game.state.phase)
        game.advanceTo(nowMillis = 1_400)
        val target = game.state.cards.getValue(DualFocusLane.One)
        assertTrue(target.isTarget)
        game.advanceTo(target.expiresAtMillis)

        assertEquals(DualFocusFailure.MissedTarget, game.state.result?.failure)
        assertNull(game.state.result?.shownShape)
    }

    @Test
    fun overdueTargetAfterSuspensionIsImmediatelyMissed() {
        val game = DualFocusGame(FakeRandom(0, 0, 0, 0))
        game.start()
        val target = game.state.cards.getValue(DualFocusLane.One)

        game.advanceTo(nowMillis = 60_000)

        assertEquals(DualFocusPhase.Result, game.state.phase)
        assertEquals(DualFocusFailure.MissedTarget, game.state.result?.failure)
        assertEquals(target.id, 1)
    }

    @Test
    fun everyLaneGetsATargetWithinFourOfItsOwnEvents() {
        val game = DualFocusGame(ConstantRandom(value = 2))
        game.start()
        val laneOneCards = mutableListOf(game.state.cards.getValue(DualFocusLane.One))
        listOf(1_400L, 2_800L, 4_200L).forEach { due ->
            game.advanceTo(due)
            laneOneCards += game.state.cards.getValue(DualFocusLane.One)
        }

        assertTrue(laneOneCards.take(3).none(DualFocusCard::isTarget))
        assertTrue(laneOneCards[3].isTarget)
    }

    @Test
    fun independentlyScheduledLanesNeverExposeTwoTargets() {
        val game = DualFocusGame(FakeRandom(*IntArray(100) { 0 }))
        game.start()
        repeat(30) { step ->
            assertTrue(game.state.cards.values.count(DualFocusCard::isTarget) <= 1)
            game.state.cards.values.firstOrNull(DualFocusCard::isTarget)?.let { card ->
                game.tap(card.lane, card.id, game.state.nowMillis + 1)
            }
            game.advanceTo((step + 1) * 250L)
        }
    }

    @Test
    fun replayCreatesFreshTargetsScheduleAndScore() {
        val random = FakeRandom(0, 0, 0, 0, 1, 1, 1, 2)
        val game = DualFocusGame(random)
        game.start()
        val firstTargets = game.state.targets
        val firstCardId = game.state.cards.getValue(DualFocusLane.One).id
        game.tap(DualFocusLane.One, firstCardId, nowMillis = 1)

        game.start(nowMillis = 500)

        assertEquals(0, game.state.score)
        assertEquals(500, game.state.cards.getValue(DualFocusLane.One).appearedAtMillis)
        assertNotEquals(firstTargets, game.state.targets)
    }

    private class FakeRandom(vararg values: Int) : DualFocusRandom {
        private val values = values.toMutableList()

        override fun nextInt(until: Int): Int = (if (values.isEmpty()) 0 else values.removeAt(0)).mod(until)
    }

    private class ConstantRandom(private val value: Int) : DualFocusRandom {
        override fun nextInt(until: Int): Int = value.mod(until)
    }
}
