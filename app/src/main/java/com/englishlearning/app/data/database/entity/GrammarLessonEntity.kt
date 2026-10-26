package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade para mini-lições de gramática.
 * Apresentadas antes de cada tópico gramatical novo.
 */
@Entity(
    tableName = "grammar_lessons",
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
        Index(value = ["grammarTopic"]),
        Index(value = ["sortOrder"])
    ]
)
data class GrammarLessonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Nível associado */
    @ColumnInfo(name = "levelId")
    val levelId: Int,
    
    /** Tópico gramatical (ex: "present simple", "modal verbs") */
    @ColumnInfo(name = "grammarTopic")
    val grammarTopic: String,
    
    /** Título da lição */
    @ColumnInfo(name = "title")
    val title: String,
    
    /** Explicação principal em Markdown */
    @ColumnInfo(name = "explanation")
    val explanation: String,
    
    /** Fórmula/estrutura (ex: "Subject + verb-s/es") */
    @ColumnInfo(name = "formula")
    val formula: String? = null,
    
    /** Ordem de exibição */
    @ColumnInfo(name = "sortOrder")
    val sortOrder: Int = 0,
    
    /** Emoji ícone */
    @ColumnInfo(name = "iconEmoji")
    val iconEmoji: String = "📚"
)

/**
 * Exemplo interativo dentro de uma lição de gramática.
 */
@Entity(
    tableName = "grammar_examples",
    foreignKeys = [
        ForeignKey(
            entity = GrammarLessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["lessonId"])
    ]
)
data class GrammarExampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** ID da lição pai */
    @ColumnInfo(name = "lessonId")
    val lessonId: Long,
    
    /** Frase em inglês */
    @ColumnInfo(name = "sentence")
    val sentence: String,
    
    /** Tradução */
    @ColumnInfo(name = "translation")
    val translation: String,
    
    /** Destaque na frase (parte gramatical importante) */
    @ColumnInfo(name = "highlight")
    val highlight: String? = null,
    
    /** Explicação adicional do exemplo */
    @ColumnInfo(name = "note")
    val note: String? = null,
    
    /** É um exemplo correto ou incorreto? */
    @ColumnInfo(name = "isCorrect")
    val isCorrect: Boolean = true,
    
    /** Ordem de exibição */
    @ColumnInfo(name = "sortOrder")
    val sortOrder: Int = 0
)

/**
 * Progresso do usuário nas lições de gramática.
 */
@Entity(
    tableName = "grammar_progress",
    primaryKeys = ["userId", "lessonId"],
    foreignKeys = [
        ForeignKey(
            entity = GrammarLessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["lessonId"])
    ]
)
data class GrammarProgressEntity(
    @ColumnInfo(name = "userId")
    val userId: Long,
    
    @ColumnInfo(name = "lessonId")
    val lessonId: Long,
    
    /** Lição foi visualizada */
    @ColumnInfo(name = "isViewed")
    val isViewed: Boolean = false,
    
    /** Lição foi completada (todos exemplos vistos) */
    @ColumnInfo(name = "isCompleted")
    val isCompleted: Boolean = false,
    
    /** Timestamp da última visualização */
    @ColumnInfo(name = "viewedAt")
    val viewedAt: Long? = null,
    
    /** Timestamp de conclusão */
    @ColumnInfo(name = "completedAt")
    val completedAt: Long? = null
)
