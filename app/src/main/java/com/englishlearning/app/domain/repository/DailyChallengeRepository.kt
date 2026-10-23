package com.englishlearning.app.domain.repository

import com.englishlearning.app.domain.model.ChallengeType
import com.englishlearning.app.domain.model.DailyChallenge
import com.englishlearning.app.domain.model.DailyChallengeSet
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository para gerenciar desafios diários.
 */
interface DailyChallengeRepository {
    
    /**
     * Obtém os desafios para uma data específica.
     */
    suspend fun getChallengesForDate(userId: Long, date: LocalDate): DailyChallengeSet?
    
    /**
     * Obtém os desafios de hoje como Flow para atualizações em tempo real.
     */
    fun observeTodayChallenges(userId: Long): Flow<DailyChallengeSet?>
    
    /**
     * Salva os desafios gerados.
     */
    suspend fun saveChallenges(userId: Long, challenges: List<DailyChallenge>)
    
    /**
     * Atualiza o progresso de um desafio específico.
     */
    suspend fun updateChallengeProgress(
        userId: Long,
        challengeId: Long,
        newProgress: Int
    )
    
    /**
     * Marca um desafio como completo.
     */
    suspend fun markChallengeCompleted(
        userId: Long,
        challengeId: Long,
        completedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Incrementa progresso para desafios do tipo especificado.
     * Útil para atualizar automaticamente quando o usuário completa exercícios.
     */
    suspend fun incrementProgressForType(
        userId: Long,
        challengeType: ChallengeType,
        incrementBy: Int = 1
    )
    
    /**
     * Obtém estatísticas de desafios completados.
     */
    suspend fun getChallengeStats(userId: Long): ChallengeStats
    
    /**
     * Limpa desafios antigos (mais de 30 dias).
     */
    suspend fun cleanupOldChallenges(userId: Long)
}

/**
 * Estatísticas de desafios.
 */
data class ChallengeStats(
    val totalCompleted: Int,
    val totalXpEarned: Int,
    val currentStreak: Int, // Dias seguidos completando todos os desafios
    val bestStreak: Int,
    val completionRate: Float // % de desafios completados no total
)
