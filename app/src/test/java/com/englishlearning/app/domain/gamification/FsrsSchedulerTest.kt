package com.englishlearning.app.domain.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [FsrsScheduler].
 *
 * Covers the three FSRS invariants required for task 18.2:
 *  - Requisito 16.3: schedule accepts SrsRating and returns FsrsScheduleResult.
 *  - Requisito 16.4: GOOD/EASY in REVIEW → interval grows monotonically.
 *  - Requisito 16.5: AGAIN always resets interval to 1.
 */
class FsrsSchedulerTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private val today = LocalDate.of(2024, 1, 15)

    private fun newCard(
        stability: Float = 2.4f,
        difficulty: Float = 5f,
        elapsedDays: Int = 0,
        scheduledDays: Int = 1,
        reps: Int = 0,
        lapses: Int = 0,
        state: SrsState = SrsState.NEW
    ) = SrsCardParams(
        stability = stability,
        difficulty = difficulty,
        elapsedDays = elapsedDays,
        scheduledDays = scheduledDays,
        reps = reps,
        lapses = lapses,
        state = state
    )

    private fun reviewCard(
        stability: Float = 10f,
        difficulty: Float = 5f,
        elapsedDays: Int = 10,
        scheduledDays: Int = 10
    ) = SrsCardParams(
        stability = stability,
        difficulty = difficulty,
        elapsedDays = elapsedDays,
        scheduledDays = scheduledDays,
        reps = 3,
        lapses = 0,
        state = SrsState.REVIEW
    )

    // ─────────────────────────────────────────────────────────────────────────
    // SrsRating enum
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `SrsRating values have correct integer mappings`() {
        assertEquals(1, SrsRating.AGAIN.value)
        assertEquals(2, SrsRating.HARD.value)
        assertEquals(3, SrsRating.GOOD.value)
        assertEquals(4, SrsRating.EASY.value)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SrsState enum
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `SrsState has all four required values`() {
        val states = SrsState.values().toSet()
        assertTrue(SrsState.NEW in states)
        assertTrue(SrsState.LEARNING in states)
        assertTrue(SrsState.REVIEW in states)
        assertTrue(SrsState.RELEARNING in states)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // retrievability
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `retrievability returns 1 when elapsedDays is 0`() {
        val r = FsrsScheduler.retrievability(stability = 10f, elapsedDays = 0)
        assertEquals(1.0f, r, 0.001f)
    }

    @Test
    fun `retrievability decreases as elapsed days increase`() {
        val r1 = FsrsScheduler.retrievability(stability = 10f, elapsedDays = 5)
        val r2 = FsrsScheduler.retrievability(stability = 10f, elapsedDays = 10)
        assertTrue("Retrievability should decrease with elapsed time", r1 > r2)
    }

    @Test
    fun `retrievability stays in 0 to 1 range for large elapsed days`() {
        val r = FsrsScheduler.retrievability(stability = 1f, elapsedDays = 10_000)
        assertTrue(r >= 0f)
        assertTrue(r <= 1f)
    }

    @Test
    fun `retrievability returns 0 for zero stability`() {
        val r = FsrsScheduler.retrievability(stability = 0f, elapsedDays = 5)
        assertEquals(0f, r, 0.001f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initDifficulty
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `initDifficulty returns decreasing difficulty for better ratings`() {
        val dAgain = FsrsScheduler.initDifficulty(SrsRating.AGAIN)
        val dHard  = FsrsScheduler.initDifficulty(SrsRating.HARD)
        val dGood  = FsrsScheduler.initDifficulty(SrsRating.GOOD)
        val dEasy  = FsrsScheduler.initDifficulty(SrsRating.EASY)

        assertTrue("AGAIN > HARD", dAgain > dHard)
        assertTrue("HARD > GOOD",  dHard  > dGood)
        assertTrue("GOOD > EASY",  dGood  > dEasy)
    }

    @Test
    fun `initDifficulty is clamped in 1 to 10`() {
        SrsRating.values().forEach { rating ->
            val d = FsrsScheduler.initDifficulty(rating)
            assertTrue("initDifficulty($rating) = $d should be in [1,10]", d in 1f..10f)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateDifficulty
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `updateDifficulty increases for AGAIN and HARD`() {
        val base = 5f
        assertTrue(FsrsScheduler.updateDifficulty(base, SrsRating.AGAIN) > base)
        assertTrue(FsrsScheduler.updateDifficulty(base, SrsRating.HARD)  > base)
    }

    @Test
    fun `updateDifficulty stays the same for GOOD`() {
        val base = 5f
        assertEquals(base, FsrsScheduler.updateDifficulty(base, SrsRating.GOOD), 0.001f)
    }

    @Test
    fun `updateDifficulty decreases for EASY`() {
        val base = 5f
        assertTrue(FsrsScheduler.updateDifficulty(base, SrsRating.EASY) < base)
    }

    @Test
    fun `updateDifficulty result is clamped in 1 to 10`() {
        val nearMax = 9.9f
        val nearMin = 1.1f
        assertTrue(FsrsScheduler.updateDifficulty(nearMax, SrsRating.AGAIN) <= 10f)
        assertTrue(FsrsScheduler.updateDifficulty(nearMin, SrsRating.EASY)  >= 1f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // nextInterval
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `nextInterval is at least 1`() {
        assertTrue(FsrsScheduler.nextInterval(0.1f) >= 1)
    }

    @Test
    fun `nextInterval grows with stability`() {
        val i1 = FsrsScheduler.nextInterval(1f)
        val i2 = FsrsScheduler.nextInterval(10f)
        val i3 = FsrsScheduler.nextInterval(100f)
        assertTrue(i2 > i1)
        assertTrue(i3 > i2)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // recallStability
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `recallStability is greater than current stability`() {
        val s0 = 10f
        val s1 = FsrsScheduler.recallStability(
            difficulty = 5f,
            stability = s0,
            retrievability = 0.9f
        )
        assertTrue("recallStability should exceed current stability", s1 > s0)
    }

    @Test
    fun `recallStability is higher for lower difficulty`() {
        val sLow  = FsrsScheduler.recallStability(difficulty = 2f, stability = 10f, retrievability = 0.9f)
        val sHigh = FsrsScheduler.recallStability(difficulty = 8f, stability = 10f, retrievability = 0.9f)
        assertTrue("Lower difficulty → greater stability gain", sLow > sHigh)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // forgottenStability
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `forgottenStability is at least 0_1`() {
        val s = FsrsScheduler.forgottenStability(
            difficulty = 5f,
            stability = 10f,
            retrievability = 0.9f
        )
        assertTrue("forgottenStability should be at least 0.1", s >= 0.1f)
    }

    @Test
    fun `forgottenStability is less than current stability`() {
        val s0 = 20f
        val s1 = FsrsScheduler.forgottenStability(
            difficulty = 5f,
            stability = s0,
            retrievability = 0.9f
        )
        assertTrue("forgottenStability should be less than current stability", s1 < s0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // schedule — NEW / LEARNING / RELEARNING states
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `schedule for NEW AGAIN returns interval 1 and LEARNING state`() {
        val result = FsrsScheduler.schedule(newCard(), SrsRating.AGAIN, today)

        assertEquals(1, result.scheduledDays)
        assertEquals(SrsState.LEARNING, result.newState)
        assertEquals(today.plusDays(1), result.dueDate)
    }

    @Test
    fun `schedule for NEW GOOD returns interval at least 1 and REVIEW state`() {
        val result = FsrsScheduler.schedule(newCard(), SrsRating.GOOD, today)

        assertTrue(result.scheduledDays >= 1)
        assertEquals(SrsState.REVIEW, result.newState)
        assertEquals(today.plusDays(result.scheduledDays.toLong()), result.dueDate)
    }

    @Test
    fun `schedule for NEW EASY returns interval at least 4 and REVIEW state`() {
        val result = FsrsScheduler.schedule(newCard(), SrsRating.EASY, today)

        assertTrue(result.scheduledDays >= 4)
        assertEquals(SrsState.REVIEW, result.newState)
    }

    @Test
    fun `schedule for LEARNING state behaves like NEW`() {
        val card = newCard(state = SrsState.LEARNING)
        val result = FsrsScheduler.schedule(card, SrsRating.GOOD, today)

        assertTrue(result.scheduledDays >= 1)
        assertEquals(SrsState.REVIEW, result.newState)
    }

    @Test
    fun `schedule for RELEARNING AGAIN returns interval 1`() {
        val card = newCard(state = SrsState.RELEARNING)
        val result = FsrsScheduler.schedule(card, SrsRating.AGAIN, today)

        assertEquals(1, result.scheduledDays)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // schedule — REVIEW state (Requisitos 16.4 e 16.5)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `AGAIN in REVIEW resets interval to 1 — Requisito 16_5`() {
        val card = reviewCard(scheduledDays = 30)
        val result = FsrsScheduler.schedule(card, SrsRating.AGAIN, today)

        assertEquals("AGAIN must reset interval to 1", 1, result.scheduledDays)
        assertEquals("AGAIN must set state to RELEARNING", SrsState.RELEARNING, result.newState)
    }

    @Test
    fun `AGAIN in REVIEW due date is tomorrow`() {
        val card = reviewCard()
        val result = FsrsScheduler.schedule(card, SrsRating.AGAIN, today)

        assertEquals(today.plusDays(1), result.dueDate)
    }

    @Test
    fun `GOOD in REVIEW produces interval greater than previous — Requisito 16_4`() {
        val card = reviewCard(scheduledDays = 10)
        val result = FsrsScheduler.schedule(card, SrsRating.GOOD, today)

        assertTrue(
            "GOOD must increase interval (got ${result.scheduledDays}, expected > 10)",
            result.scheduledDays > 10
        )
        assertEquals(SrsState.REVIEW, result.newState)
    }

    @Test
    fun `EASY in REVIEW produces interval greater than previous — Requisito 16_4`() {
        val card = reviewCard(scheduledDays = 10)
        val result = FsrsScheduler.schedule(card, SrsRating.EASY, today)

        assertTrue(
            "EASY must increase interval (got ${result.scheduledDays}, expected > 10)",
            result.scheduledDays > 10
        )
        assertEquals(SrsState.REVIEW, result.newState)
    }

    @Test
    fun `HARD in REVIEW keeps state as REVIEW`() {
        val card = reviewCard()
        val result = FsrsScheduler.schedule(card, SrsRating.HARD, today)

        assertEquals(SrsState.REVIEW, result.newState)
    }

    @Test
    fun `EASY interval is at least as large as GOOD interval in REVIEW`() {
        val card = reviewCard()
        val goodResult = FsrsScheduler.schedule(card, SrsRating.GOOD, today)
        val easyResult = FsrsScheduler.schedule(card, SrsRating.EASY, today)

        assertTrue(
            "EASY interval (${easyResult.scheduledDays}) should be >= GOOD (${goodResult.scheduledDays})",
            easyResult.scheduledDays >= goodResult.scheduledDays
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FsrsScheduleResult fields
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `dueDate equals today plus scheduledDays`() {
        SrsRating.values().forEach { rating ->
            val card = reviewCard()
            val result = FsrsScheduler.schedule(card, rating, today)
            assertEquals(
                "dueDate mismatch for $rating",
                today.plusDays(result.scheduledDays.toLong()),
                result.dueDate
            )
        }
    }

    @Test
    fun `newStability is positive after any rating`() {
        SrsRating.values().forEach { rating ->
            val card = reviewCard()
            val result = FsrsScheduler.schedule(card, rating, today)
            assertTrue("newStability must be positive for $rating", result.newStability > 0f)
        }
    }

    @Test
    fun `newDifficulty is in 1 to 10 after any rating`() {
        SrsRating.values().forEach { rating ->
            val card = reviewCard()
            val result = FsrsScheduler.schedule(card, rating, today)
            assertTrue(
                "newDifficulty must be in [1,10] for $rating",
                result.newDifficulty in 1f..10f
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // scheduledDays is always >= 1
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `scheduledDays is always at least 1 for all ratings and states`() {
        val cards = listOf(
            newCard(state = SrsState.NEW),
            newCard(state = SrsState.LEARNING),
            newCard(state = SrsState.RELEARNING),
            reviewCard()
        )
        cards.forEach { card ->
            SrsRating.values().forEach { rating ->
                val result = FsrsScheduler.schedule(card, rating, today)
                assertTrue(
                    "scheduledDays must be >= 1 for state=${card.state}, rating=$rating",
                    result.scheduledDays >= 1
                )
            }
        }
    }
}
