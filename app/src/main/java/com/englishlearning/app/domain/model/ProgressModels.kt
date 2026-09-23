package com.englishlearning.app.domain.model

import java.time.LocalDate

/**
 * Estatísticas gerais do usuário.
 * Requisito 5.6: streak exibido na HomeScreen.
 */
data class UserStats(
    val userId: Long,
    val totalXp: Int,
    val totalExercisesCompleted: Int,
    val totalCorrectAnswers: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val currentLevelId: Int
) {
    val overallAccuracyRate: Float
        get() = if (totalExercisesCompleted > 0)
            totalCorrectAnswers.toFloat() / totalExercisesCompleted
        else 0f
}

/**
 * Progresso do usuário em um nível/categoria específicos.
 * Requisito 6.5: taxa de progresso por nível na LevelSelectScreen.
 */
data class LevelProgress(
    val userId: Long,
    val levelId: Int,
    val categoryId: Int,
    val exercisesCompleted: Int,
    val totalAttempts: Int,
    val correctAnswers: Int,
    val totalXpEarned: Int,
    val lastAttemptAt: Long = 0L
) {
    val accuracyRate: Float
        get() = if (totalAttempts > 0) correctAnswers.toFloat() / totalAttempts else 0f
}

/**
 * Conquista desbloqueada pelo usuário ao atingir um marco.
 * Requisito 8.1: conquistas para marcos específicos.
 * Requisito 8.5: isNew indica conquistas não visualizadas.
 */
data class Achievement(
    val id: Long = 0,
    val userId: Long,
    val achievementKey: String,  // Ex: "first_correct", "streak_7", "level_2_complete"
    val unlockedAt: Long = System.currentTimeMillis(),
    val isNew: Boolean = true
)

/**
 * Resultado do cálculo do streak diário.
 * Requisito 5.2–5.5: comportamento do streak conforme diferença de dias.
 */
data class StreakResult(
    val newStreak: Int,
    val streakBroken: Boolean,
    val isNewDay: Boolean
)

/**
 * Registro do streak do usuário no banco de dados.
 * Requisito 5.5: longestStreak preservado e atualizado quando currentStreak o supera.
 */
data class StreakRecord(
    val userId: Long,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActivityDate: LocalDate? = null
)
