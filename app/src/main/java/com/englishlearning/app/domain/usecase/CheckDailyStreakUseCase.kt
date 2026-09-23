package com.englishlearning.app.domain.usecase

import com.englishlearning.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Observa o streak diário atual do usuário como um fluxo reativo.
 *
 * Requisito 5.6: streak exibido prominentemente na HomeScreen.
 */
class CheckDailyStreakUseCase @Inject constructor(
    private val progressRepository: ProgressRepository
) {
    /**
     * Retorna um Flow que emite o [currentStreak] sempre que o registro de streak for atualizado.
     *
     * Emite 0 quando não há registro de streak (primeiro acesso do usuário).
     *
     * @param userId ID do usuário. Padrão: [DEFAULT_USER_ID] para app single-user.
     */
    operator fun invoke(userId: Long = DEFAULT_USER_ID): Flow<Int> {
        return progressRepository.observeStreak(userId)
            .map { streakRecord -> streakRecord?.currentStreak ?: 0 }
    }

    companion object {
        const val DEFAULT_USER_ID = 1L
    }
}
