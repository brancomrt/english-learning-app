package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room para níveis de dificuldade do aplicativo.
 * Requisito 9.1: persistência de todos os dados no Room.
 */
@Entity(tableName = "levels")
data class LevelEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "name_key")
    val nameKey: String,

    @ColumnInfo(name = "description_key")
    val descriptionKey: String,

    @ColumnInfo(name = "min_score")
    val minScore: Int,

    @ColumnInfo(name = "max_score")
    val maxScore: Int,

    @ColumnInfo(name = "is_unlocked")
    val isUnlocked: Boolean,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int
)
