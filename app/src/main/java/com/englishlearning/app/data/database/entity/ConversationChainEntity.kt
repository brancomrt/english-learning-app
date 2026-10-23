package com.englishlearning.app.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade Room para cadeias de conversação.
 */
@Entity(
    tableName = "conversation_chains",
    indices = [
        Index(value = ["levelId"]),
        Index(value = ["levelId", "categoryId"])
    ]
)
data class ConversationChainEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val levelId: Int,
    val categoryId: Int,
    val contextStory: String? = null,
    val sortOrder: Int = 0
)

/**
 * Entidade para associar exercícios a uma chain.
 */
@Entity(
    tableName = "chain_exercises",
    primaryKeys = ["chainId", "exerciseId"],
    indices = [Index(value = ["chainId"])]
)
data class ChainExerciseEntity(
    val chainId: Long,
    val exerciseId: Long,
    val orderInChain: Int,
    val speakerRole: String // "A" ou "B"
)

/**
 * Entidade para rastrear progresso do usuário em chains.
 */
@Entity(
    tableName = "chain_progress",
    primaryKeys = ["userId", "chainId"],
    indices = [Index(value = ["userId"])]
)
data class ChainProgressEntity(
    val userId: Long,
    val chainId: Long,
    val currentExerciseIndex: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val startedAt: Long = System.currentTimeMillis()
)
