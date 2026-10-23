package com.englishlearning.app.data.repository

import com.englishlearning.app.data.database.dao.ConversationChainDao
import com.englishlearning.app.data.database.entity.ChainProgressEntity
import com.englishlearning.app.data.database.entity.ConversationChainEntity
import com.englishlearning.app.domain.model.ConversationChain
import com.englishlearning.app.domain.repository.ChainStats
import com.englishlearning.app.domain.repository.ConversationChainRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationChainRepositoryImpl @Inject constructor(
    private val chainDao: ConversationChainDao
) : ConversationChainRepository {
    
    override suspend fun getChainsForLevel(levelId: Int): List<ConversationChain> {
        val entities = chainDao.getChainsForLevel(levelId)
        return entities.map { it.toDomain() }
    }
    
    override suspend fun getChainsForLevelAndCategory(levelId: Int, categoryId: Int): List<ConversationChain> {
        val entities = chainDao.getChainsForLevelAndCategory(levelId, categoryId)
        return entities.map { it.toDomain() }
    }
    
    override suspend fun getChainById(chainId: Long): ConversationChain? {
        val entity = chainDao.getChainById(chainId) ?: return null
        return entity.toDomain()
    }
    
    override suspend fun getUncompletedChains(userId: Long, levelId: Int): List<ConversationChain> {
        val entities = chainDao.getUncompletedChains(userId, levelId)
        return entities.map { it.toDomain() }
    }
    
    override suspend fun markChainCompleted(userId: Long, chainId: Long) {
        chainDao.markChainCompleted(userId, chainId, System.currentTimeMillis())
    }
    
    override suspend fun getChainProgress(userId: Long, chainId: Long): Int {
        return chainDao.getProgressIndex(userId, chainId) ?: 0
    }
    
    override suspend fun updateChainProgress(userId: Long, chainId: Long, exerciseIndex: Int) {
        val existing = chainDao.getProgress(userId, chainId)
        if (existing == null) {
            chainDao.insertOrUpdateProgress(
                ChainProgressEntity(
                    userId = userId,
                    chainId = chainId,
                    currentExerciseIndex = exerciseIndex
                )
            )
        } else {
            chainDao.updateProgressIndex(userId, chainId, exerciseIndex)
        }
    }
    
    override suspend fun getChainStats(userId: Long): ChainStats {
        val totalCompleted = chainDao.getTotalCompletedChains(userId)
        val totalAvailable = chainDao.getTotalChainsCount()
        val totalExercises = chainDao.getTotalExercisesInChains() ?: 0
        
        return ChainStats(
            totalChainsCompleted = totalCompleted,
            totalChainsAvailable = totalAvailable,
            totalExercisesInChains = totalExercises,
            favoriteCategory = null
        )
    }
    
    override fun observeAvailableChains(userId: Long, levelId: Int): Flow<List<ConversationChain>> {
        return chainDao.observeChainsForLevel(levelId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    // ────────────────────────────────────────────────────────────────
    // Mappers
    // ────────────────────────────────────────────────────────────────
    
    private fun ConversationChainEntity.toDomain(): ConversationChain = ConversationChain(
        id = id,
        title = title,
        description = description,
        levelId = levelId,
        categoryId = categoryId,
        exercises = emptyList(), // Exercícios são carregados sob demanda quando necessário
        contextStory = contextStory
    )
}
