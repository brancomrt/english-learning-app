package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.englishlearning.app.data.database.entity.VocabularyCardEntity

/**
 * DAO para operações de leitura e escrita de vocabulary cards.
 *
 * Suporta busca por nível, por categoria e por combinação nível+categoria,
 * seguindo o mesmo padrão do [ExerciseDao] para consistência na camada de dados.
 *
 * Requisito 16.8: suporte a VocabularyCards no fluxo SRS.
 * Requisito 16.9: campos word, definition (PT-BR), exampleSentence (inglês),
 *                 levelId e categoryId persistidos corretamente.
 */
@Dao
interface VocabularyDao {

    /**
     * Retorna todos os vocabulary cards de um nível específico,
     * ordenados pelo ID (ordem de inserção).
     *
     * Requisito 16.8: acesso a vocabulary cards por nível.
     */
    @Query("SELECT * FROM vocabulary_cards WHERE level_id = :levelId ORDER BY id")
    suspend fun getByLevel(levelId: Int): List<VocabularyCardEntity>

    /**
     * Retorna todos os vocabulary cards de uma categoria específica.
     *
     * Requisito 16.8: acesso a vocabulary cards por categoria.
     */
    @Query("SELECT * FROM vocabulary_cards WHERE category_id = :categoryId ORDER BY id")
    suspend fun getByCategory(categoryId: Int): List<VocabularyCardEntity>

    /**
     * Retorna vocabulary cards de uma combinação nível + categoria.
     * Utilizado por SrsRepository.getVocabularyCards.
     *
     * Requisito 16.8: busca de VocabularyCards para exibição na SrsReviewScreen.
     */
    @Query("""
        SELECT * FROM vocabulary_cards
        WHERE level_id = :levelId AND category_id = :categoryId
        ORDER BY id
    """)
    suspend fun getByLevelAndCategory(levelId: Int, categoryId: Int): List<VocabularyCardEntity>

    /**
     * Retorna um vocabulary card pelo ID, ou null se não encontrado.
     * Usado para recuperar o conteúdo a exibir durante a revisão SRS.
     */
    @Query("SELECT * FROM vocabulary_cards WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VocabularyCardEntity?

    /**
     * Insere ou substitui uma lista de vocabulary cards.
     * Utilizado pelo DatabaseSeeder ao popular o banco com dados dos assets JSON.
     *
     * Requisito 18.8: DatabaseSeeder popula VocabularyCardEntity.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<VocabularyCardEntity>)
}
