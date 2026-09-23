package com.englishlearning.app.domain.repository

import com.englishlearning.app.domain.model.Achievement
import com.englishlearning.app.domain.model.AttemptResult
import com.englishlearning.app.domain.model.LevelProgress
import com.englishlearning.app.domain.model.StreakRecord
import com.englishlearning.app.domain.model.UserStats
import kotlinx.coroutines.flow.Flow

/**
 * Abstração de acesso ao progresso do usuário, streak diário e conquistas.
 *
 * Requisito 9.1: persiste UserProgress, StreakRecord e Achievement no banco de dados local.
 * Requisito 5.1–5.5: rastreia atividade diária e calcula streak.
 * Requisito 8.2: desbloqueia conquistas ao atingir marcos.
 *
 * Esta interface é Pure Kotlin — não importa nenhuma classe do Android SDK,
 * permitindo que a camada de domínio permaneça independente de infraestrutura.
 */
interface ProgressRepository {

    /**
     * Atualiza o progresso do usuário com base no resultado de uma tentativa.
     *
     * Persiste a tentativa individual e recalcula `exercisesCompleted`,
     * `totalAttempts`, `correctAnswers` e `totalXpEarned` para o par (level, category).
     *
     * Requisito 9.3: registra a tentativa com userAnswer, validationStatus, score, xpEarned e timestamp.
     * Requisito 4.1: XP calculado e persistido conforme difficultyScore e attemptNumber.
     *
     * @param userId ID do usuário.
     * @param attemptResult Resultado da tentativa a ser persistida.
     */
    suspend fun updateProgress(userId: Long, attemptResult: AttemptResult)

    /**
     * Verifica marcos de conquistas e retorna as novas conquistas desbloqueadas.
     *
     * Verifica marcos como: primeira resposta correta, streak de 7/30 dias,
     * completar um nível, precisão de 90% em uma sessão.
     *
     * Requisito 8.1: conquistas para marcos específicos.
     * Requisito 8.2: desbloqueia e registra a conquista no banco de dados.
     *
     * @param userId ID do usuário.
     * @return Lista de conquistas recém-desbloqueadas (vazias se nenhuma nova).
     */
    suspend fun checkAchievements(userId: Long): List<Achievement>

    /**
     * Atualiza o streak diário do usuário com base na data atual.
     *
     * Aplica as regras de streak:
     * - Mesmo dia → mantém currentStreak.
     * - Dia anterior → incrementa currentStreak.
     * - Mais de 1 dia de diferença → reseta para 1.
     * Atualiza longestStreak quando currentStreak o supera.
     *
     * Requisito 5.2: incrementa streak quando atividade no dia anterior.
     * Requisito 5.3: mantém streak quando atividade no mesmo dia.
     * Requisito 5.4: reseta streak quando inativo por mais de 1 dia.
     * Requisito 5.5: preserva e atualiza longestStreak.
     *
     * @param userId ID do usuário.
     * @return O registro de streak atualizado com newStreak e longestStreak.
     */
    suspend fun updateStreak(userId: Long): StreakRecord

    /**
     * Retorna o progresso do usuário para um nível e categoria específicos.
     *
     * Requisito 6.5: taxa de progresso por nível na LevelSelectScreen.
     *
     * @param userId ID do usuário.
     * @param levelId ID do nível desejado.
     * @param categoryId ID da categoria desejada.
     * @return O progresso atual, ou um objeto zerado se não houver histórico.
     */
    suspend fun getLevelProgress(userId: Long, levelId: Int, categoryId: Int): LevelProgress

    /**
     * Observa as estatísticas gerais do usuário como um fluxo reativo.
     *
     * Emite um novo valor sempre que progresso, streak ou XP for atualizado.
     * Usado pelo HomeViewModel para manter a UI sincronizada.
     *
     * Requisito 5.6: streak exibido na HomeScreen.
     * Requisito 6.3: informações de nível atual disponíveis na UI.
     *
     * @param userId ID do usuário.
     * @return Flow que emite UserStats atualizadas continuamente.
     */
    fun getOverallStats(userId: Long): Flow<UserStats>

    /**
     * Observa todas as conquistas do usuário como um fluxo reativo.
     *
     * Emite um novo valor sempre que uma conquista for desbloqueada ou marcada como vista.
     *
     * Requisito 8.4: exibir conquistas desbloqueadas e bloqueadas na ProgressScreen.
     *
     * @param userId ID do usuário.
     * @return Flow que emite a lista completa de conquistas em ordem cronológica decrescente.
     */
    fun observeAchievements(userId: Long): Flow<List<Achievement>>

    /**
     * Marca todas as conquistas do usuário como visualizadas (`isNew = false`).
     *
     * Requisito 8.5: isNew atualizado para false após visualização.
     *
     * @param userId ID do usuário.
     */
    suspend fun markAchievementsAsSeen(userId: Long)

    /**
     * Observa o registro de streak do usuário como um fluxo reativo.
     *
     * @param userId ID do usuário.
     * @return Flow que emite o StreakRecord atualizado continuamente.
     */
    fun observeStreak(userId: Long): Flow<StreakRecord?>
}
