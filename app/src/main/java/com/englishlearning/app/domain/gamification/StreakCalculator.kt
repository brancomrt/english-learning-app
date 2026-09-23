package com.englishlearning.app.domain.gamification

import com.englishlearning.app.domain.model.StreakResult
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Calcula o streak diário do usuário com base na data da última atividade.
 *
 * Requisito 5.2: atividade no dia anterior → incrementar streak.
 * Requisito 5.3: atividade no mesmo dia → manter streak.
 * Requisito 5.4: atividade há mais de 1 dia → resetar para 1.
 * Requisito 5.5: longestStreak preservado (gerenciado pelo chamador com base no StreakResult).
 */
class StreakCalculator {

    /**
     * Calcula o novo streak com base na data da última atividade e na data atual.
     *
     * @param currentStreak streak atual do usuário (>= 0)
     * @param lastActivityDate data da última atividade registrada
     * @param today data de hoje (padrão: LocalDate.now())
     * @return StreakResult com o novo valor de streak, se foi quebrado e se é um novo dia
     */
    fun calculateNewStreak(
        currentStreak: Int,
        lastActivityDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): StreakResult {
        val daysDiff = ChronoUnit.DAYS.between(lastActivityDate, today)

        return when (daysDiff) {
            0L -> StreakResult(
                newStreak = currentStreak,
                streakBroken = false,
                isNewDay = false
            )
            1L -> StreakResult(
                newStreak = currentStreak + 1,
                streakBroken = false,
                isNewDay = true
            )
            else -> StreakResult(
                newStreak = 1,
                streakBroken = currentStreak > 1,
                isNewDay = true
            )
        }
    }
}
