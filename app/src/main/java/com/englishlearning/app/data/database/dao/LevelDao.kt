package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.englishlearning.app.data.database.entity.LevelEntity

/**
 * DAO para operações de leitura e escrita de níveis.
 *
 * Requisito 9.1: persistência da entidade Level no Room.
 * Requisito 9.2: inserção via DatabaseSeeder na primeira inicialização.
 * Requisito 6.1: níveis organizados em progressão (sort_order).
 */
@Dao
interface LevelDao {

    /**
     * Retorna todos os níveis ordenados por sort_order.
     * Requisito 6.3: exibir níveis na LevelSelectScreen.
     */
    @Query("SELECT * FROM levels ORDER BY sort_order")
    suspend fun getAll(): List<LevelEntity>

    /**
     * Retorna um nível pelo id.
     */
    @Query("SELECT * FROM levels WHERE id = :levelId")
    suspend fun getById(levelId: Int): LevelEntity?

    /**
     * Insere ou substitui uma lista de níveis.
     * Utilizado pelo DatabaseSeeder na primeira inicialização.
     * Requisito 9.2.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(levels: List<LevelEntity>)

    /**
     * Atualiza o estado de desbloqueio de um nível.
     * Requisito 6.2: desbloquear próximo nível ao atingir critérios mínimos.
     */
    @Query("UPDATE levels SET is_unlocked = :isUnlocked WHERE id = :levelId")
    suspend fun updateUnlocked(levelId: Int, isUnlocked: Boolean)
}
