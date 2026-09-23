package com.englishlearning.app.domain.gamification

import com.englishlearning.app.domain.model.LevelProgress
import com.englishlearning.app.domain.model.ValidationStatus

/**
 * Algoritmo de progressão de níveis e cálculo de pontuação/XP.
 *
 * Requisitos:
 *  - 4.1: XP calculado baseado em difficultyScore, tentativas e streak.
 *  - 4.2: Multiplicador de 1.5x quando streak ativo.
 *  - 4.3: Penalidade de 20 XP por tentativa adicional além da primeira.
 *  - 4.4: XP calculado é sempre >= 0.
 *  - 4.5: ValidationStatus.TOO_CLOSE concede metade do XP correto.
 *  - 4.6: ValidationStatus.INCORRECT concede 0 XP.
 *  - 6.2: Próximo nível desbloqueado ao completar >= 10 exercícios com >= 70% de acerto.
 */
object LevelProgressionAlgorithm {

    /** Pontos base para resposta totalmente correta. */
    const val BASE_POINTS_CORRECT = 100

    /** Pontos base para resposta parcialmente correta (TOO_CLOSE). */
    const val BASE_POINTS_PARTIAL = 50

    /**
     * Penalidade de XP por cada tentativa adicional além da primeira.
     * Requisito 4.3.
     */
    const val ATTEMPT_PENALTY = 20

    /**
     * Multiplicador aplicado ao XP quando o usuário possui streak ativo.
     * Requisito 4.2: fator de 1.5x.
     */
    const val STREAK_MULTIPLIER = 1.5

    /**
     * Número mínimo de exercícios completados para avançar de nível.
     * Requisito 6.2.
     */
    const val MIN_EXERCISES_TO_ADVANCE = 10

    /**
     * Taxa de acerto mínima para avançar de nível.
     * Requisito 6.2: >= 70%.
     */
    const val MIN_ACCURACY_TO_ADVANCE = 0.70f

    /**
     * Determina o próximo nível a ser desbloqueado com base no progresso atual.
     *
     * Precondição: [progress.accuracyRate] está no intervalo [0.0, 1.0].
     * Postcondição: retorna o próximo [currentLevelId + 1] se os critérios forem atendidos,
     *               ou `null` se o usuário já está no nível máximo ou ainda não atingiu
     *               os requisitos mínimos.
     *
     * @param currentLevelId ID do nível atual do usuário.
     * @param progress       Progresso do usuário no nível atual.
     * @param totalLevels    Número total de níveis disponíveis.
     * @return Próximo levelId, ou `null` se não for possível avançar.
     */
    fun computeNextLevel(
        currentLevelId: Int,
        progress: LevelProgress,
        totalLevels: Int
    ): Int? {
        if (currentLevelId >= totalLevels) return null

        val canProgress = progress.exercisesCompleted >= MIN_EXERCISES_TO_ADVANCE
            && progress.accuracyRate >= MIN_ACCURACY_TO_ADVANCE

        return if (canProgress) currentLevelId + 1 else null
    }

    /**
     * Calcula os pontos/XP ganhos em uma tentativa.
     *
     * Postcondição: resultado >= 0 (Requisito 4.4).
     *
     * @param status          Status de validação da resposta do usuário.
     * @param difficultyScore Dificuldade do exercício, no intervalo [1, 10].
     * @param attemptNumber   Número da tentativa (começa em 1).
     * @param hasActiveStreak Indica se o usuário possui streak ativo.
     * @return Pontos ganhos, sempre >= 0.
     */
    fun calculatePoints(
        status: ValidationStatus,
        difficultyScore: Int,       // 1-10
        attemptNumber: Int,         // começa em 1
        hasActiveStreak: Boolean
    ): Int {
        // Pontos base conforme status (Requisitos 4.5 e 4.6)
        val basePoints = when (status) {
            ValidationStatus.CORRECT   -> BASE_POINTS_CORRECT
            ValidationStatus.TOO_CLOSE -> BASE_POINTS_PARTIAL
            ValidationStatus.PARTIAL   -> BASE_POINTS_PARTIAL / 2
            ValidationStatus.INCORRECT -> 0
        }

        // Multiplicador de dificuldade: 1.0 para score 1, até 1.9 para score 10
        val difficultyMultiplier = 1.0 + (difficultyScore - 1) * 0.1

        // Penalidade por tentativas adicionais (Requisito 4.3)
        val attemptPenalty = maxOf(0, (attemptNumber - 1) * ATTEMPT_PENALTY)

        // Bônus de streak (Requisito 4.2)
        val streakBonus = if (hasActiveStreak) STREAK_MULTIPLIER else 1.0

        // Garante que o resultado nunca seja negativo (Requisito 4.4)
        return maxOf(
            0,
            ((basePoints - attemptPenalty) * difficultyMultiplier * streakBonus).toInt()
        )
    }
}
