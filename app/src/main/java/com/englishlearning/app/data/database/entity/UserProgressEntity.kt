package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade Room para progresso do usuário por combinação nível/categoria.
 * Requisito 9.1: persistência de todos os dados no Room.
 * Requisito 9.6: índices em userId e levelId para performance; índice único em (userId, levelId, categoryId).
 */
@Entity(
    tableName = "user_progress",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["level_id"]),
        Index(value = ["user_id", "level_id", "category_id"], unique = true)
    ]
)
data class UserProgressEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "level_id")
    val levelId: Int,

    @ColumnInfo(name = "category_id")
    val categoryId: Int,

    @ColumnInfo(name = "exercises_completed")
    val exercisesCompleted: Int = 0,

    @ColumnInfo(name = "total_attempts")
    val totalAttempts: Int = 0,

    @ColumnInfo(name = "correct_answers")
    val correctAnswers: Int = 0,

    @ColumnInfo(name = "total_xp_earned")
    val totalXpEarned: Int = 0,

    @ColumnInfo(name = "last_attempt_at")
    val lastAttemptAt: Long = 0L
)
