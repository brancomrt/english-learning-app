package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.englishlearning.app.data.database.entity.GrammarExampleEntity
import com.englishlearning.app.data.database.entity.GrammarLessonEntity
import com.englishlearning.app.data.database.entity.GrammarProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para lições de gramática e exemplos interativos.
 */
@Dao
interface GrammarLessonDao {
    
    // ═══════════════════════════════════════════════════════════════════════
    // Grammar Lessons
    // ═══════════════════════════════════════════════════════════════════════
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: GrammarLessonEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<GrammarLessonEntity>)
    
    @Update
    suspend fun updateLesson(lesson: GrammarLessonEntity)
    
    @Query("SELECT * FROM grammar_lessons WHERE id = :id")
    suspend fun getLessonById(id: Long): GrammarLessonEntity?
    
    @Query("SELECT * FROM grammar_lessons WHERE levelId = :levelId ORDER BY sortOrder")
    suspend fun getLessonsByLevel(levelId: Int): List<GrammarLessonEntity>
    
    @Query("SELECT * FROM grammar_lessons WHERE levelId = :levelId ORDER BY sortOrder")
    fun getLessonsByLevelFlow(levelId: Int): Flow<List<GrammarLessonEntity>>
    
    @Query("SELECT * FROM grammar_lessons WHERE grammarTopic = :topic LIMIT 1")
    suspend fun getLessonByTopic(topic: String): GrammarLessonEntity?
    
    @Query("SELECT * FROM grammar_lessons WHERE grammarTopic LIKE '%' || :keyword || '%' ORDER BY sortOrder")
    suspend fun searchLessons(keyword: String): List<GrammarLessonEntity>
    
    @Query("SELECT DISTINCT grammarTopic FROM grammar_lessons ORDER BY grammarTopic")
    suspend fun getAllTopics(): List<String>
    
    @Query("SELECT COUNT(*) FROM grammar_lessons WHERE levelId = :levelId")
    suspend fun getLessonCountByLevel(levelId: Int): Int
    
    // ═══════════════════════════════════════════════════════════════════════
    // Grammar Examples
    // ═══════════════════════════════════════════════════════════════════════
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExample(example: GrammarExampleEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamples(examples: List<GrammarExampleEntity>)
    
    @Query("SELECT * FROM grammar_examples WHERE lessonId = :lessonId ORDER BY sortOrder")
    suspend fun getExamplesByLesson(lessonId: Long): List<GrammarExampleEntity>
    
    @Query("SELECT * FROM grammar_examples WHERE lessonId = :lessonId ORDER BY sortOrder")
    fun getExamplesByLessonFlow(lessonId: Long): Flow<List<GrammarExampleEntity>>
    
    @Query("SELECT * FROM grammar_examples WHERE lessonId = :lessonId AND isCorrect = :isCorrect ORDER BY sortOrder")
    suspend fun getExamplesByCorrectness(lessonId: Long, isCorrect: Boolean): List<GrammarExampleEntity>
    
    // ═══════════════════════════════════════════════════════════════════════
    // Grammar Progress
    // ═══════════════════════════════════════════════════════════════════════
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: GrammarProgressEntity)
    
    @Query("SELECT * FROM grammar_progress WHERE userId = :userId AND lessonId = :lessonId")
    suspend fun getProgress(userId: Long, lessonId: Long): GrammarProgressEntity?
    
    @Query("SELECT * FROM grammar_progress WHERE userId = :userId")
    suspend fun getAllProgressByUser(userId: Long): List<GrammarProgressEntity>
    
    @Query("SELECT * FROM grammar_progress WHERE userId = :userId")
    fun getProgressFlow(userId: Long): Flow<List<GrammarProgressEntity>>
    
    @Query("""
        UPDATE grammar_progress 
        SET isViewed = 1, viewedAt = :timestamp 
        WHERE userId = :userId AND lessonId = :lessonId
    """)
    suspend fun markAsViewed(userId: Long, lessonId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE grammar_progress 
        SET isCompleted = 1, completedAt = :timestamp 
        WHERE userId = :userId AND lessonId = :lessonId
    """)
    suspend fun markAsCompleted(userId: Long, lessonId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("""
        SELECT COUNT(*) FROM grammar_progress 
        WHERE userId = :userId AND isCompleted = 1
    """)
    suspend fun getCompletedCount(userId: Long): Int
    
    @Query("""
        SELECT gl.* FROM grammar_lessons gl
        LEFT JOIN grammar_progress gp ON gl.id = gp.lessonId AND gp.userId = :userId
        WHERE gl.levelId = :levelId AND (gp.isCompleted IS NULL OR gp.isCompleted = 0)
        ORDER BY gl.sortOrder
        LIMIT 1
    """)
    suspend fun getNextLesson(userId: Long, levelId: Int): GrammarLessonEntity?
    
    // ═══════════════════════════════════════════════════════════════════════
    // Combined Queries
    // ═══════════════════════════════════════════════════════════════════════
    
    @Transaction
    @Query("""
        SELECT gl.*, 
               COALESCE(gp.isViewed, 0) as isViewed,
               COALESCE(gp.isCompleted, 0) as isCompleted
        FROM grammar_lessons gl
        LEFT JOIN grammar_progress gp ON gl.id = gp.lessonId AND gp.userId = :userId
        WHERE gl.levelId = :levelId
        ORDER BY gl.sortOrder
    """)
    suspend fun getLessonsWithProgress(userId: Long, levelId: Int): List<LessonWithProgress>
    
    data class LessonWithProgress(
        val id: Long,
        val levelId: Int,
        val grammarTopic: String,
        val title: String,
        val explanation: String,
        val formula: String?,
        val sortOrder: Int,
        val iconEmoji: String,
        val isViewed: Boolean,
        val isCompleted: Boolean
    )
}
