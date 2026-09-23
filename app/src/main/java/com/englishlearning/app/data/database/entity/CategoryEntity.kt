package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade Room para categorias temáticas dentro de um nível.
 * Requisito 9.1: persistência de todos os dados no Room.
 * Requisito 9.6: índice em levelId para performance de queries.
 */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = LevelEntity::class,
            parentColumns = ["id"],
            childColumns = ["level_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["level_id"])
    ]
)
data class CategoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "level_id")
    val levelId: Int,

    @ColumnInfo(name = "name_key")
    val nameKey: String,

    @ColumnInfo(name = "description_key")
    val descriptionKey: String,

    @ColumnInfo(name = "icon")
    val icon: String,

    @ColumnInfo(name = "theme_color_hex")
    val themeColorHex: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int
)
