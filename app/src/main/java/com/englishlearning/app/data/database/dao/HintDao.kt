package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.englishlearning.app.data.database.entity.HintEntity

/**
 * DAO para operações de leitura e escrita de dicas progressivas.
 *
 * Requisito 9.1: persistência da entidade Hint no Room.
 * Requisito 9.2: inserção via DatabaseSeeder na primeira inicialização.
 * Requisito 3.2: dicas reveladas progressivamente por hintOrder.
 */
@Dao
interface HintDao {

    /**
     * Retorna todas as dicas de um exercício, ordenadas por hint_order.
     * Requisito 3.2: dicas exibidas em ordem crescente de hintOrder.
     */
    @Query("SELECT * FROM hints WHERE exercise_id = :exerciseId ORDER BY hint_order")
    suspend fun getByExercise(exerciseId: Long): List<HintEntity>

    /**
     * Insere ou substitui uma lista de dicas.
     * Utilizado pelo DatabaseSeeder na primeira inicialização.
     * Requisito 9.2.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hints: List<HintEntity>)
}
