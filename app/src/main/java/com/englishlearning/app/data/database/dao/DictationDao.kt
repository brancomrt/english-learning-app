package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.englishlearning.app.data.database.entity.DictationExerciseEntity
import com.englishlearning.app.data.database.entity.DictationProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para exercícios de ditado (Spaced Writing).
 */
@Dao
interface DictationDao {
    
    // ═══════════════════════════════════════════════════════════════════════
    // Dictation Exercises
    // ═══════════════════════════════════════════════════════════════════════
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: DictationExerciseEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<DictationExerciseEntity>)
    
    @Query("SELECT * FROM dictation_exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): DictationExerciseEntity?
    
    @Query("SELECT * FROM dictation_exercises WHERE levelId = :levelId ORDER BY difficulty")
    suspend fun getExercisesByLevel(levelId: Int): List<DictationExerciseEntity>
    
    @Query("SELECT * FROM dictation_exercises WHERE levelId = :levelId ORDER BY difficulty")
    fun getExercisesByLevelFlow(levelId: Int): Flow<List<DictationExerciseEntity>>
    
    @Query("SELECT * FROM dictation_exercises WHERE sourceDrillId = :drillId LIMIT 1")
    suspend fun getExerciseByDrill(drillId: Int): DictationExerciseEntity?
    
    @Query("SELECT * FROM dictation_exercises WHERE difficulty = :difficulty ORDER BY RANDOM() LIMIT :limit")
    suspend fun getExercisesByDifficulty(difficulty: Int, limit: Int = 10): List<DictationExerciseEntity>
    
    @Query("SELECT COUNT(*) FROM dictation_exercises WHERE levelId = :levelId")
    suspend fun getExerciseCountByLevel(levelId: Int): Int
    
    @Query("SELECT COUNT(*) FROM dictation_exercises")
    suspend fun getTotalExerciseCount(): Int
    
    // ═══════════════════════════════════════════════════════════════════════
    // Dictation Progress (SRS)
    // ═══════════════════════════════════════════════════════════════════════
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: DictationProgressEntity)
    
    @Update
    suspend fun updateProgress(progress: DictationProgressEntity)
    
    @Query("SELECT * FROM dictation_progress WHERE userId = :userId AND exerciseId = :exerciseId")
    suspend fun getProgress(userId: Long, exerciseId: Long): DictationProgressEntity?
    
    @Query("SELECT * FROM dictation_progress WHERE userId = :userId")
    suspend fun getAllProgress(userId: Long): List<DictationProgressEntity>
    
    /**
     * Obtém exercícios devidos para revisão SRS.
     */
    @Query("""
        SELECT de.* FROM dictation_exercises de
        INNER JOIN dictation_progress dp ON de.id = dp.exerciseId
        WHERE dp.userId = :userId AND dp.dueDate <= :today
        ORDER BY dp.dueDate ASC
        LIMIT :limit
    """)
    suspend fun getDueExercises(userId: Long, today: String, limit: Int = 20): List<DictationExerciseEntity>
    
    @Query("""
        SELECT COUNT(*) FROM dictation_progress 
        WHERE userId = :userId AND dueDate <= :today
    """)
    suspend fun getDueCount(userId: Long, today: String): Int
    
    @Query("""
        SELECT COUNT(*) FROM dictation_progress 
        WHERE userId = :userId AND dueDate <= :today
    """)
    fun getDueCountFlow(userId: Long, today: String): Flow<Int>
    
    /**
     * Obtém exercícios novos (sem progresso ainda).
     */
    @Query("""
        SELECT de.* FROM dictation_exercises de
        LEFT JOIN dictation_progress dp ON de.id = dp.exerciseId AND dp.userId = :userId
        WHERE dp.exerciseId IS NULL AND de.levelId <= :maxLevel
        ORDER BY de.levelId ASC, de.difficulty ASC
        LIMIT :limit
    """)
    suspend fun getNewExercises(userId: Long, maxLevel: Int, limit: Int = 10): List<DictationExerciseEntity>
    
    @Query("""
        SELECT COUNT(*) FROM dictation_exercises de
        LEFT JOIN dictation_progress dp ON de.id = dp.exerciseId AND dp.userId = :userId
        WHERE dp.exerciseId IS NULL
    """)
    suspend fun getNewCount(userId: Long): Int
    
    /**
     * Atualiza parâmetros SRS após revisão.
     */
    @Query("""
        UPDATE dictation_progress SET
            attempts = attempts + 1,
            lastScore = :score,
            bestScore = MAX(bestScore, :score),
            correctCount = CASE WHEN :score >= 80 THEN correctCount + 1 ELSE correctCount END,
            stability = :stability,
            difficulty = :difficulty,
            elapsedDays = :elapsedDays,
            scheduledDays = :scheduledDays,
            reps = :reps,
            lapses = :lapses,
            state = :state,
            dueDate = :dueDate,
            lastReviewDate = :lastReviewDate
        WHERE userId = :userId AND exerciseId = :exerciseId
    """)
    suspend fun updateAfterReview(
        userId: Long,
        exerciseId: Long,
        score: Int,
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
    // Statistics
    // ═══════════════════════════════════════════════════════════════════════
    
    @Query("""
        SELECT AVG(bestScore) FROM dictation_progress 
        WHERE userId = :userId AND attempts > 0
    """)
    suspend fun getAverageScore(userId: Long): Float?
    
    @Query("""
        SELECT COUNT(*) FROM dictation_progress 
        WHERE userId = :userId AND bestScore >= 90
    """)
    suspend fun getMasteredCount(userId: Long): Int
    
    @Query("""
        SELECT SUM(attempts) FROM dictation_progress 
        WHERE userId = :userId
    """)
    suspend fun getTotalAttempts(userId: Long): Int?
    
    @Query("""
        SELECT state, COUNT(*) as count FROM dictation_progress 
        WHERE userId = :userId 
        GROUP BY state
    """)
    suspend fun getStateDistribution(userId: Long): List<StateCount>
    
    data class StateCount(
        val state: String,
        val count: Int
    )
    
    // ═══════════════════════════════════════════════════════════════════════
    // Reset/Delete
    // ═══════════════════════════════════════════════════════════════════════
    
    @Query("DELETE FROM dictation_progress WHERE userId = :userId")
    suspend fun deleteAllProgress(userId: Long)
    
    @Query("""
        UPDATE dictation_progress SET 
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
    suspend fun resetAllSrs(userId: Long, today: String)
}
