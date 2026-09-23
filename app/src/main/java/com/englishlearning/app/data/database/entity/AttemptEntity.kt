package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade Room para tentativas individuais de resposta do usuário.
 * Requisito 9.1: persistência de todos os dados no Room.
 * Requisito 9.3: registrar userAnswer, validationStatus, score, xpEarned e timestamp.
 * Requisito 9.6: índices em userId, exerciseId e attemptedAt.
 */
@Entity(
    tableName = "attempts",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["exercise_id"]),
        Index(value = ["attempted_at"])
    ]
)
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long,

    /** Pergunta digitada pelo usuário. */
    @ColumnInfo(name = "user_answer")
    val userAnswer: String,

    /** Resultado da validação armazenado como String do enum ValidationStatus. */
    @ColumnInfo(name = "validation_status")
    val validationStatus: String,

    /** Score calculado para esta tentativa [0, 100]. */
    @ColumnInfo(name = "score")
    val score: Int,

    /** XP concedido ao usuário por esta tentativa. */
    @ColumnInfo(name = "xp_earned")
    val xpEarned: Int,

    /** Timestamp da tentativa em epoch millis. */
    @ColumnInfo(name = "attempted_at")
    val attemptedAt: Long = System.currentTimeMillis()
)
