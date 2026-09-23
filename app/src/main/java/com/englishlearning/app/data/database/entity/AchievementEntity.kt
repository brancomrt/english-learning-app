package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade Room para conquistas desbloqueadas pelo usuário.
 * Requisito 9.1: persistência de todos os dados no Room.
 * Requisito 8.5: isNew indica conquistas ainda não visualizadas pelo usuário.
 * Requisito 9.6: índices em userId; índice único em (userId, achievementKey) para evitar duplicatas.
 */
@Entity(
    tableName = "achievements",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["user_id", "achievement_key"], unique = true)
    ]
)
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    /** Identificador da conquista, ex: "first_correct", "streak_7", "level_2_complete". */
    @ColumnInfo(name = "achievement_key")
    val achievementKey: String,

    /** Timestamp do desbloqueio em epoch millis. */
    @ColumnInfo(name = "unlocked_at")
    val unlockedAt: Long = System.currentTimeMillis(),

    /** Indica se a conquista ainda não foi visualizada pelo usuário. */
    @ColumnInfo(name = "is_new")
    val isNew: Boolean = true
)
