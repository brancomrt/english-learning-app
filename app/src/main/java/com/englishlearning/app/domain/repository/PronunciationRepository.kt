package com.englishlearning.app.domain.repository

import com.englishlearning.app.domain.model.PronunciationResult
import com.englishlearning.app.domain.model.PronunciationStatistics
import kotlinx.coroutines.flow.Flow

/**
 * Repository para avaliação de pronúncia.
 */
interface PronunciationRepository {
    
    /**
     * Obtém frases para praticar pronúncia.
     * @param levelId ID do nível para filtrar exercícios
     * @param count quantidade de frases a retornar
     */
    suspend fun getSentencesForPractice(levelId: Int = 1, count: Int = 15): List<String>
    
    /**
     * Salva resultado de avaliação.
     */
    suspend fun saveResult(userId: Long, result: PronunciationResult)
    
    /**
     * Obtém resultados recentes.
     */
    suspend fun getRecentResults(userId: Long, limit: Int = 50): List<PronunciationResult>
    
    /**
     * Obtém estatísticas de pronúncia.
     */
    suspend fun getStatistics(userId: Long): PronunciationStatistics
    
    /**
     * Obtém palavras mais erradas.
     */
    suspend fun getCommonErrors(userId: Long): List<String>
    
    /**
     * Flow de estatísticas.
     */
    fun getStatisticsFlow(userId: Long): Flow<PronunciationStatistics?>
}
