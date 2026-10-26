package com.englishlearning.app.domain.repository

import com.englishlearning.app.domain.model.DictationExercise
import com.englishlearning.app.domain.model.DictationProgress
import com.englishlearning.app.domain.model.DictationResult
import com.englishlearning.app.domain.model.DictationStatistics
import kotlinx.coroutines.flow.Flow

/**
 * Repository para exercícios de ditado.
 */
interface DictationRepository {
    
    /**
     * Obtém exercícios devidos para revisão.
     */
    suspend fun getDueExercises(userId: Long, today: String, limit: Int = 20): List<DictationExercise>
    
    /**
     * Obtém exercícios novos.
     */
    suspend fun getNewExercises(userId: Long, maxLevel: Int, limit: Int = 10): List<DictationExercise>
    
    /**
     * Obtém exercício por ID.
     */
    suspend fun getById(id: Long): DictationExercise?
    
    /**
     * Obtém exercícios por nível.
     */
    suspend fun getByLevel(levelId: Int): List<DictationExercise>
    
    /**
     * Salva resultado de uma tentativa de ditado.
     */
    suspend fun saveResult(userId: Long, exerciseId: Long, result: DictationResult)
    
    /**
     * Obtém progresso do usuário em um exercício.
     */
    suspend fun getProgress(userId: Long, exerciseId: Long): DictationProgress?
    
    /**
     * Obtém estatísticas de ditado.
     */
    suspend fun getStatistics(userId: Long): DictationStatistics
    
    /**
     * Contagem de exercícios devidos.
     */
    suspend fun getDueCount(userId: Long, today: String): Int
    
    /**
     * Flow de contagem de exercícios devidos.
     */
    fun getDueCountFlow(userId: Long, today: String): Flow<Int>
    
    /**
     * Gera exercícios de ditado a partir dos drills.
     */
    suspend fun generateFromDrills()
}
