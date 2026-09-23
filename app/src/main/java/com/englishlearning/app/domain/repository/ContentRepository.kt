package com.englishlearning.app.domain.repository

import com.englishlearning.app.domain.model.Category
import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.Level

/**
 * Abstração de acesso ao conteúdo de exercícios.
 *
 * Requisito 7.3: suporta busca por nível, por categoria e por combinação de ambos.
 * Requisito 9.1: persiste Exercise, Level e Category no banco de dados local.
 *
 * Esta interface é Pure Kotlin — não importa nenhuma classe do Android SDK,
 * permitindo que a camada de domínio permaneça independente de infraestrutura.
 */
interface ContentRepository {

    /**
     * Retorna exercícios de um nível específico.
     *
     * Requisito 7.3: busca por nível.
     *
     * @param levelId ID do nível desejado.
     * @return Lista de exercícios do nível, possivelmente vazia.
     */
    suspend fun getExercisesByLevel(levelId: Int): List<Exercise>

    /**
     * Retorna exercícios de uma categoria específica (independente de nível).
     *
     * Requisito 7.3: busca por categoria.
     *
     * @param categoryId ID da categoria desejada.
     * @return Lista de exercícios da categoria, possivelmente vazia.
     */
    suspend fun getExercisesByCategory(categoryId: Int): List<Exercise>

    /**
     * Retorna exercícios filtrados por nível e categoria simultaneamente.
     *
     * Requisito 7.3: busca por combinação de nível e categoria.
     * Requisito 7.4: quando não há exercícios não tentados, retorna exercícios já tentados para revisão.
     *
     * @param levelId ID do nível desejado.
     * @param categoryId ID da categoria desejada.
     * @return Lista de exercícios da combinação, possivelmente vazia.
     */
    suspend fun getExercisesByLevelAndCategory(levelId: Int, categoryId: Int): List<Exercise>

    /**
     * Retorna um exercício pelo seu ID, ou null se não encontrado.
     *
     * @param id ID único do exercício.
     * @return O exercício com as respostas aceitas e dicas, ou null.
     */
    suspend fun getExerciseById(id: Long): Exercise?

    /**
     * Marca um exercício como completado pelo usuário.
     *
     * Requisito 9.3: registra tentativas no banco de dados local.
     *
     * @param exerciseId ID do exercício completado.
     * @param userId ID do usuário (padrão: 1 para app single-user).
     */
    suspend fun markExerciseCompleted(exerciseId: Long, userId: Long)

    /**
     * Retorna todas as categorias disponíveis no conteúdo.
     *
     * Requisito 7.2: exibir categorias disponíveis na CategorySelectScreen.
     *
     * @return Lista de todas as categorias, ordenadas por sortOrder.
     */
    suspend fun getCategories(): List<Category>

    /**
     * Desbloqueia um nível manualmente (avanço forçado pelo usuário).
     *
     * @param levelId ID do nível a desbloquear.
     */
    suspend fun unlockLevel(levelId: Int)

    /**
     * Retorna todos os níveis disponíveis.
     *
     * Requisito 6.1: conteúdo organizado em níveis progressivos.
     * Requisito 6.3: exibir níveis desbloqueados, em progresso e bloqueados.
     *
     * @return Lista de todos os níveis, ordenados por sortOrder.
     */
    suspend fun getLevels(): List<Level>
}
