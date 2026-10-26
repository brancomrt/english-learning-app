package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade para armazenar resultados de avaliação de pronúncia.
 */
@Entity(
    tableName = "pronunciation_results",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "timestamp"]),
        Index(value = ["exerciseId"])
    ]
)
data class PronunciationResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "userId")
    val userId: Long,
    
    /** ID do exercício (drill ou dictation) */
    @ColumnInfo(name = "exerciseId")
    val exerciseId: Long,
    
    /** Tipo: "DRILL", "DICTATION", "SHADOWING" */
    @ColumnInfo(name = "exerciseType")
    val exerciseType: String,
    
    /** Texto esperado */
    @ColumnInfo(name = "expectedText")
    val expectedText: String,
    
    /** Texto reconhecido pelo Speech Recognition */
    @ColumnInfo(name = "recognizedText")
    val recognizedText: String,
    
    /** Pontuação geral (0-100) */
    @ColumnInfo(name = "overallScore")
    val overallScore: Int,
    
    /** Pontuação de precisão (palavras corretas) */
    @ColumnInfo(name = "accuracyScore")
    val accuracyScore: Int,
    
    /** Pontuação de fluência */
    @ColumnInfo(name = "fluencyScore")
    val fluencyScore: Int,
    
    /** Pontuação de completude */
    @ColumnInfo(name = "completenessScore")
    val completenessScore: Int,
    
    /** Palavras com erro (JSON array) */
    @ColumnInfo(name = "errorWords")
    val errorWords: String? = null,
    
    /** Palavras omitidas (JSON array) */
    @ColumnInfo(name = "omittedWords")
    val omittedWords: String? = null,
    
    /** Palavras inseridas (não esperadas) (JSON array) */
    @ColumnInfo(name = "insertedWords")
    val insertedWords: String? = null,
    
    /** Duração do áudio em milissegundos */
    @ColumnInfo(name = "audioDurationMs")
    val audioDurationMs: Long? = null,
    
    /** Nível de confiança do reconhecimento (0-1) */
    @ColumnInfo(name = "confidence")
    val confidence: Float? = null,
    
    /** Timestamp da avaliação */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Estatísticas agregadas de pronúncia por usuário.
 */
@Entity(
    tableName = "pronunciation_stats",
    primaryKeys = ["userId"]
)
data class PronunciationStatsEntity(
    @ColumnInfo(name = "userId")
    val userId: Long,
    
    /** Total de avaliações */
    @ColumnInfo(name = "totalAttempts")
    val totalAttempts: Int = 0,
    
    /** Média de pontuação geral */
    @ColumnInfo(name = "averageScore")
    val averageScore: Float = 0f,
    
    /** Melhor pontuação */
    @ColumnInfo(name = "bestScore")
    val bestScore: Int = 0,
    
    /** Palavras mais erradas (JSON: {"word": count}) */
    @ColumnInfo(name = "commonErrors")
    val commonErrors: String? = null,
    
    /** Última atualização */
    @ColumnInfo(name = "lastUpdated")
    val lastUpdated: Long = System.currentTimeMillis()
)
