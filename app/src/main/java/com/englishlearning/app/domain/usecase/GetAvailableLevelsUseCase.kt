package com.englishlearning.app.domain.usecase

import com.englishlearning.app.domain.model.Level
import com.englishlearning.app.domain.repository.ContentRepository
import javax.inject.Inject

/**
 * Retorna a lista de todos os níveis disponíveis no conteúdo.
 *
 * Requisito 6.1: conteúdo organizado em níveis progressivos.
 * Requisito 6.3: exibir níveis desbloqueados, em progresso e bloqueados.
 */
class GetAvailableLevelsUseCase @Inject constructor(
    private val contentRepository: ContentRepository
) {
    /**
     * Retorna todos os níveis ordenados por sortOrder.
     *
     * @return Lista de [Level], possivelmente vazia.
     */
    suspend operator fun invoke(): List<Level> {
        return contentRepository.getLevels()
    }
}
