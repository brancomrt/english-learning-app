package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade Room para dicas progressivas de um exercício.
 * Dicas são reveladas em ordem crescente de `hintOrder` após tentativas incorretas.
 * Requisito 9.1: persistência de todos os dados no Room.
 * Requisito 3.2: dicas reveladas progressivamente por hintOrder.
 */
@Entity(
    tableName = "hints",
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
data class HintEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long,

    /** Ordem de exibição da dica: 1 = mais suave, N = mais revelador. */
    @ColumnInfo(name = "hint_order")
    val hintOrder: Int,

    @ColumnInfo(name = "hint_text")
    val hintText: String,

    /** Tipo da dica: "structural", "vocabulary", "grammar" ou "answer". */
    @ColumnInfo(name = "hint_type")
    val hintType: String
)
