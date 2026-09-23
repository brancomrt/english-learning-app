package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.englishlearning.app.data.database.entity.CategoryEntity

/**
 * DAO para operações de leitura e escrita de categorias temáticas.
 *
 * Requisito 9.1: persistência da entidade Category no Room.
 * Requisito 9.2: inserção via DatabaseSeeder na primeira inicialização.
 * Requisito 7.1: categorias organizadas por nível.
 */
@Dao
interface CategoryDao {

    /**
     * Retorna todas as categorias de um nível, ordenadas por sort_order.
     * Requisito 7.2: exibir categorias do nível na CategorySelectScreen.
     */
    @Query("SELECT * FROM categories WHERE level_id = :levelId ORDER BY sort_order")
    suspend fun getByLevel(levelId: Int): List<CategoryEntity>

    /**
     * Retorna todas as categorias do banco, ordenadas por level_id e sort_order.
     * Requisito 7.3: ContentRepository busca categorias.
     */
    @Query("SELECT * FROM categories ORDER BY level_id, sort_order")
    suspend fun getAll(): List<CategoryEntity>

    /**
     * Retorna uma categoria pelo id.
     */
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getById(categoryId: Int): CategoryEntity?

    /**
     * Insere ou substitui uma lista de categorias.
     * Utilizado pelo DatabaseSeeder na primeira inicialização.
     * Requisito 9.2.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)
}
