package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.englishlearning.app.data.database.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para conquistas do usuário.
 * Suporta inserção idempotente, observação reativa e marcação de conquistas visualizadas.
 *
 * Requisito 8.2: ProgressTracker desbloqueia conquista e registra no banco.
 * Requisito 8.4: exibição de todas as conquistas na ProgressScreen via Flow.
 * Requisito 8.5: marcar isNew = false quando o usuário visualiza a conquista.
 */
@Dao
interface AchievementDao {

    /**
     * Observa a lista de conquistas do usuário em tempo real.
     * Inclui todas as conquistas desbloqueadas, novas ou já vistas.
     * Requisito 8.4.
     */
    @Query("SELECT * FROM achievements WHERE user_id = :userId ORDER BY unlocked_at DESC")
    fun getAchievements(userId: Long): Flow<List<AchievementEntity>>

    /**
     * Insere uma conquista, ignorando se já existir (índice único em user_id + achievement_key).
     * Garante idempotência: desbloquear a mesma conquista duas vezes não gera duplicata.
     * Requisito 8.2.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(achievement: AchievementEntity)

    /**
     * Marca uma conquista específica como visualizada (is_new = 0).
     * Chamado quando a notificação de conquista é dispensada pelo usuário.
     * Requisito 8.5.
     */
    @Query("""
        UPDATE achievements
        SET is_new = 0
        WHERE user_id = :userId AND achievement_key = :achievementKey
    """)
    suspend fun markSeen(userId: Long, achievementKey: String)

    /**
     * Marca todas as conquistas do usuário como visualizadas (is_new = 0).
     * Chamado quando o usuário abre a ProgressScreen e visualiza todas as conquistas.
     * Requisito 8.5.
     */
    @Query("""
        UPDATE achievements
        SET is_new = 0
        WHERE user_id = :userId
    """)
    suspend fun markAllSeen(userId: Long)
}
