package com.englishlearning.app.domain.usecase

import com.englishlearning.app.data.repository.SessionSettingsRepository
import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.repository.ContentRepository
import javax.inject.Inject

/**
 * Carrega a lista de exercícios para uma combinação de nível e categoria,
 * respeitando o limite de questões configurado pelo usuário em [SessionSettingsRepository].
 *
 * O limite é aplicado embaralhando a lista e tomando os primeiros N itens,
 * garantindo variedade entre sessões.
 */
class LoadExercisesUseCase @Inject constructor(
    private val contentRepository: ContentRepository,
    private val settingsRepository: SessionSettingsRepository
) {

    /**
     * Retorna [Result.success] com a lista de exercícios limitada pela configuração
     * do usuário para o [levelId], ou [Result.failure] se não houver exercícios.
     *
     * Quando [categoryId] == 0, carrega todos os exercícios do nível sem filtro de categoria.
     */
    suspend operator fun invoke(levelId: Int, categoryId: Int): Result<List<Exercise>> {
        return try {
            // categoryId == 0 → todos os exercícios do nível (sem filtro de categoria)
            val all = if (categoryId == 0)
                contentRepository.getExercisesByLevel(levelId)
            else
                contentRepository.getExercisesByLevelAndCategory(levelId, categoryId)

            if (all.isEmpty()) {
                Result.failure(NoExercisesException(levelId, categoryId))
            } else {
                val limit = settingsRepository.getQuestionsLimit(levelId)
                val selected = all.shuffled().take(limit)
                Result.success(selected)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
