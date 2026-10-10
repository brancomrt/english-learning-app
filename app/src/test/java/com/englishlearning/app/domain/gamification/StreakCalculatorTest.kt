package com.englishlearning.app.domain.gamification

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Testes unitários para [StreakCalculator].
 *
 * Complementa [StreakCalculatorPropertyTest] com casos determinísticos e legíveis.
 * Requisitos: 5.2 (incrementar), 5.3 (manter), 5.4 (resetar), 5.5 (longestStreak).
 */
class StreakCalculatorTest {

    private lateinit var calculator: StreakCalculator
    private val today = LocalDate.of(2026, 10, 10)

    @Before
    fun setUp() {
        calculator = StreakCalculator()
    }

    // ── Requisito 5.3: mesmo dia ────────────────────────────────────────────

    @Test
    fun `same day activity keeps streak unchanged`() {
        val result = calculator.calculateNewStreak(
            currentStreak = 5,
            lastActivityDate = today,
            today = today
        )
        assertEquals(5, result.newStreak)
        assertFalse(result.streakBroken)
        assertFalse(result.isNewDay)
    }

    @Test
    fun `same day with streak of 1 stays at 1`() {
        val result = calculator.calculateNewStreak(
            currentStreak = 1,
            lastActivityDate = today,
            today = today
        )
        assertEquals(1, result.newStreak)
        assertFalse(result.streakBroken)
        assertFalse(result.isNewDay)
    }

    // ── Requisito 5.2: dia anterior incrementa ──────────────────────────────

    @Test
    fun `activity on previous day increments streak by 1`() {
        val yesterday = today.minusDays(1)
        val result = calculator.calculateNewStreak(
            currentStreak = 3,
            lastActivityDate = yesterday,
            today = today
        )
        assertEquals(4, result.newStreak)
        assertFalse(result.streakBroken)
        assertTrue(result.isNewDay)
    }

    @Test
    fun `first day (streak=0) incremented to 1 when active on previous day`() {
        val yesterday = today.minusDays(1)
        val result = calculator.calculateNewStreak(
            currentStreak = 0,
            lastActivityDate = yesterday,
            today = today
        )
        assertEquals(1, result.newStreak)
        assertFalse(result.streakBroken)
        assertTrue(result.isNewDay)
    }

    // ── Requisito 5.4: mais de 1 dia — resetar ──────────────────────────────

    @Test
    fun `gap of 2 days resets streak to 1`() {
        val twoDaysAgo = today.minusDays(2)
        val result = calculator.calculateNewStreak(
            currentStreak = 10,
            lastActivityDate = twoDaysAgo,
            today = today
        )
        assertEquals(1, result.newStreak)
        assertTrue(result.streakBroken)
        assertTrue(result.isNewDay)
    }

    @Test
    fun `gap of 7 days resets streak to 1`() {
        val sevenDaysAgo = today.minusDays(7)
        val result = calculator.calculateNewStreak(
            currentStreak = 30,
            lastActivityDate = sevenDaysAgo,
            today = today
        )
        assertEquals(1, result.newStreak)
        assertTrue(result.streakBroken)
        assertTrue(result.isNewDay)
    }

    @Test
    fun `streak broken is false when resetting from streak of 1`() {
        // Streak was already 1 — not considered "broken" since there was nothing to lose
        val twoDaysAgo = today.minusDays(2)
        val result = calculator.calculateNewStreak(
            currentStreak = 1,
            lastActivityDate = twoDaysAgo,
            today = today
        )
        assertEquals(1, result.newStreak)
        assertFalse("streakBroken should be false when previous streak was 1", result.streakBroken)
    }

    @Test
    fun `streak broken is true when resetting from streak greater than 1`() {
        val twoDaysAgo = today.minusDays(2)
        val result = calculator.calculateNewStreak(
            currentStreak = 2,
            lastActivityDate = twoDaysAgo,
            today = today
        )
        assertTrue(result.streakBroken)
    }

    // ── Invariants ──────────────────────────────────────────────────────────

    @Test
    fun `newStreak is always at least 1`() {
        val farPast = today.minusDays(365)
        val result = calculator.calculateNewStreak(
            currentStreak = 0,
            lastActivityDate = farPast,
            today = today
        )
        assertTrue(result.newStreak >= 1)
    }

    @Test
    fun `newStreak never negative`() {
        listOf(0, 1, 5, 100).forEach { streak ->
            val result = calculator.calculateNewStreak(
                currentStreak = streak,
                lastActivityDate = today.minusDays(5),
                today = today
            )
            assertTrue("newStreak must be >= 0 for streak=$streak", result.newStreak >= 0)
        }
    }
}
