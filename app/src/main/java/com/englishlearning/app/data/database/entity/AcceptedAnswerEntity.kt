package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade Room para respostas aceitas de um exercício.
 * Um exercício pode ter múltiplas formas de pergunta aceitas como corretas.
 * Requisito 9.1: persistência de todos os dados no Room.
 */
@Entity(
    tableName = "accepted_answers",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["exercise_id"])
    ]
)
data class AcceptedAnswerEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long,

    /** A pergunta aceita como correta. */
    @ColumnInfo(name = "question_text")
    val questionText: String,

    /** Indica se esta é a resposta canônica principal do exercício. */
    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean,

    /** Threshold de similaridade semântica para aceitar a resposta [0.0, 1.0]. */
    @ColumnInfo(name = "match_threshold")
    val matchThreshold: Float
)
