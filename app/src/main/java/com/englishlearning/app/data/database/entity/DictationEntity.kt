package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade para exercícios de ditado no estilo Callan Method.
 * O usuário ouve o texto 3 vezes e escreve o que ouviu.
 * Organizado por Stages (1-12) seguindo a progressão do método Callan.
 */
@Entity(
    tableName = "dictation_exercises",
    indices = [
        Index(value = ["stage"]),
        Index(value = ["completed"])
    ]
)
data class DictationExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Stage do método Callan (1-12) */
    @ColumnInfo(name = "stage")
    val stage: Int,
    
    /** Título do exercício */
    @ColumnInfo(name = "title")
    val title: String,
    
    /** Texto completo a ser ditado (parágrafo) */
    @ColumnInfo(name = "text")
    val text: String,
    
    /** Tradução para português */
    @ColumnInfo(name = "translation")
    val translation: String? = null,
    
    /** Número de palavras no texto */
    @ColumnInfo(name = "wordCount")
    val wordCount: Int = 0,
    
    /** Foco gramatical do exercício */
    @ColumnInfo(name = "grammarFocus")
    val grammarFocus: String? = null,
    
    /** Se o exercício foi completado com 100% */
    @ColumnInfo(name = "completed")
    val completed: Boolean = false
)

/**
 * Progresso do usuário em exercícios de ditado.
 */
@Entity(
    tableName = "dictation_progress",
    primaryKeys = ["userId", "exerciseId"],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "dueDate"]),
        Index(value = ["state"])
    ]
)
data class DictationProgressEntity(
    @ColumnInfo(name = "userId")
    val userId: Long,
    
    @ColumnInfo(name = "exerciseId")
    val exerciseId: Long,
    
    /** Número de tentativas */
    @ColumnInfo(name = "attempts")
    val attempts: Int = 0,
    
    /** Número de acertos */
    @ColumnInfo(name = "correctCount")
    val correctCount: Int = 0,
    
    /** Melhor pontuação (0-100) */
    @ColumnInfo(name = "bestScore")
    val bestScore: Int = 0,
    
    /** Última pontuação */
    @ColumnInfo(name = "lastScore")
    val lastScore: Int = 0,
    
    // ═══════════════════════════════════════════════════════════════════════
    // Campos SRS para repetição espaçada
    // ═══════════════════════════════════════════════════════════════════════
    
    @ColumnInfo(name = "stability")
    val stability: Double = 0.0,
    
    @ColumnInfo(name = "difficulty")
    val difficulty: Double = 5.0,
    
    @ColumnInfo(name = "elapsedDays")
    val elapsedDays: Int = 0,
    
    @ColumnInfo(name = "scheduledDays")
    val scheduledDays: Int = 0,
    
    @ColumnInfo(name = "reps")
    val reps: Int = 0,
    
    @ColumnInfo(name = "lapses")
    val lapses: Int = 0,
    
    @ColumnInfo(name = "state")
    val state: String = "NEW",
    
    @ColumnInfo(name = "dueDate")
    val dueDate: String,
    
    @ColumnInfo(name = "lastReviewDate")
    val lastReviewDate: String? = null
)
