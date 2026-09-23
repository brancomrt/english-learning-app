package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.englishlearning.app.data.database.entity.AcceptedAnswerEntity

/**
 * DAO para operações de leitura e escrita de respostas aceitas.
 *
 * Requisito 9.1: persistência da entidade AcceptedAnswer no Room.
 * Requisito 9.2: inserção via DatabaseSeeder na primeira inicialização.
 * Requisito 2.5: SemanticMatcher compara com respostas aceitas do exercício.
 */
@Dao
interface AcceptedAnswerDao {

    /**
     * Retorna todas as respostas aceitas de um exercício.
     */
    @Query("SELECT * FROM accepted_answers WHERE exercise_id = :exerciseId")
    suspend fun getByExercise(exerciseId: Long): List<AcceptedAnswerEntity>

    /**
     * Insere ou substitui uma lista de respostas aceitas.
     * Utilizado pelo DatabaseSeeder na primeira inicialização.
     * Requisito 9.2.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(answers: List<AcceptedAnswerEntity>)
}
