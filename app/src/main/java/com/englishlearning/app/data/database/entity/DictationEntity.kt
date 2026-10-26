package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade para exercícios de ditado (Spaced Writing).
 * O usuário ouve o áudio e escreve a frase.
 */
@Entity(
    tableName = "dictation_exercises",
    indices = [
        Index(value = ["levelId"]),
        Index(value = ["sourceDrillId"]),
        Index(value = ["difficulty"])
    ]
)
data class DictationExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Frase a ser ditada */
    @ColumnInfo(name = "sentence")
    val sentence: String,
    
    /** Tradução/dica (opcional) */
    @ColumnInfo(name = "translation")
    val translation: String? = null,
    
    /** Nível de dificuldade */
    @ColumnInfo(name = "levelId")
    val levelId: Int,
    
    /** ID do drill de origem (se extraído de drills) */
    @ColumnInfo(name = "sourceDrillId")
    val sourceDrillId: Int? = null,
    
    /** Dificuldade calculada (baseada em comprimento, palavras difíceis) */
    @ColumnInfo(name = "difficulty")
    val difficulty: Int = 1,
    
    /** Palavras-chave para validação parcial */
    @ColumnInfo(name = "keywords")
    val keywords: String? = null, // JSON array serializado
    
    /** Categoria gramatical */
    @ColumnInfo(name = "grammarTopic")
    val grammarTopic: String? = null
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
