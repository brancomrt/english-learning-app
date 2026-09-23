package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade Room para exercícios.
 *
 * Suporta dois modos:
 * - QUESTION_FROM_ANSWER (padrão / mecânica invertida): exibe [answerText], usuário formula a pergunta.
 * - ANSWER_FROM_QUESTION: exibe [promptText] (uma pergunta), usuário formula a resposta.
 */
@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = LevelEntity::class,
            parentColumns = ["id"],
            childColumns = ["level_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["level_id"]),
        Index(value = ["category_id"])
    ]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "level_id")
    val levelId: Int,

    @ColumnInfo(name = "category_id")
    val categoryId: Int,

    /** Resposta em inglês exibida no modo QUESTION_FROM_ANSWER. Requisito 1.1. */
    @ColumnInfo(name = "answer_text")
    val answerText: String,

    /**
     * Pergunta exibida no modo ANSWER_FROM_QUESTION.
     * Null nos exercícios legados do modo padrão.
     */
    @ColumnInfo(name = "prompt_text")
    val promptText: String? = null,

    /** Contexto opcional de conversa. Requisito 1.3. */
    @ColumnInfo(name = "context_text")
    val contextText: String?,

    /** Tipo gramatical da pergunta/resposta esperada. */
    @ColumnInfo(name = "question_type")
    val questionType: String,

    /**
     * Modo do exercício:
     * - "QUESTION_FROM_ANSWER" — mecânica invertida (padrão)
     * - "ANSWER_FROM_QUESTION" — dado uma pergunta, formular a resposta
     */
    @ColumnInfo(name = "exercise_mode")
    val exerciseMode: String = "QUESTION_FROM_ANSWER",

    @ColumnInfo(name = "grammar_focus")
    val grammarFocus: String,

    @ColumnInfo(name = "difficulty_score")
    val difficultyScore: Int,

    @ColumnInfo(name = "xp_reward")
    val xpReward: Int,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0
)
