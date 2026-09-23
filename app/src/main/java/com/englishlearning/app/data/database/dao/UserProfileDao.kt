package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.englishlearning.app.data.database.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO dedicado ao perfil do usuário.
 * Separado do ProgressDao para responsabilidade única e facilidade de injeção.
 *
 * Requisito 9.1: persistência do UserProfile.
 * Requisito 5.6: HomeScreen observa perfil para exibir nome e XP total.
 */
@Dao
interface UserProfileDao {

    /**
     * Observa o perfil do usuário em tempo real.
     * Retorna null enquanto o perfil ainda não foi criado (primeira inicialização).
     * Requisito 9.1.
     */
    @Query("SELECT * FROM user_profile WHERE id = :userId LIMIT 1")
    fun getProfile(userId: Long): Flow<UserProfileEntity?>

    /**
     * Insere ou atualiza o perfil do usuário.
     * Requisito 9.1.
     */
    @Upsert
    suspend fun upsert(profile: UserProfileEntity)
}
