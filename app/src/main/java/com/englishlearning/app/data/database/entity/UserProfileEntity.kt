package com.englishlearning.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room para o perfil do usuário (app single-user).
 *
 * O id fixo = 1 reflete o design single-user do aplicativo.
 * Requisito 9.1: persistência de todos os dados no Room.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long = 1,

    @ColumnInfo(name = "display_name")
    val displayName: String = "Usuário",

    /** Total de XP acumulado pelo usuário em todos os exercícios. */
    @ColumnInfo(name = "total_xp")
    val totalXp: Int = 0,

    /** ID do nível atual do usuário, usado para desbloquear conteúdo progressivo. */
    @ColumnInfo(name = "current_level_id")
    val currentLevelId: Int = 1,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /** Timestamp da última interação do usuário com o app, em epoch millis. */
    @ColumnInfo(name = "last_active_at")
    val lastActiveAt: Long = System.currentTimeMillis()
)
