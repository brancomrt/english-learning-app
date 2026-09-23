package com.englishlearning.app.data.repository

import com.englishlearning.app.data.database.dao.CategoryDao
import com.englishlearning.app.data.database.dao.ExerciseDao
import com.englishlearning.app.data.database.dao.LevelDao
import com.englishlearning.app.data.database.dao.ProgressDao
import com.englishlearning.app.data.database.entity.AttemptEntity
import com.englishlearning.app.domain.model.Category
import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.Level
import com.englishlearning.app.domain.model.ValidationStatus
import com.englishlearning.app.domain.repository.ContentRepository
import javax.inject.Inject

/**
 * Implementação de [ContentRepository] que acessa o banco de dados local via Room DAOs.
 *
 * Requisito 7.3: suporta busca de exercícios por nível, por categoria, e por combinação.
 * Requisito 7.4: quando não há exercícios não tentados, retorna exercícios já tentados
 *   para revisão (lógica de fallback em [getExercisesByLevelAndCategory]).
 * Requisito 9.4: persistência e acesso offline completo.
 */
class ContentRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val levelDao: LevelDao,
    private val categoryDao: CategoryDao,
    private val progressDao: ProgressDao
) : ContentRepository {

    /**
     * Retorna exercícios de um nível específico com respostas aceitas e dicas.
     *
     * Requisito 7.3: busca por nível.
     *
     * @param levelId ID do nível desejado.
     * @return Lista de exercícios completos (com respostas e dicas), possivelmente vazia.
     */
    override suspend fun getExercisesByLevel(levelId: Int): List<Exercise> {
        return exerciseDao.getWithAnswersByLevel(levelId).map { it.toDomain() }
    }

    /**
     * Retorna exercícios de uma categoria específica com respostas aceitas e dicas.
     *
     * Requisito 7.3: busca por categoria.
     *
     * @param categoryId ID da categoria desejada.
     * @return Lista de exercícios completos, possivelmente vazia.
     */
    override suspend fun getExercisesByCategory(categoryId: Int): List<Exercise> {
        return exerciseDao.getWithAnswersByCategory(categoryId).map { it.toDomain() }
    }

    /**
     * Retorna exercícios filtrados por nível e categoria com lógica de fallback.
     *
     * Tenta primeiro retornar exercícios que o usuário ainda não tentou (userId = 1,
     * o usuário padrão do app single-user). Se não houver exercícios não tentados,
     * retorna todos os exercícios da combinação para revisão.
     *
     * Requisito 7.3: busca por combinação de nível e categoria.
     * Requisito 7.4: fallback para exercícios já tentados quando não há novos.
     *
     * @param levelId ID do nível desejado.
     * @param categoryId ID da categoria desejada.
     * @return Lista de exercícios completos: preferencialmente não tentados; caso contrário, todos.
     */
    override suspend fun getExercisesByLevelAndCategory(
        levelId: Int,
        categoryId: Int
    ): List<Exercise> {
        return getExercisesForSession(levelId, categoryId, userId = DEFAULT_USER_ID)
    }

    /**
     * Retorna um exercício completo (com respostas aceitas e dicas) pelo ID.
     *
     * @param id ID único do exercício.
     * @return O exercício completo, ou null se não encontrado.
     */
    override suspend fun getExerciseById(id: Long): Exercise? {
        return exerciseDao.getWithAnswersById(id)?.toDomain()
    }

    /**
     * Marca um exercício como completado registrando uma tentativa no banco.
     *
     * Insere um registro de [AttemptEntity] com status CORRECT e score 0 (mínimo)
     * para indicar que o exercício foi visitado/completado pelo usuário. Esse registro
     * é usado pelas queries de exercícios não tentados (Requisito 7.4).
     *
     * Requisito 9.3: registrar tentativas no banco de dados local.
     *
     * @param exerciseId ID do exercício completado.
     * @param userId ID do usuário (padrão: 1 para app single-user).
     */
    override suspend fun markExerciseCompleted(exerciseId: Long, userId: Long) {
        val attempt = AttemptEntity(
            userId = userId,
            exerciseId = exerciseId,
            userAnswer = "",
            validationStatus = ValidationStatus.CORRECT.name,
            score = 0,
            xpEarned = 0,
            attemptedAt = System.currentTimeMillis()
        )
        progressDao.insertAttempt(attempt)
    }

    /**
     * Retorna todas as categorias disponíveis, ordenadas por level_id e sort_order.
     *
     * Requisito 7.2: exibir categorias disponíveis na CategorySelectScreen.
     *
     * @return Lista de todas as categorias.
     */
    override suspend fun getCategories(): List<Category> {
        return categoryDao.getAll().map { it.toDomain() }
    }

    /**
     * Retorna todos os níveis disponíveis, ordenados por sort_order.
     *
     * Requisito 6.1: conteúdo organizado em níveis progressivos.
     * Requisito 6.3: exibir níveis desbloqueados, em progresso e bloqueados.
     *
     * @return Lista de todos os níveis.
     */
    override suspend fun getLevels(): List<Level> {
        return levelDao.getAll().map { it.toDomain() }
    }

    override suspend fun unlockLevel(levelId: Int) {
        levelDao.updateUnlocked(levelId, isUnlocked = true)
    }

    /**
     * Carrega exercícios para uma sessão de aprendizado com lógica de fallback.
     *
     * Primeiro tenta buscar exercícios não tentados pelo usuário nessa combinação
     * de nível + categoria. Se não houver exercícios não tentados (todos já foram
     * realizados), retorna todos os exercícios da combinação para revisão.
     *
     * Requisito 7.4: quando uma categoria não possui exercícios não tentados pelo
     * usuário, o ContentRepository retorna exercícios já tentados para revisão.
     *
     * @param levelId ID do nível desejado.
     * @param categoryId ID da categoria desejada.
     * @param userId ID do usuário para verificar tentativas anteriores.
     * @return Lista de exercícios: não tentados se disponíveis, ou todos para revisão.
     */
    private suspend fun getExercisesForSession(
        levelId: Int,
        categoryId: Int,
        userId: Long
    ): List<Exercise> {
        val notAttempted = exerciseDao.getNotAttemptedWithAnswers(levelId, categoryId, userId)

        return if (notAttempted.isNotEmpty()) {
            // Caminho feliz: há exercícios novos para o usuário
            notAttempted.map { it.toDomain() }
        } else {
            // Fallback (Requisito 7.4): todos os exercícios para revisão
            exerciseDao.getWithAnswersByLevelAndCategory(levelId, categoryId)
                .map { it.toDomain() }
        }
    }

    companion object {
        /**
         * ID do usuário padrão para o app single-user.
         * Utilizado nas queries de exercícios não tentados quando o userId não é
         * fornecido explicitamente pela chamada pública da interface.
         */
        const val DEFAULT_USER_ID = 1L
    }
}
