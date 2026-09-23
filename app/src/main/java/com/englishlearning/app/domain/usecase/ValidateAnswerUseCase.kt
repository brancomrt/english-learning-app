package com.englishlearning.app.domain.usecase

import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.ValidationResult
import com.englishlearning.app.domain.validation.QuestionValidationEngine
import javax.inject.Inject

/**
 * Valida a resposta (pergunta formulada) do usuário para um exercício.
 *
 * Requisito 2.1: normalizar e validar a pergunta submetida via ValidationEngine.
 * Requisito 13.1: LearningViewModel delega a validação a este use case.
 *
 * @param validationEngine Motor de validação de perguntas.
 */
class ValidateAnswerUseCase @Inject constructor(
    private val validationEngine: QuestionValidationEngine
) {

    /**
     * Executa a validação sincronamente (o motor de validação é puro Kotlin,
     * sem I/O, portanto não precisa ser suspensa).
     *
     * Precondição: [userQuestion] não é nulo; [exercise].acceptedAnswers não é vazio.
     * Postcondição: resultado.score está no intervalo [0, 100] (garantido pelo engine).
     *
     * @param userQuestion Pergunta digitada pelo usuário.
     * @param exercise     Exercício atual exibido na sessão.
     * @return [ValidationResult] com status, score, correções e dicas.
     */
    operator fun invoke(userQuestion: String, exercise: Exercise): ValidationResult {
        return validationEngine.validate(userQuestion, exercise)
    }
}
