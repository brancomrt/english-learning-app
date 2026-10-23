package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.englishlearning.app.data.database.entity.DailyChallengeEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações com desafios diários.
 */
@Dao
interface DailyChallengeDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(challenges: List<DailyChallengeEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(challenge: DailyChallengeEntity): Long
    
    @Update
    suspend fun update(challenge: DailyChallengeEntity)
    
    @Query("SELECT * FROM daily_challenges WHERE userId = :userId AND date = :date")
    suspend fun getChallengesForDate(userId: Long, date: String): List<DailyChallengeEntity>
    
    @Query("SELECT * FROM daily_challenges WHERE userId = :userId AND date = :date")
    fun observeChallengesForDate(userId: Long, date: String): Flow<List<DailyChallengeEntity>>
    
    @Query("SELECT * FROM daily_challenges WHERE id = :id")
    suspend fun getById(id: Long): DailyChallengeEntity?
    
    @Query("""
        UPDATE daily_challenges 
        SET currentProgress = :progress 
        WHERE id = :challengeId AND userId = :userId
    """)
    suspend fun updateProgress(userId: Long, challengeId: Long, progress: Int)
    
    @Query("""
        UPDATE daily_challenges 
        SET isCompleted = 1, completedAt = :completedAt 
        WHERE id = :challengeId AND userId = :userId
    """)
    suspend fun markCompleted(userId: Long, challengeId: Long, completedAt: Long)
    
    @Query("""
        UPDATE daily_challenges 
        SET currentProgress = currentProgress + :increment 
        WHERE userId = :userId AND type = :type AND date = :date AND isCompleted = 0
    """)
    suspend fun incrementProgressForType(userId: Long, type: String, date: String, increment: Int)
    
    @Query("SELECT COUNT(*) FROM daily_challenges WHERE userId = :userId AND isCompleted = 1")
    suspend fun getTotalCompletedCount(userId: Long): Int
    
    @Query("SELECT SUM(xpReward + bonusXp) FROM daily_challenges WHERE userId = :userId AND isCompleted = 1")
    suspend fun getTotalXpEarned(userId: Long): Int?
    
    @Query("DELETE FROM daily_challenges WHERE date < :cutoffDate")
    suspend fun deleteOldChallenges(cutoffDate: String)
    
    @Query("""
        SELECT COUNT(DISTINCT date) FROM daily_challenges 
        WHERE userId = :userId AND isCompleted = 1
    """)
    suspend fun getDaysWithCompletedChallenges(userId: Long): Int
}
