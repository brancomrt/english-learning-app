package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.englishlearning.app.data.database.entity.PronunciationResultEntity
import com.englishlearning.app.data.database.entity.PronunciationStatsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para resultados de avaliação de pronúncia.
 */
@Dao
interface PronunciationDao {
    
    // ═══════════════════════════════════════════════════════════════════════
    // Results
    // ═══════════════════════════════════════════════════════════════════════
    
    @Insert
    suspend fun insertResult(result: PronunciationResultEntity): Long
    
    @Query("SELECT * FROM pronunciation_results WHERE id = :id")
    suspend fun getResultById(id: Long): PronunciationResultEntity?
    
    @Query("""
        SELECT * FROM pronunciation_results 
        WHERE userId = :userId 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    suspend fun getRecentResults(userId: Long, limit: Int = 50): List<PronunciationResultEntity>
    
    @Query("""
        SELECT * FROM pronunciation_results 
        WHERE userId = :userId 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    fun getRecentResultsFlow(userId: Long, limit: Int = 50): Flow<List<PronunciationResultEntity>>
    
    @Query("""
        SELECT * FROM pronunciation_results 
        WHERE userId = :userId AND exerciseId = :exerciseId AND exerciseType = :type
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLastResultForExercise(userId: Long, exerciseId: Long, type: String): PronunciationResultEntity?
    
    @Query("""
        SELECT * FROM pronunciation_results 
        WHERE userId = :userId AND exerciseId = :exerciseId
        ORDER BY timestamp DESC
    """)
    suspend fun getHistoryForExercise(userId: Long, exerciseId: Long): List<PronunciationResultEntity>
    
    @Query("""
        SELECT AVG(overallScore) FROM pronunciation_results 
        WHERE userId = :userId AND timestamp > :since
    """)
    suspend fun getAverageScore(userId: Long, since: Long): Float?
    
    @Query("""
        SELECT MAX(overallScore) FROM pronunciation_results 
        WHERE userId = :userId
    """)
    suspend fun getBestScore(userId: Long): Int?
    
    @Query("""
        SELECT COUNT(*) FROM pronunciation_results 
        WHERE userId = :userId AND overallScore >= :threshold
    """)
    suspend fun getGoodScoreCount(userId: Long, threshold: Int = 80): Int
    
    // ═══════════════════════════════════════════════════════════════════════
    // Statistics
    // ═══════════════════════════════════════════════════════════════════════
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStats(stats: PronunciationStatsEntity)
    
    @Update
    suspend fun updateStats(stats: PronunciationStatsEntity)
    
    @Query("SELECT * FROM pronunciation_stats WHERE userId = :userId")
    suspend fun getStats(userId: Long): PronunciationStatsEntity?
    
    @Query("SELECT * FROM pronunciation_stats WHERE userId = :userId")
    fun getStatsFlow(userId: Long): Flow<PronunciationStatsEntity?>
    
    /**
     * Atualiza estatísticas após uma nova avaliação.
     */
    @Query("""
        UPDATE pronunciation_stats SET
            totalAttempts = totalAttempts + 1,
            averageScore = (averageScore * (totalAttempts - 1) + :newScore) / totalAttempts,
            bestScore = MAX(bestScore, :newScore),
            lastUpdated = :timestamp
        WHERE userId = :userId
    """)
    suspend fun incrementStats(userId: Long, newScore: Int, timestamp: Long = System.currentTimeMillis())
    
    // ═══════════════════════════════════════════════════════════════════════
    // Error Analysis
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Obtém palavras mais frequentemente erradas.
     * errorWords é um JSON array, então precisamos de processamento no app.
     */
    @Query("""
        SELECT errorWords FROM pronunciation_results 
        WHERE userId = :userId AND errorWords IS NOT NULL
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getRecentErrorWords(userId: Long, limit: Int = 100): List<String>
    
    /**
     * Obtém tendência de melhoria (últimas N avaliações).
     */
    @Query("""
        SELECT overallScore FROM pronunciation_results 
        WHERE userId = :userId 
        ORDER BY timestamp DESC 
        LIMIT :count
    """)
    suspend fun getRecentScores(userId: Long, count: Int = 10): List<Int>
    
    /**
     * Obtém distribuição de pontuações.
     */
    @Query("""
        SELECT 
            CASE 
                WHEN overallScore >= 90 THEN 'excellent'
                WHEN overallScore >= 80 THEN 'good'
                WHEN overallScore >= 60 THEN 'fair'
                ELSE 'needs_practice'
            END as category,
            COUNT(*) as count
        FROM pronunciation_results 
        WHERE userId = :userId
        GROUP BY category
    """)
    suspend fun getScoreDistribution(userId: Long): List<ScoreCategory>
    
    data class ScoreCategory(
        val category: String,
        val count: Int
    )
    
    // ═══════════════════════════════════════════════════════════════════════
    // Cleanup
    // ═══════════════════════════════════════════════════════════════════════
    
    @Query("DELETE FROM pronunciation_results WHERE userId = :userId")
    suspend fun deleteAllResults(userId: Long)
    
    @Query("DELETE FROM pronunciation_results WHERE timestamp < :before")
    suspend fun deleteOldResults(before: Long)
    
    @Query("DELETE FROM pronunciation_stats WHERE userId = :userId")
    suspend fun deleteStats(userId: Long)
}
