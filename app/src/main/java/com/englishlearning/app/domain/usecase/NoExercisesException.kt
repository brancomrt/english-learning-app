package com.englishlearning.app.domain.usecase

/**
 * Lançada quando o ContentRepository não retorna nenhum exercício
 * para a combinação de [levelId] e [categoryId] solicitada.
 *
 * Requisito 13.1: LoadExercisesUseCase deve lidar com resultado vazio
 * retornando falha com esta exceção, permitindo que a UI exiba
 * mensagem de erro amigável com opção de retry (Req. 12.7).
 */
class NoExercisesException(
    val levelId: Int,
    val categoryId: Int
) : Exception("Nenhum exercício encontrado para levelId=$levelId, categoryId=$categoryId")
