package com.englishlearning.app.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.englishlearning.app.domain.model.ChallengeType

/**
 * Entidade Room para persistir desafios diários.
 */
@Entity(
    tableName = "daily_challenges",
    indices = [
        Index(value = ["userId", "date"]),
        Index(value = ["userId", "type"])
    ]
)
data class DailyChallengeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val type: String, // ChallengeType.name()
    val title: String,
    val description: String,
    val targetValue: Int,
    val currentProgress: Int = 0,
    val xpReward: Int,
    val bonusXp: Int = 0,
    val date: String, // ISO date string (LocalDate.toString())
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val iconEmoji: String = "🎯"
)
