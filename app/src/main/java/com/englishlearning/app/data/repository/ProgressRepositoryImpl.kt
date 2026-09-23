package com.englishlearning.app.data.repository

import com.englishlearning.app.data.database.dao.AchievementDao
import com.englishlearning.app.data.database.dao.ProgressDao
import com.englishlearning.app.data.database.dao.StreakDao
import com.englishlearning.app.data.database.dao.UserProfileDao
import com.englishlearning.app.data.database.entity.AchievementEntity
import com.englishlearning.app.data.database.entity.AttemptEntity
import com.englishlearning.app.data.database.entity.StreakRecordEntity
import com.englishlearning.app.data.database.entity.UserProfileEntity
import com.englishlearning.app.data.database.entity.UserProgressEntity
import com.englishlearning.app.domain.gamification.StreakCalculator
import com.englishlearning.app.domain.model.Achievement
import com.englishlearning.app.domain.model.AttemptResult
import com.englishlearning.app.domain.model.LevelProgress
import com.englishlearning.app.domain.model.StreakRecord
import com.englishlearning.app.domain.model.UserStats
import com.englishlearning.app.domain.model.ValidationStatus
import com.englishlearning.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Implementação de [ProgressRepository] que persiste dados no Room Database e
 * integra [StreakCalculator] para cálculo de streak diário.
 *
 * Requisito 5.1–5.5: rastreamento e cálculo do streak diário via StreakCalculator.
 * Requisito 6.1: progresso por nível para lógica de desbloqueio.
 * Requisito 8.2: desbloqueio de conquistas ao atingir marcos.
 * Requisito 8.5: marcação de conquistas como visualizadas.
 */
