package com.englishlearning.app.domain.repository

import com.englishlearning.app.domain.model.DictationExercise
import com.englishlearning.app.domain.model.DictationProgress
import com.englishlearning.app.domain.model.DictationResult
import com.englishlearning.app.domain.model.DictationStatistics
import kotlinx.coroutines.flow.Flow

/**
 * Repository para exercícios de ditado no estilo Callan Method.
 * Organizado por Stages (1-12) com textos completos.
 */
interface DictationRepository {
    
    // ═══════════════════════════════════════════════════════════════════════
    // Exercises by Stage (Callan Style)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Obtém exercícios por Stage.
     */
    suspend fun getByStage(stage: Int): List<DictationExercise>
    
    /**
     * Flow de exercícios por Stage.
     */
    fun getByStageFlow(stage: Int): Flow<List<DictationExercise>>
    
    /**
     * Obtém todos os exercícios.
     */
    suspend fun getAllExercises(): List<DictationExercise>
    
    /**
     * Flow de todos os exercícios.
     */
    fun getAllExercisesFlow(): Flow<List<DictationExercise>>
    
    /**
     * Lista os Stages disponíveis.
     */
    suspend fun getAvailableStages(): List<Int>
    
    /**
     * Obtém exercício por ID.
     */
    suspend fun getById(id: Long): DictationExercise?
    
    /**
     * Obtém exercícios incompletos de um Stage.
     */
    suspend fun getIncompleteByStage(stage: Int): List<DictationExercise>
    
    // ═══════════════════════════════════════════════════════════════════════
    // Mark Complete (100% success)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Marca exercício como completo (100% de acerto).
     */
    suspend fun markCompleted(exerciseId: Long, completed: Boolean)
    
    /**
     * Conta exercícios completos por Stage.
     */
    suspend fun getCompletedCountByStage(stage: Int): Int
    
    /**
     * Conta total de exercícios completos.
     */
    suspend fun getTotalCompletedCount(): Int
    
    // ═══════════════════════════════════════════════════════════════════════
    // Progress and Results
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Salva resultado de uma tentativa de ditado.
     * Se score = 100, marca como completo automaticamente.
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
    
    // ═══════════════════════════════════════════════════════════════════════
    // SRS (Spaced Repetition)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Obtém exercícios devidos para revisão.
     */
    suspend fun getDueExercises(userId: Long, today: String, limit: Int = 20): List<DictationExercise>
    
    /**
     * Obtém exercícios novos por Stage.
     */
    suspend fun getNewExercisesByStage(userId: Long, stage: Int, limit: Int = 10): List<DictationExercise>
    
    /**
     * Contagem de exercícios devidos.
     */
    suspend fun getDueCount(userId: Long, today: String): Int
    
    /**
     * Flow de contagem de exercícios devidos.
     */
    fun getDueCountFlow(userId: Long, today: String): Flow<Int>
}
