package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vocabulary_cards",
    foreignKeys = [
        ForeignKey(entity = LevelEntity::class, parentColumns = ["id"], childColumns = ["level_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["category_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["level_id"]), Index(value = ["category_id"])]
)
data class VocabularyCardEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "word") val word: String,
    @ColumnInfo(name = "definition") val definition: String,
    @ColumnInfo(name = "example_sentence") val exampleSentence: String,
    @ColumnInfo(name = "level_id") val levelId: Int,
    @ColumnInfo(name = "category_id") val categoryId: Int,
    @ColumnInfo(name = "audio_path") val audioPath: String? = null,
    /** URL de imagem ilustrativa do Unsplash para a palavra. */
    @ColumnInfo(name = "image_url") val imageUrl: String? = null
)