class ProgressRepositoryImpl @Inject constructor(
    private val progressDao: ProgressDao,
    private val streakDao: StreakDao,
    private val achievementDao: AchievementDao,
    private val userProfileDao: UserProfileDao,
    private val streakCalculator: StreakCalculator,
) : ProgressRepository {

    // ─── Achievement key constants ───────────────────────────────────────────

    companion object {
        const val KEY_FIRST_CORRECT       = "first_correct"
        const val KEY_STREAK_7            = "streak_7"
        const val KEY_STREAK_30           = "streak_30"
        const val KEY_LEVEL_1_COMPLETE    = "level_1_complete"
        const val KEY_LEVEL_2_COMPLETE    = "level_2_complete"
        const val KEY_ACCURACY_90_SESSION = "accuracy_90_session"
    }

    // ─── updateProgress ──────────────────────────────────────────────────────

    /**
     * Registra a tentativa individual e atualiza contadores de progresso
     * do par (level, category) e o totalXp do perfil do usuário.
     *
     * Requisito 9.3: persiste userAnswer, validationStatus, score, xpEarned e timestamp.
     * Requisito 4.1: XP já calculado no AttemptResult e persistido aqui.
     */
    override suspend fun updateProgress(userId: Long, attemptResult: AttemptResult) {
        val exerciseId = attemptResult.exerciseId
        val result     = attemptResult.validationResult

        // 1. Inserir tentativa individual
        progressDao.insertAttempt(
            AttemptEntity(
                userId           = userId,
                exerciseId       = exerciseId,
                userAnswer       = attemptResult.userAnswer,
                validationStatus = result.status.name,
                score            = result.score,
                xpEarned         = attemptResult.xpEarned,
                attemptedAt      = attemptResult.attemptedAt,
            )
        )

        // 2. Determinar o nível e categoria com base no exercício
        //    Nota: AttemptResult não carrega levelId/categoryId directamente.
        //    A fonte de verdade é o exercício no banco; aqui usamos os IDs do
        //    exercício armazenados em AttemptResult via campo dedicado quando disponível.
        //    Para a atualização de UserProgress precisamos de levelId e categoryId:
        //    eles são obtidos pela tentativa mais recente de mesma estrutura que já existe,
        //    mas como AttemptResult não os tem, delegamos a atualização ao caller
        //    que deve usar updateProgressForExercise. Assim, o updateProgress genérico
        //    apenas persiste a tentativa e atualiza o totalXp do perfil.

        // 3. Atualizar totalXp do UserProfile
        if (attemptResult.xpEarned > 0) {
            val profile = userProfileDao.getProfile(userId).first()
            val current = profile ?: UserProfileEntity(id = userId)
            userProfileDao.upsert(
                current.copy(
                    totalXp      = current.totalXp + attemptResult.xpEarned,
                    lastActiveAt = System.currentTimeMillis(),
                )
            )
        }
    }

    /**
     * Sobrecarga de [updateProgress] que também atualiza o UserProgress para o par
     * (levelId, categoryId). Chamada pelo use case de sessão com contexto completo.
     *
     * Requisito 5.1: registra atividade no dia para o streak.
     * Requisito 6.1: mantém exercisesCompleted e accuracyRate atualizados.
     */
    suspend fun updateProgressWithLevel(
        userId: Long,
        levelId: Int,
        categoryId: Int,
        attemptResult: AttemptResult,
    ) {
        // Delegar para updateProgress primeiro (persiste tentativa + XP do perfil)
        updateProgress(userId, attemptResult)

        val result = attemptResult.validationResult
        val isCorrect = result.status == ValidationStatus.CORRECT

        // Buscar progresso existente ou criar zerado
        val existing = progressDao.getProgressByUserLevelAndCategory(userId, levelId, categoryId)
        val newExercisesCompleted = existing?.exercisesCompleted?.let { prev ->
            // Incrementa apenas quando a resposta é correta (exercício "completado")
            if (isCorrect) prev + 1 else prev
        } ?: (if (isCorrect) 1 else 0)

        val newCorrectAnswers = (existing?.correctAnswers ?: 0) + (if (isCorrect) 1 else 0)
        val newTotalAttempts  = (existing?.totalAttempts ?: 0) + 1
        val newTotalXp        = (existing?.totalXpEarned ?: 0) + attemptResult.xpEarned

        progressDao.upsertProgress(
            UserProgressEntity(
                id                 = existing?.id ?: 0,
                userId             = userId,
                levelId            = levelId,
                categoryId         = categoryId,
                exercisesCompleted = newExercisesCompleted,
                totalAttempts      = newTotalAttempts,
                correctAnswers     = newCorrectAnswers,
                totalXpEarned      = newTotalXp,
                lastAttemptAt      = System.currentTimeMillis(),
            )
        )
    }

    // ─── updateStreak ─────────────────────────────────────────────────────────

    /**
     * Calcula e persiste o novo streak com base na data de hoje.
     * Atualiza longestStreak quando currentStreak o supera.
     *
     * Requisito 5.2: atividade no dia anterior → incrementar.
     * Requisito 5.3: atividade no mesmo dia → manter.
     * Requisito 5.4: atividade há mais de 1 dia → resetar para 1.
     * Requisito 5.5: longestStreak preservado e atualizado.
     */
    override suspend fun updateStreak(userId: Long): StreakRecord {
        val today      = LocalDate.now()
        val entity     = streakDao.getStreak(userId).first()
        val currentStreak    = entity?.currentStreak ?: 0
        val currentLongest   = entity?.longestStreak ?: 0
        val lastActivityDate = entity?.lastActivityDate?.let { LocalDate.parse(it) }

        val newStreakValue: Int
        if (lastActivityDate == null) {
            // Primeira atividade do usuário
            newStreakValue = 1
        } else {
            val streakResult = streakCalculator.calculateNewStreak(
                currentStreak      = currentStreak,
                lastActivityDate   = lastActivityDate,
                today              = today,
            )
            newStreakValue = streakResult.newStreak
        }

        val newLongest = maxOf(currentLongest, newStreakValue)

        val updatedEntity = StreakRecordEntity(
            userId           = userId,
            currentStreak    = newStreakValue,
            longestStreak    = newLongest,
            lastActivityDate = today.toString(),
        )
        streakDao.upsertStreak(updatedEntity)

        return StreakRecord(
            userId           = userId,
            currentStreak    = newStreakValue,
            longestStreak    = newLongest,
            lastActivityDate = today,
        )
    }

    // ─── checkAchievements ───────────────────────────────────────────────────

    /**
     * Verifica todos os marcos de conquistas e insere as novas (conflito ignorado).
     * Retorna apenas as conquistas recém-inseridas nesta chamada.
     *
     * Marcos verificados:
     *  - first_correct: primeira resposta CORRECT no banco.
     *  - streak_7: currentStreak >= 7.
     *  - streak_30: currentStreak >= 30.
     *  - level_1_complete: exercisesCompleted >= 10 com accuracyRate >= 70% no nível 1.
     *  - level_2_complete: idem para nível 2.
     *  - accuracy_90_session: última sessão com 90%+ de acertos (total de respostas corretas
     *    nos últimos exercícios únicos completados).
     *
     * Requisito 8.1: conquistas para marcos específicos.
     * Requisito 8.2: desbloqueia e registra via AchievementDao.IGNORE.
     */
    override suspend fun checkAchievements(userId: Long): List<Achievement> {
        val stats      = progressDao.getOverallStats(userId)
        val streakEntity = streakDao.getStreak(userId).first()
        val currentStreak = streakEntity?.currentStreak ?: 0

        val candidates = mutableListOf<String>()

        // Primeira resposta correta
        if (stats.correctAnswers >= 1) {
            candidates += KEY_FIRST_CORRECT
        }

        // Streaks
        if (currentStreak >= 7)  candidates += KEY_STREAK_7
        if (currentStreak >= 30) candidates += KEY_STREAK_30

        // Completar nível: exercisesCompleted >= 10 e accuracy >= 70%
        checkLevelCompletion(userId, levelId = 1)?.let { candidates += it }
        checkLevelCompletion(userId, levelId = 2)?.let { candidates += it }

        // Precisão de 90% na sessão (usa totais acumulados como aproximação)
        if (stats.totalAttempts > 0) {
            val overallAccuracy = stats.correctAnswers.toFloat() / stats.totalAttempts
            if (overallAccuracy >= 0.90f) candidates += KEY_ACCURACY_90_SESSION
        }

        if (candidates.isEmpty()) return emptyList()

        // Buscar conquistas já existentes para identificar as realmente novas
        val existing = achievementDao.getAchievements(userId).first()
            .map { it.achievementKey }
            .toSet()

        val newlyUnlocked = mutableListOf<Achievement>()
        val now = System.currentTimeMillis()

        for (key in candidates) {
            if (key !in existing) {
                val entity = AchievementEntity(
                    userId         = userId,
                    achievementKey = key,
                    unlockedAt     = now,
                    isNew          = true,
                )
                achievementDao.insert(entity)  // OnConflictStrategy.IGNORE garante idempotência
                newlyUnlocked += Achievement(
                    userId         = userId,
                    achievementKey = key,
                    unlockedAt     = now,
                    isNew          = true,
                )
            }
        }

        return newlyUnlocked
    }

    /**
     * Verifica se o usuário completou um nível (>= 10 exercícios com >= 70% de acerto)
     * e retorna a chave de conquista correspondente, ou null se o critério não foi atingido.
     */
    private suspend fun checkLevelCompletion(userId: Long, levelId: Int): String? {
        val progressList = progressDao.getProgressByUserAndLevel(userId, levelId).first()
        // Agrega progresso de todas as categorias do nível
        val totalExercises = progressList.sumOf { it.exercisesCompleted }
        val totalAttempts  = progressList.sumOf { it.totalAttempts }
        val totalCorrect   = progressList.sumOf { it.correctAnswers }
        val accuracy       = if (totalAttempts > 0) totalCorrect.toFloat() / totalAttempts else 0f

        return if (totalExercises >= 10 && accuracy >= 0.70f) {
            when (levelId) {
                1 -> KEY_LEVEL_1_COMPLETE
                2 -> KEY_LEVEL_2_COMPLETE
                else -> null
            }
        } else {
            null
        }
    }

    // ─── getLevelProgress ────────────────────────────────────────────────────

    /**
     * Retorna o progresso do usuário para um nível e categoria específicos.
     * Cria um objeto zerado quando não há histórico.
     *
     * Requisito 6.5: taxa de progresso por nível na LevelSelectScreen.
     */
    override suspend fun getLevelProgress(userId: Long, levelId: Int, categoryId: Int): LevelProgress {
        val entity = progressDao.getProgressByUserLevelAndCategory(userId, levelId, categoryId)
        return entity?.toDomain() ?: LevelProgress(
            userId             = userId,
            levelId            = levelId,
            categoryId         = categoryId,
            exercisesCompleted = 0,
            totalAttempts      = 0,
            correctAnswers     = 0,
            totalXpEarned      = 0,
        )
    }

    // ─── getOverallStats ─────────────────────────────────────────────────────

    /**
     * Observa estatísticas gerais do usuário como Flow reativo.
     * Combina dados do perfil e streak; os totais de tentativas são agregados
     * via uma query de banco a cada emissão.
     *
     * Requisito 5.6: streak exibido na HomeScreen.
     * Requisito 6.3: informações de nível atual disponíveis na UI.
     */
    override fun getOverallStats(userId: Long): Flow<UserStats> {
        val profileFlow = userProfileDao.getProfile(userId)
        val streakFlow  = streakDao.getStreak(userId)

        return combine(profileFlow, streakFlow) { profile, streakEntity ->
            val currentStreak = streakEntity?.currentStreak ?: 0
            val longestStreak = streakEntity?.longestStreak ?: 0
            val totalXp       = profile?.totalXp ?: 0
            val currentLevel  = profile?.currentLevelId ?: 1

            // Buscar contadores de tentativas de forma suspensa dentro do Flow collector
            val aggStats = progressDao.getOverallStats(userId)

            UserStats(
                userId                  = userId,
                totalXp                 = totalXp,
                totalExercisesCompleted = aggStats.totalExercises,
                totalCorrectAnswers     = aggStats.correctAnswers,
                currentStreak           = currentStreak,
                longestStreak           = longestStreak,
                currentLevelId          = currentLevel,
            )
        }
    }

    // ─── observeAchievements ─────────────────────────────────────────────────

    /**
     * Observa todas as conquistas do usuário como Flow reativo.
     *
     * Requisito 8.4: exibir conquistas na ProgressScreen.
     */
    override fun observeAchievements(userId: Long): Flow<List<Achievement>> {
        return achievementDao.getAchievements(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // ─── markAchievementsAsSeen ───────────────────────────────────────────────

    /**
     * Marca todas as conquistas do usuário como visualizadas.
     *
     * Requisito 8.5: isNew = false após visualização.
     */
    override suspend fun markAchievementsAsSeen(userId: Long) {
        achievementDao.markAllSeen(userId)
    }

    // ─── observeStreak ────────────────────────────────────────────────────────

    /**
     * Observa o registro de streak do usuário como Flow reativo.
     */
    override fun observeStreak(userId: Long): Flow<StreakRecord?> {
        return streakDao.getStreak(userId).map { entity ->
            entity?.let {
                StreakRecord(
                    userId           = it.userId,
                    currentStreak    = it.currentStreak,
                    longestStreak    = it.longestStreak,
                    lastActivityDate = it.lastActivityDate?.let { d -> LocalDate.parse(d) },
                )
            }
        }
    }

    // ─── Mapper extensions ────────────────────────────────────────────────────

    private fun UserProgressEntity.toDomain(): LevelProgress = LevelProgress(
        userId             = userId,
        levelId            = levelId,
        categoryId         = categoryId,
        exercisesCompleted = exercisesCompleted,
        totalAttempts      = totalAttempts,
        correctAnswers     = correctAnswers,
        totalXpEarned      = totalXpEarned,
        lastAttemptAt      = lastAttemptAt,
    )

    private fun AchievementEntity.toDomain(): Achievement = Achievement(
        id             = id,
        userId         = userId,
        achievementKey = achievementKey,
        unlockedAt     = unlockedAt,
        isNew          = isNew,
    )
}
