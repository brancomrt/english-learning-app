package com.englishlearning.app.data.repository

import com.englishlearning.app.data.database.dao.AchievementDao
import com.englishlearning.app.data.database.dao.ProgressDao
import com.englishlearning.app.data.database.dao.StreakDao
import com.englishlearning.app.data.database.dao.UserProfileDao
import com.englishlearning.app.data.database.dao.UserStatsEntity
import com.englishlearning.app.data.database.entity.AchievementEntity
import com.englishlearning.app.data.database.entity.AttemptEntity
import com.englishlearning.app.data.database.entity.StreakRecordEntity
import com.englishlearning.app.data.database.entity.UserProfileEntity
import com.englishlearning.app.data.database.entity.UserProgressEntity
import com.englishlearning.app.domain.gamification.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Testes unitários para a lógica de verificação de conquistas em [ProgressRepositoryImpl].
 *
 * Requisito 8.1: conquistas para marcos específicos (primeira resposta correta,
 *                streak 7/30 dias, completar nível, precisão 90%).
 * Requisito 8.2: desbloqueia e registra conquistas via AchievementDao com IGNORE.
 */
class AchievementCheckTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Fake DAO implementations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ProgressDao fake que retorna dados configuráveis para os testes de conquistas.
     */
    private inner class FakeProgressDao(
        private val statsEntity: UserStatsEntity,
        private val progressByLevel: Map<Int, List<UserProgressEntity>> = emptyMap()
    ) : ProgressDao {

        override suspend fun upsertProgress(progress: UserProgressEntity) {}

        override fun getProgressByUserAndLevel(
            userId: Long,
            levelId: Int
        ): Flow<List<UserProgressEntity>> =
            flowOf(progressByLevel[levelId] ?: emptyList())

        override suspend fun getProgressByUserLevelAndCategory(
            userId: Long,
            levelId: Int,
            categoryId: Int
        ): UserProgressEntity? = progressByLevel[levelId]?.firstOrNull {
            it.levelId == levelId && it.categoryId == categoryId
        }

        override suspend fun insertAttempt(attempt: AttemptEntity) {}

        override suspend fun getAttemptCount(userId: Long, exerciseId: Long): Int = 0

        override suspend fun getOverallStats(userId: Long): UserStatsEntity = statsEntity

        override fun getUserProfile(userId: Long): Flow<UserProfileEntity?> = flowOf(null)

        override suspend fun upsertProfile(profile: UserProfileEntity) {}
    }

    /**
     * StreakDao fake que expõe o streak configurável.
     */
    private inner class FakeStreakDao(
        private val streakEntity: StreakRecordEntity?
    ) : StreakDao {

        override suspend fun upsertStreak(streak: StreakRecordEntity) {}

        override fun getStreak(userId: Long): Flow<StreakRecordEntity?> =
            flowOf(streakEntity)
    }

    /**
     * AchievementDao fake que registra as conquistas inseridas para inspeção.
     */
    private inner class FakeAchievementDao(
        private val existingAchievements: List<AchievementEntity> = emptyList()
    ) : AchievementDao {

        val inserted = mutableListOf<AchievementEntity>()

        override fun getAchievements(userId: Long): Flow<List<AchievementEntity>> =
            flowOf(existingAchievements + inserted)

        override suspend fun insert(achievement: AchievementEntity) {
            // Simulate OnConflictStrategy.IGNORE: skip if key already exists
            val alreadyExists = existingAchievements.any { it.achievementKey == achievement.achievementKey }
            if (!alreadyExists) {
                inserted += achievement
            }
        }

        override suspend fun markSeen(userId: Long, achievementKey: String) {}
        override suspend fun markAllSeen(userId: Long) {}
    }

    /**
     * UserProfileDao fake mínimo.
     */
    private inner class FakeUserProfileDao : UserProfileDao {
        override fun getProfile(userId: Long): Flow<UserProfileEntity?> = flowOf(null)
        override suspend fun upsert(profile: UserProfileEntity) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Builder helper
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildRepository(
        stats: UserStatsEntity,
        streakEntity: StreakRecordEntity? = null,
        progressByLevel: Map<Int, List<UserProgressEntity>> = emptyMap(),
        existingAchievements: List<AchievementEntity> = emptyList()
    ): Pair<ProgressRepositoryImpl, FakeAchievementDao> {
        val achievementDao = FakeAchievementDao(existingAchievements)
        val repo = ProgressRepositoryImpl(
            progressDao    = FakeProgressDao(stats, progressByLevel),
            streakDao      = FakeStreakDao(streakEntity),
            achievementDao = achievementDao,
            userProfileDao = FakeUserProfileDao(),
            streakCalculator = StreakCalculator(),
        )
        return Pair(repo, achievementDao)
    }

    private fun noStreak(): StreakRecordEntity = StreakRecordEntity(
        userId = 1L,
        currentStreak = 0,
        longestStreak = 0,
        lastActivityDate = null
    )

    private fun streakOf(n: Int): StreakRecordEntity = StreakRecordEntity(
        userId = 1L,
        currentStreak = n,
        longestStreak = n,
        lastActivityDate = "2024-01-01"
    )

    private fun noStats() = UserStatsEntity(
        totalXp = 0,
        totalExercises = 0,
        correctAnswers = 0,
        totalAttempts = 0
    )

    private fun statsWithCorrect(correct: Int, total: Int = correct) = UserStatsEntity(
        totalXp = correct * 10,
        totalExercises = correct,
        correctAnswers = correct,
        totalAttempts = total
    )

    private fun levelProgress(
        levelId: Int,
        exercisesCompleted: Int,
        correctAnswers: Int,
        totalAttempts: Int = exercisesCompleted
    ) = UserProgressEntity(
        id = 0,
        userId = 1L,
        levelId = levelId,
        categoryId = 1,
        exercisesCompleted = exercisesCompleted,
        totalAttempts = totalAttempts,
        correctAnswers = correctAnswers,
        totalXpEarned = 0
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Tests — FIRST_CORRECT (Requisito 8.1)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `checkAchievements unlocks FIRST_CORRECT when user has at least one correct answer`() = runTest {
        val (repo, dao) = buildRepository(
            stats = statsWithCorrect(1),
            streakEntity = noStreak()
        )

        val unlocked = repo.checkAchievements(userId = 1L)

        val keys = unlocked.map { it.achievementKey }
        assertTrue(
            "FIRST_CORRECT should be unlocked when correctAnswers >= 1",
            keys.contains(ProgressRepositoryImpl.KEY_FIRST_CORRECT)
        )
        assertTrue(
            "FIRST_CORRECT should be inserted into DB via AchievementDao",
            dao.inserted.any { it.achievementKey == ProgressRepositoryImpl.KEY_FIRST_CORRECT }
        )
    }

    @Test
    fun `checkAchievements does NOT unlock FIRST_CORRECT when no correct answers`() = runTest {
        val (repo, dao) = buildRepository(
            stats = noStats(),
            streakEntity = noStreak()
        )

        val unlocked = repo.checkAchievements(userId = 1L)

        val keys = unlocked.map { it.achievementKey }
        assertTrue(
            "FIRST_CORRECT should NOT be unlocked with zero correct answers",
            !keys.contains(ProgressRepositoryImpl.KEY_FIRST_CORRECT)
        )
        assertEquals(0, dao.inserted.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests — STREAK_7 (Requisito 8.1)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `checkAchievements unlocks STREAK_7 when currentStreak is exactly 7`() = runTest {
        val (repo, dao) = buildRepository(
            stats = noStats(),
            streakEntity = streakOf(7)
        )

        val unlocked = repo.checkAchievements(userId = 1L)

        assertTrue(
            "STREAK_7 should be unlocked when currentStreak == 7",
            unlocked.map { it.achievementKey }
                .contains(ProgressRepositoryImpl.KEY_STREAK_7)
        )
    }

    @Test
    fun `checkAchievements unlocks STREAK_7 when currentStreak exceeds 7`() = runTest {
        val (repo, _) = buildRepository(
            stats = noStats(),
            streakEntity = streakOf(15)
        )

        val unlocked = repo.checkAchievements(userId = 1L)

        assertTrue(
            "STREAK_7 should be unlocked when currentStreak > 7",
            unlocked.map { it.achievementKey }
                .contains(ProgressRepositoryImpl.KEY_STREAK_7)
        )
    }

    @Test
    fun `checkAchievements does NOT unlock STREAK_7 when currentStreak is 6`() = runTest {
        val (repo, _) = buildRepository(
            stats = noStats(),
            streakEntity = streakOf(6)
        )

        val unlocked = repo.checkAchievements(userId = 1L)

        assertTrue(
            "STREAK_7 should NOT be unlocked when currentStreak < 7",
            !unlocked.map { it.achievementKey }
                .contains(ProgressRepositoryImpl.KEY_STREAK_7)
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests — STREAK_30 (Requisito 8.1)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `checkAchievements unlocks STREAK_30 when currentStreak is exactly 30`() = runTest {
        val (repo, _) = buildRepository(
            stats = noStats(),
            streakEntity = streakOf(30)
        )

        val unlocked = repo.checkAchievements(userId = 1L)

        assertTrue(
            "STREAK_30 should be unlocked when currentStreak == 30",
            unlocked.map { it.achievementKey }
                .contains(ProgressRepositoryImpl.KEY_STREAK_30)
        )
    }

    @Test
    fun `checkAchievements unlocks both STREAK_7 and STREAK_30 when currentStreak is 30`() = runTest {
        val (repo, _) = buildRepository(
            stats = noStats(),
            streakEntity = streakOf(30)
        )

        val keys = repo.checkAchievements(userId = 1L).map { it.achievementKey }

        assertTrue("STREAK_7 should be unlocked", keys.contains(ProgressRepositoryImpl.KEY_STREAK_7))
        assertTrue("STREAK_30 should be unlocked", keys.contains(ProgressRepositoryImpl.KEY_STREAK_30))
    }

    @Test
    fun `checkAchievements does NOT unlock STREAK_30 when currentStreak is 29`() = runTest {
        val (repo, _) = buildRepository(
            stats = noStats(),
            streakEntity = streakOf(29)
        )

        val keys = repo.checkAchievements(userId = 1L).map { it.achievementKey }

        assertTrue(
            "STREAK_30 should NOT be unlocked when currentStreak < 30",
            !keys.contains(ProgressRepositoryImpl.KEY_STREAK_30)
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests — LEVEL_COMPLETE (Requisito 8.1)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `checkAchievements unlocks LEVEL_1_COMPLETE when level 1 progress meets criteria`() = runTest {
        val progress = levelProgress(
            levelId = 1,
            exercisesCompleted = 10,
            correctAnswers = 8,
            totalAttempts = 10
        )
        val (repo, dao) = buildRepository(
            stats = noStats(),
            streakEntity = noStreak(),
            progressByLevel = mapOf(1 to listOf(progress))
        )

        val keys = repo.checkAchievements(userId = 1L).map { it.achievementKey }

        assertTrue(
            "LEVEL_1_COMPLETE should be unlocked with 10 exercises and 80% accuracy",
            keys.contains(ProgressRepositoryImpl.KEY_LEVEL_1_COMPLETE)
        )
        assertTrue(
            "LEVEL_1_COMPLETE should be inserted into DB",
            dao.inserted.any { it.achievementKey == ProgressRepositoryImpl.KEY_LEVEL_1_COMPLETE }
        )
    }

    @Test
    fun `checkAchievements unlocks LEVEL_1_COMPLETE at exact threshold (10 exercises, 70 percent accuracy)`() = runTest {
        val progress = levelProgress(
            levelId = 1,
            exercisesCompleted = 10,
            correctAnswers = 7,
            totalAttempts = 10
        )
        val (repo, _) = buildRepository(
            stats = noStats(),
            streakEntity = noStreak(),
            progressByLevel = mapOf(1 to listOf(progress))
        )

        val keys = repo.checkAchievements(userId = 1L).map { it.achievementKey }

        assertTrue(
            "LEVEL_1_COMPLETE should be unlocked at exactly 10 exercises and 70% accuracy",
            keys.contains(ProgressRepositoryImpl.KEY_LEVEL_1_COMPLETE)
        )
    }

    @Test
    fun `checkAchievements does NOT unlock LEVEL_1_COMPLETE when exercises below minimum`() = runTest {
        val progress = levelProgress(
            levelId = 1,
            exercisesCompleted = 9,
            correctAnswers = 9,
            totalAttempts = 9
        )
        val (repo, _) = buildRepository(
            stats = noStats(),
            streakEntity = noStreak(),
            progressByLevel = mapOf(1 to listOf(progress))
        )

        val keys = repo.checkAchievements(userId = 1L).map { it.achievementKey }

        assertTrue(
            "LEVEL_1_COMPLETE should NOT be unlocked with fewer than 10 exercises",
            !keys.contains(ProgressRepositoryImpl.KEY_LEVEL_1_COMPLETE)
        )
    }

    @Test
    fun `checkAchievements does NOT unlock LEVEL_1_COMPLETE when accuracy below 70 percent`() = runTest {
        val progress = levelProgress(
            levelId = 1,
            exercisesCompleted = 10,
            correctAnswers = 6,  // 60% accuracy — below threshold
            totalAttempts = 10
        )
        val (repo, _) = buildRepository(
            stats = noStats(),
            streakEntity = noStreak(),
            progressByLevel = mapOf(1 to listOf(progress))
        )

        val keys = repo.checkAchievements(userId = 1L).map { it.achievementKey }

        assertTrue(
            "LEVEL_1_COMPLETE should NOT be unlocked below 70% accuracy",
            !keys.contains(ProgressRepositoryImpl.KEY_LEVEL_1_COMPLETE)
        )
    }

    @Test
    fun `checkAchievements unlocks LEVEL_2_COMPLETE when level 2 progress meets criteria`() = runTest {
        val progress = levelProgress(
            levelId = 2,
            exercisesCompleted = 10,
            correctAnswers = 10,
            totalAttempts = 10
        )
        val (repo, _) = buildRepository(
            stats = noStats(),
            streakEntity = noStreak(),
            progressByLevel = mapOf(2 to listOf(progress))
        )

        val keys = repo.checkAchievements(userId = 1L).map { it.achievementKey }

        assertTrue(
            "LEVEL_2_COMPLETE should be unlocked with 10 exercises and >= 70% accuracy",
            keys.contains(ProgressRepositoryImpl.KEY_LEVEL_2_COMPLETE)
        )
    }

    @Test
    fun `checkAchievements aggregates progress across multiple categories in same level`() = runTest {
        // 5 exercises in category 1 + 5 exercises in category 2 = 10 total
        val cat1 = levelProgress(levelId = 1, exercisesCompleted = 5, correctAnswers = 4, totalAttempts = 5)
        val cat2 = UserProgressEntity(
            id = 0, userId = 1L, levelId = 1, categoryId = 2,
            exercisesCompleted = 5, totalAttempts = 5, correctAnswers = 4, totalXpEarned = 0
        )
        val (repo, _) = buildRepository(
            stats = noStats(),
            streakEntity = noStreak(),
            progressByLevel = mapOf(1 to listOf(cat1, cat2))
        )

        val keys = repo.checkAchievements(userId = 1L).map { it.achievementKey }

        assertTrue(
            "LEVEL_1_COMPLETE should aggregate across categories (10 total exercises, 80% accuracy)",
            keys.contains(ProgressRepositoryImpl.KEY_LEVEL_1_COMPLETE)
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests — ACCURACY_90 (Requisito 8.1)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `checkAchievements unlocks ACCURACY_90 when overall accuracy is exactly 90 percent`() = runTest {
        val (repo, dao) = buildRepository(
            stats = UserStatsEntity(
                totalXp = 100,
                totalExercises = 10,
                correctAnswers = 9,
                totalAttempts = 10  // 90% accuracy
            ),
            streakEntity = noStreak()
        )

        val keys = repo.checkAchievements(userId = 1L).map { it.achievementKey }

        assertTrue(
            "ACCURACY_90 should be unlocked with 90% overall accuracy",
            keys.contains(ProgressRepositoryImpl.KEY_ACCURACY_90_SESSION)
        )
        assertTrue(
            "ACCURACY_90 should be inserted into DB",
            dao.inserted.any { it.achievementKey == ProgressRepositoryImpl.KEY_ACCURACY_90_SESSION }
        )
    }

    @Test
    fun `checkAchievements unlocks ACCURACY_90 when accuracy exceeds 90 percent`() = runTest {
        val (repo, _) = buildRepository(
            stats = UserStatsEntity(
                totalXp = 100,
                totalExercises = 10,
                correctAnswers = 10,
                totalAttempts = 10  // 100% accuracy
            ),
            streakEntity = noStreak()
        )

        val keys = repo.checkAchievements(userId = 1L).map { it.achievementKey }

        assertTrue(
            "ACCURACY_90 should be unlocked with 100% accuracy",
            keys.contains(ProgressRepositoryImpl.KEY_ACCURACY_90_SESSION)
        )
    }

    @Test
    fun `checkAchievements does NOT unlock ACCURACY_90 when accuracy is below 90 percent`() = runTest {
        val (repo, _) = buildRepository(
            stats = UserStatsEntity(
                totalXp = 80,
                totalExercises = 10,
                correctAnswers = 8,
                totalAttempts = 10  // 80% accuracy
            ),
            streakEntity = noStreak()
        )

        val keys = repo.checkAchievements(userId = 1L).map { it.achievementKey }

        assertTrue(
            "ACCURACY_90 should NOT be unlocked with 80% accuracy",
            !keys.contains(ProgressRepositoryImpl.KEY_ACCURACY_90_SESSION)
        )
    }

    @Test
    fun `checkAchievements does NOT unlock ACCURACY_90 when no attempts recorded`() = runTest {
        val (repo, _) = buildRepository(
            stats = noStats(),
            streakEntity = noStreak()
        )

        val keys = repo.checkAchievements(userId = 1L).map { it.achievementKey }

        assertTrue(
            "ACCURACY_90 should NOT be unlocked when totalAttempts == 0",
            !keys.contains(ProgressRepositoryImpl.KEY_ACCURACY_90_SESSION)
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests — Idempotency (Requisito 8.2: OnConflictStrategy.IGNORE)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `checkAchievements does NOT re-unlock achievements that already exist in the database`() = runTest {
        val existing = listOf(
            AchievementEntity(
                id = 1L,
                userId = 1L,
                achievementKey = ProgressRepositoryImpl.KEY_FIRST_CORRECT,
                unlockedAt = System.currentTimeMillis() - 1000,
                isNew = false
            )
        )
        val (repo, dao) = buildRepository(
            stats = statsWithCorrect(5),  // Still has correct answers
            streakEntity = noStreak(),
            existingAchievements = existing
        )

        val unlocked = repo.checkAchievements(userId = 1L)

        assertTrue(
            "Already-existing FIRST_CORRECT should NOT be returned as newly unlocked",
            unlocked.none { it.achievementKey == ProgressRepositoryImpl.KEY_FIRST_CORRECT }
        )
        assertTrue(
            "Already-existing FIRST_CORRECT should NOT be re-inserted into DB",
            dao.inserted.none { it.achievementKey == ProgressRepositoryImpl.KEY_FIRST_CORRECT }
        )
    }

    @Test
    fun `checkAchievements returns only newly unlocked achievements, not already existing ones`() = runTest {
        val existing = listOf(
            AchievementEntity(
                id = 1L,
                userId = 1L,
                achievementKey = ProgressRepositoryImpl.KEY_FIRST_CORRECT,
                unlockedAt = System.currentTimeMillis() - 5000,
                isNew = false
            )
        )
        // Conditions also satisfy STREAK_7
        val (repo, _) = buildRepository(
            stats = statsWithCorrect(1),
            streakEntity = streakOf(7),
            existingAchievements = existing
        )

        val unlocked = repo.checkAchievements(userId = 1L)
        val keys = unlocked.map { it.achievementKey }

        assertTrue(
            "FIRST_CORRECT (already existing) should NOT be in returned list",
            !keys.contains(ProgressRepositoryImpl.KEY_FIRST_CORRECT)
        )
        assertTrue(
            "STREAK_7 (new) should be in returned list",
            keys.contains(ProgressRepositoryImpl.KEY_STREAK_7)
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests — Return value correctness (Requisito 8.2)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `checkAchievements returns empty list when no milestone is reached`() = runTest {
        val (repo, _) = buildRepository(
            stats = noStats(),
            streakEntity = noStreak()
        )

        val unlocked = repo.checkAchievements(userId = 1L)

        assertTrue("Result should be empty when no milestones are met", unlocked.isEmpty())
    }

    @Test
    fun `checkAchievements returns multiple achievements when multiple milestones are met simultaneously`() = runTest {
        // All milestones satisfied in one check
        val progress = levelProgress(
            levelId = 1,
            exercisesCompleted = 10,
            correctAnswers = 10,
            totalAttempts = 10
        )
        val (repo, dao) = buildRepository(
            stats = UserStatsEntity(
                totalXp = 200,
                totalExercises = 10,
                correctAnswers = 10,
                totalAttempts = 10
            ),
            streakEntity = streakOf(30),
            progressByLevel = mapOf(1 to listOf(progress))
        )

        val unlocked = repo.checkAchievements(userId = 1L)
        val keys = unlocked.map { it.achievementKey }

        assertTrue("FIRST_CORRECT should be unlocked", keys.contains(ProgressRepositoryImpl.KEY_FIRST_CORRECT))
        assertTrue("STREAK_7 should be unlocked", keys.contains(ProgressRepositoryImpl.KEY_STREAK_7))
        assertTrue("STREAK_30 should be unlocked", keys.contains(ProgressRepositoryImpl.KEY_STREAK_30))
        assertTrue("LEVEL_1_COMPLETE should be unlocked", keys.contains(ProgressRepositoryImpl.KEY_LEVEL_1_COMPLETE))
        assertTrue("ACCURACY_90 should be unlocked", keys.contains(ProgressRepositoryImpl.KEY_ACCURACY_90_SESSION))

        // All should be inserted into the DB
        assertEquals(
            "All 5 distinct achievements should have been inserted",
            5,
            dao.inserted.size
        )
    }

    @Test
    fun `checkAchievements marks all newly inserted achievements as isNew = true`() = runTest {
        val (repo, dao) = buildRepository(
            stats = statsWithCorrect(1),
            streakEntity = noStreak()
        )

        repo.checkAchievements(userId = 1L)

        assertTrue(
            "All newly inserted achievements should have isNew = true",
            dao.inserted.all { it.isNew }
        )
    }

    @Test
    fun `checkAchievements assigns correct userId to all unlocked achievements`() = runTest {
        val targetUserId = 42L
        val (repo, dao) = buildRepository(
            stats = statsWithCorrect(1),
            streakEntity = noStreak()
        )

        repo.checkAchievements(userId = targetUserId)

        assertTrue(
            "All inserted achievements should have the correct userId",
            dao.inserted.all { it.userId == targetUserId }
        )
    }
}
