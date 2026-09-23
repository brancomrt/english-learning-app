package com.englishlearning.app.domain.usecase

import com.englishlearning.app.domain.model.UserStats
import com.englishlearning.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observa as estatísticas gerais do usuário como um fluxo reativo.
 *
 * Requisito 5.6: streak exibido na HomeScreen.
 * Requisito 6.3: informações de nível atual disponíveis na UI.
 */
class GetUserStatsUseCase @Inject constructor(
    private val progressRepository: ProgressRepository
) {
    /**
     * Retorna um Flow que emite [UserStats] sempre que o progresso for atualizado.
     *
     * @param userId ID do usuário. Padrão: [DEFAULT_USER_ID] para app single-user.
     */
    operator fun invoke(userId: Long = DEFAULT_USER_ID): Flow<UserStats> {
        return progressRepository.getOverallStats(userId)
    }

    companion object {
        const val DEFAULT_USER_ID = 1L
    }
}
