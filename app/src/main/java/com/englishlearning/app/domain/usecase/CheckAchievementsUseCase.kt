package com.englishlearning.app.domain.usecase

import com.englishlearning.app.domain.model.Achievement
import com.englishlearning.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Gerencia verificação e observação das conquistas do usuário.
 *
 * Requisito 8.2: desbloqueia conquistas ao atingir marcos.
 * Requisito 8.4: exibir conquistas desbloqueadas e bloqueadas na ProgressScreen.
 */
class CheckAchievementsUseCase @Inject constructor(
    private val progressRepository: ProgressRepository
) {
    /**
     * Observa todas as conquistas do usuário como um fluxo reativo.
     *
     * Emite um novo valor sempre que uma conquista for desbloqueada ou marcada como vista.
     *
     * Requisito 8.4: lista completa de conquistas para exibição na ProgressScreen.
     *
     * @param userId ID do usuário. Padrão: [DEFAULT_USER_ID] para app single-user.
     */
    fun observe(userId: Long = DEFAULT_USER_ID): Flow<List<Achievement>> {
        return progressRepository.observeAchievements(userId)
    }

    /**
     * Verifica marcos de conquistas e retorna as novas conquistas desbloqueadas.
     *
     * Requisito 8.2: desbloqueia e registra conquistas no banco de dados.
     *
     * @param userId ID do usuário. Padrão: [DEFAULT_USER_ID] para app single-user.
     * @return Lista de [Achievement] recém-desbloqueadas (vazia se nenhuma nova).
     */
    suspend operator fun invoke(userId: Long = DEFAULT_USER_ID): List<Achievement> {
        return progressRepository.checkAchievements(userId)
    }

    companion object {
        const val DEFAULT_USER_ID = 1L
    }
}
