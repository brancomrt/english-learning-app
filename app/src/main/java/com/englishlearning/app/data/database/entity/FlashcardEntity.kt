package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade para flashcards de vocabulário extraídos automaticamente dos drills.
 * Usa sistema SRS (FSRS-4.5) separado para repetição espaçada de palavras.
 */
@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = LevelEntity::class,
            parentColumns = ["id"],
            childColumns = ["levelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["levelId"]),
        Index(value = ["word"], unique = true),
        Index(value = ["userId", "dueDate"]),
        Index(value = ["userId", "state"])
    ]
)
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "userId")
    val userId: Long = 1,
    
    /** Palavra em inglês */
    @ColumnInfo(name = "word")
    val word: String,
    
    /** Tradução ou definição */
    @ColumnInfo(name = "definition")
    val definition: String,
    
    /** Frase de exemplo extraída do drill */
    @ColumnInfo(name = "exampleSentence")
    val exampleSentence: String,
    
    /** Contexto adicional (opcional) */
    @ColumnInfo(name = "context")
    val context: String? = null,
    
    /** ID do drill de origem */
    @ColumnInfo(name = "sourceDrillId")
    val sourceDrillId: Int? = null,
    
    /** Nível do exercício */
    @ColumnInfo(name = "levelId")
    val levelId: Int,
    
    /** Categoria gramatical: noun, verb, adjective, etc */
    @ColumnInfo(name = "partOfSpeech")
    val partOfSpeech: String? = null,
    
    /** Pronúncia em IPA (opcional) */
    @ColumnInfo(name = "pronunciation")
    val pronunciation: String? = null,
    
    /** URL de áudio (opcional) */
    @ColumnInfo(name = "audioUrl")
    val audioUrl: String? = null,
    
    /** URL de imagem ilustrativa (opcional) */
    @ColumnInfo(name = "imageUrl")
    val imageUrl: String? = null,
    
    // ═══════════════════════════════════════════════════════════════════════
    // Campos FSRS-4.5 para repetição espaçada
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Estabilidade da memória (quanto tempo até esquecer) */
    @ColumnInfo(name = "stability")
    val stability: Double = 0.0,
    
    /** Dificuldade intrínseca do card (1-10) */
    @ColumnInfo(name = "difficulty")
    val difficulty: Double = 5.0,
    
    /** Dias desde última revisão */
    @ColumnInfo(name = "elapsedDays")
    val elapsedDays: Int = 0,
    
    /** Dias até próxima revisão programada */
    @ColumnInfo(name = "scheduledDays")
    val scheduledDays: Int = 0,
    
    /** Número total de revisões */
    @ColumnInfo(name = "reps")
    val reps: Int = 0,
    
    /** Número de lapsos (esquecimentos) */
    @ColumnInfo(name = "lapses")
    val lapses: Int = 0,
    
    /** Estado do card: NEW, LEARNING, REVIEW, RELEARNING */
    @ColumnInfo(name = "state")
    val state: String = "NEW",
    
    /** Data de vencimento (ISO-8601) */
    @ColumnInfo(name = "dueDate")
    val dueDate: String,
    
    /** Data da última revisão (ISO-8601, nullable) */
    @ColumnInfo(name = "lastReviewDate")
    val lastReviewDate: String? = null,
    
    /** Timestamp de criação */
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)
