package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.englishlearning.app.data.database.entity.ExerciseEntity

/**
 * DAO para operações de leitura e escrita de exercícios.
 * Suporta busca por nível, categoria, combinação e exercícios não tentados.
 * Requisito 7.3: ContentRepository busca por nível, categoria e combinação.
 * Requisito 9.3: inserção de exercícios com substituição em caso de conflito.
 * Requisito 9.6: índices em level_id e category_id garantem performance.
 */
@Dao
interface ExerciseDao {

    /**
     * Retorna todos os exercícios de um nível.
     * Requisito 7.3.
     */
    @Query("SELECT * FROM exercises WHERE level_id = :levelId")
    suspend fun getByLevel(levelId: Int): List<ExerciseEntity>

    /**
     * Retorna todos os exercícios de uma categoria.
     * Requisito 7.3.
     */
    @Query("SELECT * FROM exercises WHERE category_id = :categoryId")
    suspend fun getByCategory(categoryId: Int): List<ExerciseEntity>

    /**
     * Retorna exercícios de uma combinação nível + categoria, ordenados por sort_order.
     * Requisito 7.3.
     */
    @Query("""
        SELECT * FROM exercises
        WHERE level_id = :levelId AND category_id = :categoryId
        ORDER BY sort_order
    """)
    suspend fun getByLevelAndCategory(levelId: Int, categoryId: Int): List<ExerciseEntity>

    /**
     * Retorna exercícios de nível + categoria que o usuário ainda não tentou.
     * Usado para apresentar apenas exercícios novos durante a sessão.
     * Requisito 7.4: quando não há exercícios não tentados, retorna lista vazia
     * (ContentRepository deve então buscar todos os exercícios para revisão).
     */
    @Query("""
        SELECT e.* FROM exercises e
        LEFT JOIN attempts a ON a.exercise_id = e.id AND a.user_id = :userId
        WHERE e.level_id = :levelId AND e.category_id = :categoryId AND a.id IS NULL
        ORDER BY e.sort_order
    """)
    suspend fun getNotAttempted(levelId: Int, categoryId: Int, userId: Long): List<ExerciseEntity>

    /**
     * Insere ou substitui uma lista de exercícios no banco.
     * Utilizado pelo DatabaseSeeder na primeira inicialização.
     * Requisito 9.2.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    /**
     * Conta exercícios de uma combinação nível + categoria.
     * Utilizado para calcular taxa de progresso.
     * Requisito 6.5.
     */
    @Query("SELECT COUNT(*) FROM exercises WHERE level_id = :levelId AND category_id = :categoryId")
    suspend fun countByLevelAndCategory(levelId: Int, categoryId: Int): Int

    /**
     * Retorna um exercício com suas respostas aceitas e dicas pelo ID.
     * Utilizado por ContentRepository.getExerciseById.
     */
    @Transaction
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getWithAnswersById(id: Long): ExerciseWithAnswers?

    /**
     * Retorna todos os exercícios de um nível com respostas aceitas e dicas.
     * Utilizado por ContentRepository.getExercisesByLevel.
     * Requisito 7.3.
     */
    @Transaction
    @Query("SELECT * FROM exercises WHERE level_id = :levelId ORDER BY sort_order")
    suspend fun getWithAnswersByLevel(levelId: Int): List<ExerciseWithAnswers>

    /**
     * Retorna todos os exercícios de uma categoria com respostas aceitas e dicas.
     * Utilizado por ContentRepository.getExercisesByCategory.
     * Requisito 7.3.
     */
    @Transaction
    @Query("SELECT * FROM exercises WHERE category_id = :categoryId ORDER BY sort_order")
    suspend fun getWithAnswersByCategory(categoryId: Int): List<ExerciseWithAnswers>

    /**
     * Retorna exercícios de nível + categoria com respostas aceitas e dicas.
     * Utilizado por ContentRepository.getExercisesByLevelAndCategory.
     * Requisito 7.3.
     */
    @Transaction
    @Query("""
        SELECT * FROM exercises
        WHERE level_id = :levelId AND category_id = :categoryId
        ORDER BY sort_order
    """)
    suspend fun getWithAnswersByLevelAndCategory(
        levelId: Int,
        categoryId: Int
    ): List<ExerciseWithAnswers>

    /**
     * Retorna exercícios não tentados (nível + categoria) com respostas aceitas e dicas.
     * Utilizado pela lógica de fallback do Requisito 7.4.
     */
    @Transaction
    @Query("""
        SELECT e.* FROM exercises e
        LEFT JOIN attempts a ON a.exercise_id = e.id AND a.user_id = :userId
        WHERE e.level_id = :levelId AND e.category_id = :categoryId AND a.id IS NULL
        ORDER BY e.sort_order
    """)
    suspend fun getNotAttemptedWithAnswers(
        levelId: Int,
        categoryId: Int,
        userId: Long
    ): List<ExerciseWithAnswers>
}
