package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.englishlearning.app.data.database.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para flashcards de vocabulário com SRS.
 */
@Dao
interface FlashcardDao {
    
    // ═══════════════════════════════════════════════════════════════════════
    // CRUD Operations
    // ═══════════════════════════════════════════════════════════════════════
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(flashcard: FlashcardEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(flashcards: List<FlashcardEntity>): List<Long>
    
    @Update
    suspend fun update(flashcard: FlashcardEntity)
    
    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun delete(id: Long)
    
    @Query("SELECT * FROM flashcards WHERE id = :id")
    suspend fun getById(id: Long): FlashcardEntity?
    
    @Query("SELECT * FROM flashcards WHERE word = :word LIMIT 1")
    suspend fun getByWord(word: String): FlashcardEntity?
    
    @Query("SELECT EXISTS(SELECT 1 FROM flashcards WHERE word = :word)")
    suspend fun wordExists(word: String): Boolean
    
    // ═══════════════════════════════════════════════════════════════════════
    // SRS Queries (Spaced Repetition)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Obtém flashcards devidos para revisão.
     * Cards com dueDate <= hoje são considerados devidos.
     */
    @Query("""
        SELECT * FROM flashcards 
        WHERE userId = :userId AND dueDate <= :today
        ORDER BY dueDate ASC
        LIMIT :limit
    """)
    suspend fun getDueCards(userId: Long, today: String, limit: Int = 50): List<FlashcardEntity>
    
    @Query("""
        SELECT * FROM flashcards 
        WHERE userId = :userId AND dueDate <= :today
        ORDER BY dueDate ASC
    """)
    fun getDueCardsFlow(userId: Long, today: String): Flow<List<FlashcardEntity>>
    
    @Query("""
        SELECT COUNT(*) FROM flashcards 
        WHERE userId = :userId AND dueDate <= :today
    """)
    suspend fun getDueCount(userId: Long, today: String): Int
    
    @Query("""
        SELECT COUNT(*) FROM flashcards 
        WHERE userId = :userId AND dueDate <= :today
    """)
    fun getDueCountFlow(userId: Long, today: String): Flow<Int>
    
    /**
     * Obtém novos flashcards (nunca revisados).
     */
    @Query("""
        SELECT * FROM flashcards 
        WHERE userId = :userId AND state = 'NEW'
        ORDER BY levelId ASC, id ASC
        LIMIT :limit
    """)
    suspend fun getNewCards(userId: Long, limit: Int = 20): List<FlashcardEntity>
    
    @Query("""
        SELECT COUNT(*) FROM flashcards 
        WHERE userId = :userId AND state = 'NEW'
    """)
    suspend fun getNewCount(userId: Long): Int
    
    /**
     * Atualiza parâmetros SRS após revisão.
     */
    @Query("""
        UPDATE flashcards SET
            stability = :stability,
            difficulty = :difficulty,
            elapsedDays = :elapsedDays,
            scheduledDays = :scheduledDays,
            reps = :reps,
            lapses = :lapses,
            state = :state,
            dueDate = :dueDate,
            lastReviewDate = :lastReviewDate
        WHERE id = :id
    """)
    suspend fun updateSrsParams(
        id: Long,
        stability: Double,
        difficulty: Double,
        elapsedDays: Int,
        scheduledDays: Int,
        reps: Int,
        lapses: Int,
        state: String,
        dueDate: String,
        lastReviewDate: String
    )
    
    // ═══════════════════════════════════════════════════════════════════════
    // Level/Category Queries
    // ═══════════════════════════════════════════════════════════════════════
    
    @Query("SELECT * FROM flashcards WHERE levelId = :levelId ORDER BY word ASC")
    suspend fun getByLevel(levelId: Int): List<FlashcardEntity>
    
    @Query("SELECT * FROM flashcards WHERE levelId = :levelId ORDER BY word ASC")
    fun getByLevelFlow(levelId: Int): Flow<List<FlashcardEntity>>
    
    @Query("SELECT COUNT(*) FROM flashcards WHERE levelId = :levelId")
    suspend fun getCountByLevel(levelId: Int): Int
    
    @Query("SELECT DISTINCT levelId FROM flashcards ORDER BY levelId")
    suspend fun getLevelsWithFlashcards(): List<Int>
    
    // ═══════════════════════════════════════════════════════════════════════
    // Statistics
    // ═══════════════════════════════════════════════════════════════════════
    
    @Query("SELECT COUNT(*) FROM flashcards WHERE userId = :userId")
    suspend fun getTotalCount(userId: Long): Int
    
    @Query("""
        SELECT COUNT(*) FROM flashcards 
        WHERE userId = :userId AND state = 'REVIEW' AND lapses = 0
    """)
    suspend fun getMasteredCount(userId: Long): Int
    
    @Query("""
        SELECT COUNT(*) FROM flashcards 
        WHERE userId = :userId AND state IN ('LEARNING', 'RELEARNING')
    """)
    suspend fun getLearningCount(userId: Long): Int
    
    @Query("""
        SELECT AVG(difficulty) FROM flashcards 
        WHERE userId = :userId AND reps > 0
    """)
    suspend fun getAverageDifficulty(userId: Long): Double?
    
    @Query("""
        SELECT state, COUNT(*) as count FROM flashcards 
        WHERE userId = :userId 
        GROUP BY state
    """)
    suspend fun getStateDistribution(userId: Long): List<StateCount>
    
    data class StateCount(
        val state: String,
        val count: Int
    )
    
    // ═══════════════════════════════════════════════════════════════════════
    // Search
    // ═══════════════════════════════════════════════════════════════════════
    
    @Query("""
        SELECT * FROM flashcards 
        WHERE userId = :userId AND (word LIKE '%' || :query || '%' OR definition LIKE '%' || :query || '%')
        ORDER BY word ASC
        LIMIT :limit
    """)
    suspend fun search(userId: Long, query: String, limit: Int = 50): List<FlashcardEntity>
    
    // ═══════════════════════════════════════════════════════════════════════
    // Bulk Operations
    // ═══════════════════════════════════════════════════════════════════════
    
    @Query("DELETE FROM flashcards WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: Long)
    
    @Query("DELETE FROM flashcards WHERE sourceDrillId = :drillId")
    suspend fun deleteByDrill(drillId: Int)
    
    @Query("""
        UPDATE flashcards SET 
            state = 'NEW', 
            stability = 0, 
            difficulty = 5.0, 
            elapsedDays = 0, 
            scheduledDays = 0, 
            reps = 0, 
            lapses = 0, 
            dueDate = :today, 
            lastReviewDate = NULL 
        WHERE userId = :userId
    """)
    suspend fun resetAllProgress(userId: Long, today: String)
}
