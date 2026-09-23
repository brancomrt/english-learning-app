package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room para o registro de streak diário do usuário.
 * Requisito 9.1: persistência de todos os dados no Room.
 * Requisito 5.5: longestStreak preservado e atualizado quando currentStreak o supera.
 *
 * A data de última atividade é armazenada como String ISO 8601 (ex: "2024-01-15")
 * para evitar dependência de TypeConverter nesta entidade.
 */
@Entity(tableName = "streak_records")
data class StreakRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "current_streak")
    val currentStreak: Int = 0,

    @ColumnInfo(name = "longest_streak")
    val longestStreak: Int = 0,

    /** Data da última atividade no formato ISO: "yyyy-MM-dd", ou null se nunca houve atividade. */
    @ColumnInfo(name = "last_activity_date")
    val lastActivityDate: String? = null
)
