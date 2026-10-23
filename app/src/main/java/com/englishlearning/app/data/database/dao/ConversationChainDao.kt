package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.englishlearning.app.data.database.entity.ChainExerciseEntity
import com.englishlearning.app.data.database.entity.ChainProgressEntity
import com.englishlearning.app.data.database.entity.ConversationChainEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações com cadeias de conversação.
 */
@Dao
interface ConversationChainDao {
    
    // ────────────────────────────────────────────────────────────────
    // Chains
    // ────────────────────────────────────────────────────────────────
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChain(chain: ConversationChainEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChains(chains: List<ConversationChainEntity>)
    
    @Query("SELECT * FROM conversation_chains WHERE levelId = :levelId ORDER BY sortOrder")
    suspend fun getChainsForLevel(levelId: Int): List<ConversationChainEntity>
    
    @Query("SELECT * FROM conversation_chains WHERE levelId = :levelId AND categoryId = :categoryId ORDER BY sortOrder")
    suspend fun getChainsForLevelAndCategory(levelId: Int, categoryId: Int): List<ConversationChainEntity>
    
    @Query("SELECT * FROM conversation_chains WHERE id = :chainId")
    suspend fun getChainById(chainId: Long): ConversationChainEntity?
    
    @Query("SELECT * FROM conversation_chains WHERE levelId = :levelId ORDER BY sortOrder")
    fun observeChainsForLevel(levelId: Int): Flow<List<ConversationChainEntity>>
    
    // ────────────────────────────────────────────────────────────────
    // Chain Exercises
    // ────────────────────────────────────────────────────────────────
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChainExercises(exercises: List<ChainExerciseEntity>)
    
    @Query("SELECT * FROM chain_exercises WHERE chainId = :chainId ORDER BY orderInChain")
    suspend fun getExercisesForChain(chainId: Long): List<ChainExerciseEntity>
    
    @Query("SELECT exerciseId FROM chain_exercises WHERE chainId = :chainId ORDER BY orderInChain")
    suspend fun getExerciseIdsForChain(chainId: Long): List<Long>
    
    // ────────────────────────────────────────────────────────────────
    // Chain Progress
    // ────────────────────────────────────────────────────────────────
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: ChainProgressEntity)
    
    @Query("SELECT * FROM chain_progress WHERE userId = :userId AND chainId = :chainId")
    suspend fun getProgress(userId: Long, chainId: Long): ChainProgressEntity?
    
    @Query("SELECT currentExerciseIndex FROM chain_progress WHERE userId = :userId AND chainId = :chainId")
    suspend fun getProgressIndex(userId: Long, chainId: Long): Int?
    
    @Query("""
        UPDATE chain_progress 
        SET currentExerciseIndex = :exerciseIndex 
        WHERE userId = :userId AND chainId = :chainId
    """)
    suspend fun updateProgressIndex(userId: Long, chainId: Long, exerciseIndex: Int)
    
    @Query("""
        UPDATE chain_progress 
        SET isCompleted = 1, completedAt = :completedAt 
        WHERE userId = :userId AND chainId = :chainId
    """)
    suspend fun markChainCompleted(userId: Long, chainId: Long, completedAt: Long)
    
    @Query("SELECT COUNT(*) FROM chain_progress WHERE userId = :userId AND isCompleted = 1")
    suspend fun getTotalCompletedChains(userId: Long): Int
    
    @Query("""
        SELECT c.* FROM conversation_chains c 
        LEFT JOIN chain_progress p ON c.id = p.chainId AND p.userId = :userId
        WHERE c.levelId = :levelId AND (p.isCompleted IS NULL OR p.isCompleted = 0)
        ORDER BY c.sortOrder
    """)
    suspend fun getUncompletedChains(userId: Long, levelId: Int): List<ConversationChainEntity>
    
    @Query("SELECT COUNT(*) FROM conversation_chains")
    suspend fun getTotalChainsCount(): Int
    
    @Query("SELECT SUM(1) FROM chain_exercises")
    suspend fun getTotalExercisesInChains(): Int?
}
