package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.englishlearning.app.data.database.entity.AttemptEntity
import com.englishlearning.app.data.database.entity.UserProfileEntity
import com.englishlearning.app.data.database.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de progresso do usuário: progresso por nível/categoria,
 * tentativas individuais e estatísticas consolidadas.
 *
 * Requisito 9.1: persistência de UserProgress e Attempt.
 * Requisito 9.3: registrar tentativas com todos os campos.
 * Requisito 9.6: queries otimizadas via índices em user_id e level_id.
 */
@Dao
interface ProgressDao {

    /**
     * Insere ou atualiza o progresso do usuário para um nível/categoria.
     * Usa índice único (user_id, level_id, category_id) para substituição.
     * Requisito 9.1.
     */
    @Upsert
    suspend fun upsertProgress(progress: UserProgressEntity)

    /**
     * Observa em tempo real o progresso do usuário em todos os categorias de um nível.
     * Retorna Flow para atualização automática da UI.
     * Requisito 6.3: exibição de níveis desbloqueados/em progresso.
     */
    @Query("""
        SELECT * FROM user_progress
        WHERE user_id = :userId AND level_id = :levelId
    """)
    fun getProgressByUserAndLevel(userId: Long, levelId: Int): Flow<List<UserProgressEntity>>

    /**
     * Retorna o progresso para uma combinação específica de usuário, nível e categoria.
     * Retorna null se ainda não existe progresso registrado.
     * Requisito 7.3.
     */
    @Query("""
        SELECT * FROM user_progress
        WHERE user_id = :userId AND level_id = :levelId AND category_id = :categoryId
        LIMIT 1
    """)
    suspend fun getProgressByUserLevelAndCategory(
        userId: Long,
        levelId: Int,
        categoryId: Int
    ): UserProgressEntity?

    /**
     * Insere uma tentativa de resposta do usuário.
     * Requisito 9.3: registrar userAnswer, validationStatus, score, xpEarned e timestamp.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: AttemptEntity)

    /**
     * Conta o número de tentativas do usuário para um exercício específico.
     * Usado para controle de dicas progressivas (Requisito 3.2) e cálculo de XP (Requisito 4.3).
     */
    @Query("""
        SELECT COUNT(*) FROM attempts
        WHERE user_id = :userId AND exercise_id = :exerciseId
    """)
    suspend fun getAttemptCount(userId: Long, exerciseId: Long): Int

    /**
     * Retorna estatísticas consolidadas do usuário agregando dados de tentativas.
     * Requisito 9.3: agrega total de XP, exercícios únicos, respostas corretas e tentativas.
     *
     * A coluna validation_status é comparada com 'CORRECT' (valor do enum ValidationStatus).
     */
    @Query("""
        SELECT
            COALESCE(SUM(xp_earned), 0)                                     AS totalXp,
            COUNT(DISTINCT exercise_id)                                      AS totalExercises,
            SUM(CASE WHEN validation_status = 'CORRECT' THEN 1 ELSE 0 END)  AS correctAnswers,
            COUNT(*)                                                          AS totalAttempts
        FROM attempts
        WHERE user_id = :userId
    """)
    suspend fun getOverallStats(userId: Long): UserStatsEntity

    /**
     * Observa o perfil do usuário em tempo real.
     * Retorna null enquanto o perfil ainda não foi criado.
     * Requisito 9.1.
     */
    @Query("SELECT * FROM user_profile WHERE id = :userId LIMIT 1")
    fun getUserProfile(userId: Long): Flow<UserProfileEntity?>

    /**
     * Insere ou atualiza o perfil do usuário.
     * Requisito 9.1.
     */
    @Upsert
    suspend fun upsertProfile(profile: UserProfileEntity)
}
