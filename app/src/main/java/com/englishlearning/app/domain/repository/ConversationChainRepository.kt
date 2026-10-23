package com.englishlearning.app.domain.repository

import com.englishlearning.app.domain.model.ConversationChain
import kotlinx.coroutines.flow.Flow

/**
 * Repository para gerenciar cadeias de conversação (mini-diálogos).
 */
interface ConversationChainRepository {
    
    /**
     * Obtém todas as cadeias disponíveis para um nível.
     */
    suspend fun getChainsForLevel(levelId: Int): List<ConversationChain>
    
    /**
     * Obtém cadeias por nível e categoria.
     */
    suspend fun getChainsForLevelAndCategory(levelId: Int, categoryId: Int): List<ConversationChain>
    
    /**
     * Obtém uma cadeia específica por ID.
     */
    suspend fun getChainById(chainId: Long): ConversationChain?
    
    /**
     * Obtém cadeias não completadas pelo usuário.
     */
    suspend fun getUncompletedChains(userId: Long, levelId: Int): List<ConversationChain>
    
    /**
     * Marca uma cadeia como completada pelo usuário.
     */
    suspend fun markChainCompleted(userId: Long, chainId: Long)
    
    /**
     * Obtém o progresso do usuário em uma cadeia (quantos exercícios completou).
     */
    suspend fun getChainProgress(userId: Long, chainId: Long): Int
    
    /**
     * Atualiza o progresso do usuário em uma cadeia.
     */
    suspend fun updateChainProgress(userId: Long, chainId: Long, exerciseIndex: Int)
    
    /**
     * Obtém estatísticas de cadeias do usuário.
     */
    suspend fun getChainStats(userId: Long): ChainStats
    
    /**
     * Observa cadeias disponíveis (para UI reativa).
     */
    fun observeAvailableChains(userId: Long, levelId: Int): Flow<List<ConversationChain>>
}

/**
 * Estatísticas de conversation chains.
 */
data class ChainStats(
    val totalChainsCompleted: Int,
    val totalChainsAvailable: Int,
    val totalExercisesInChains: Int,
    val favoriteCategory: String? // Categoria mais praticada em chains
)
