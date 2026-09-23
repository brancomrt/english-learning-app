package com.englishlearning.app.domain.gamification

import com.englishlearning.app.domain.model.LevelProgress
import com.englishlearning.app.domain.model.ValidationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelProgressionAlgorithmTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun progressWith(
        exercisesCompleted: Int,
        correctAnswers: Int,
        totalAttempts: Int = exercisesCompleted
    ) = LevelProgress(
        userId = 1L,
        levelId = 1,
        categoryId = 1,
        exercisesCompleted = exercisesCompleted,
        totalAttempts = totalAttempts,
        correctAnswers = correctAnswers,
        totalXpEarned = 0
    )

    // ─────────────────────────────────────────────────────────────────────────
    // computeNextLevel
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `computeNextLevel returns next level when requirements met`() {
        // 10 exercises, 10 correct → 100% accuracy — well above the 70% threshold
        val progress = progressWith(exercisesCompleted = 10, correctAnswers = 10)
        val result = LevelProgressionAlgorithm.computeNextLevel(
            currentLevelId = 1,
            progress = progress,
            totalLevels = 5
        )
        assertEquals(2, result)
    }

    @Test
    fun `computeNextLevel returns next level at exact minimum threshold`() {
        // 10 exercises, 7 correct → exactly 70% accuracy
        val progress = progressWith(exercisesCompleted = 10, correctAnswers = 7, totalAttempts = 10)
        val result = LevelProgressionAlgorithm.computeNextLevel(
            currentLevelId = 2,
            progress = progress,
            totalLevels = 5
        )
        assertEquals(3, result)
    }

    @Test
    fun `computeNextLevel returns null when not enough exercises completed`() {
        // Only 9 exercises — one short of the required 10
        val progress = progressWith(exercisesCompleted = 9, correctAnswers = 9)
        val result = LevelProgressionAlgorithm.computeNextLevel(
            currentLevelId = 1,
            progress = progress,
            totalLevels = 5
        )
        assertNull(result)
    }

    @Test
    fun `computeNextLevel returns null when accuracy below threshold`() {
        // 10 exercises but only 60% accuracy (below the 70% requirement)
        val progress = progressWith(exercisesCompleted = 10, correctAnswers = 6, totalAttempts = 10)
        val result = LevelProgressionAlgorithm.computeNextLevel(
            currentLevelId = 1,
            progress = progress,
            totalLevels = 5
        )
        assertNull(result)
    }

    @Test
    fun `computeNextLevel returns null when currentLevelId equals totalLevels`() {
        // Already on the last level
        val progress = progressWith(exercisesCompleted = 20, correctAnswers = 20)
        val result = LevelProgressionAlgorithm.computeNextLevel(
            currentLevelId = 5,
            progress = progress,
            totalLevels = 5
        )
        assertNull(result)
    }

    @Test
    fun `computeNextLevel returns null when currentLevelId exceeds totalLevels`() {
        val progress = progressWith(exercisesCompleted = 20, correctAnswers = 20)
        val result = LevelProgressionAlgorithm.computeNextLevel(
            currentLevelId = 6,
            progress = progress,
            totalLevels = 5
        )
        assertNull(result)
    }

    @Test
    fun `computeNextLevel returns null when no exercises completed`() {
        val progress = progressWith(exercisesCompleted = 0, correctAnswers = 0)
        val result = LevelProgressionAlgorithm.computeNextLevel(
            currentLevelId = 1,
            progress = progress,
            totalLevels = 5
        )
        assertNull(result)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // calculatePoints — CORRECT status
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `calculatePoints returns positive value for CORRECT on first attempt without streak`() {
        val points = LevelProgressionAlgorithm.calculatePoints(
            status = ValidationStatus.CORRECT,
            difficultyScore = 5,
            attemptNumber = 1,
            hasActiveStreak = false
        )
        assertTrue(points > 0)
    }

    @Test
    fun `calculatePoints applies streak multiplier for CORRECT with active streak`() {
        val withoutStreak = LevelProgressionAlgorithm.calculatePoints(
            status = ValidationStatus.CORRECT,
            difficultyScore = 5,
            attemptNumber = 1,
            hasActiveStreak = false
        )
        val withStreak = LevelProgressionAlgorithm.calculatePoints(
            status = ValidationStatus.CORRECT,
            difficultyScore = 5,
            attemptNumber = 1,
            hasActiveStreak = true
        )
        // Streak should give at least as many points (Requisito 4.2)
        assertTrue(withStreak >= withoutStreak)
    }

    @Test
    fun `calculatePoints applies attempt penalty for second attempt`() {
        val firstAttempt = LevelProgressionAlgorithm.calculatePoints(
            status = ValidationStatus.CORRECT,
            difficultyScore = 5,
            attemptNumber = 1,
            hasActiveStreak = false
        )
        val secondAttempt = LevelProgressionAlgorithm.calculatePoints(
            status = ValidationStatus.CORRECT,
            difficultyScore = 5,
            attemptNumber = 2,
            hasActiveStreak = false
        )
        // Penalty must reduce points (Requisito 4.3)
        assertTrue(secondAttempt < firstAttempt)
    }

    @Test
    fun `calculatePoints is always non-negative regardless of many attempts`() {
        // After many attempts the penalty might exceed base points — must clamp to 0
        val points = LevelProgressionAlgorithm.calculatePoints(
            status = ValidationStatus.CORRECT,
            difficultyScore = 1,
            attemptNumber = 100,
            hasActiveStreak = false
        )
        assertTrue("Points must be >= 0 (Requisito 4.4)", points >= 0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // calculatePoints — TOO_CLOSE status (Requisito 4.5)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `calculatePoints for TOO_CLOSE is less than for CORRECT under same conditions`() {
        val correct = LevelProgressionAlgorithm.calculatePoints(
            status = ValidationStatus.CORRECT,
            difficultyScore = 5,
            attemptNumber = 1,
            hasActiveStreak = false
        )
        val tooClose = LevelProgressionAlgorithm.calculatePoints(
            status = ValidationStatus.TOO_CLOSE,
            difficultyScore = 5,
            attemptNumber = 1,
            hasActiveStreak = false
        )
        assertTrue(tooClose < correct)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // calculatePoints — INCORRECT status (Requisito 4.6)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `calculatePoints returns 0 for INCORRECT regardless of difficulty and streak`() {
        listOf(true, false).forEach { streak ->
            (1..10).forEach { diff ->
                val points = LevelProgressionAlgorithm.calculatePoints(
                    status = ValidationStatus.INCORRECT,
                    difficultyScore = diff,
                    attemptNumber = 1,
                    hasActiveStreak = streak
                )
                assertEquals(
                    "INCORRECT must always give 0 XP (difficulty=$diff, streak=$streak)",
                    0, points
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // calculatePoints — difficulty scaling
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `calculatePoints scales with difficulty score`() {
        val lowDifficulty = LevelProgressionAlgorithm.calculatePoints(
            status = ValidationStatus.CORRECT,
            difficultyScore = 1,
            attemptNumber = 1,
            hasActiveStreak = false
        )
        val highDifficulty = LevelProgressionAlgorithm.calculatePoints(
            status = ValidationStatus.CORRECT,
            difficultyScore = 10,
            attemptNumber = 1,
            hasActiveStreak = false
        )
        assertTrue(highDifficulty > lowDifficulty)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `constants have expected values`() {
        assertEquals(100, LevelProgressionAlgorithm.BASE_POINTS_CORRECT)
        assertEquals(50, LevelProgressionAlgorithm.BASE_POINTS_PARTIAL)
        assertEquals(20, LevelProgressionAlgorithm.ATTEMPT_PENALTY)
        assertEquals(1.5, LevelProgressionAlgorithm.STREAK_MULTIPLIER, 0.0001)
        assertEquals(10, LevelProgressionAlgorithm.MIN_EXERCISES_TO_ADVANCE)
        assertEquals(0.70f, LevelProgressionAlgorithm.MIN_ACCURACY_TO_ADVANCE, 0.0001f)
    }
}
