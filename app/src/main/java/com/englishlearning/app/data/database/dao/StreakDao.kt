package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.englishlearning.app.data.database.entity.StreakRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para leitura e atualização do streak diário do usuário.
 * Usa Flow para que a HomeScreen observe mudanças em tempo real.
 *
 * Requisito 5.1–5.5: cálculo e persistência do streak.
 * Requisito 5.6: exibição do currentStreak na HomeScreen via observação reativa.
 */
@Dao
interface StreakDao {

    /**
     * Observa o registro de streak do usuário em tempo real.
     * Retorna null se o usuário ainda não iniciou nenhuma atividade.
     * Requisito 5.6: currentStreak exibido reativamente na HomeScreen.
     */
    @Query("SELECT * FROM streak_records WHERE user_id = :userId LIMIT 1")
    fun getStreak(userId: Long): Flow<StreakRecordEntity?>

    /**
     * Insere ou atualiza o registro de streak do usuário.
     * Chamado pelo StreakCalculator após cada exercício completado.
     * Requisito 5.1–5.5.
     */
    @Upsert
    suspend fun upsertStreak(streak: StreakRecordEntity)
}
