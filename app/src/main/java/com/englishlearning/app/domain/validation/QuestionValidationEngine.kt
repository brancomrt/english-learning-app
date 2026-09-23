package com.englishlearning.app.domain.validation

import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.Hint
import com.englishlearning.app.domain.model.ValidationResult

/**
 * Contrato do motor de validação de perguntas formuladas pelo usuário.
 *
 * Requisitos:
 * - 2.1: Normalizar e validar a pergunta submetida
 * - 2.7: Retornar CORRECT quando score >= threshold e estrutura correta
 * - 2.9: Retornar score em [0, 100] para qualquer entrada
 */
interface QuestionValidationEngine {

    /**
     * Valida a pergunta formulada pelo usuário contra o exercício fornecido.
     *
     * Pipeline:
     * 1. Normalizar texto
     * 2. Detectar e verificar tipo gramatical
     * 3. Verificar estrutura gramatical
     * 4. Calcular similaridade semântica contra respostas aceitas
     *
     * Precondição: userQuestion não nulo; exercise.acceptedAnswers não vazio
     * Postcondição: result.score em [0, 100]
     */
    fun validate(
        userQuestion: String,
        exercise: Exercise
    ): ValidationResult

    /**
     * Gera uma lista de dicas progressivas para auxiliar o usuário.
     *
     * Precondição: userQuestion não nulo; exercise.hints pode ser vazio
     * Postcondição: lista ordenada por hintOrder (mais suave primeiro)
     */
    fun generateHints(
        userQuestion: String,
        exercise: Exercise
    ): List<Hint>

    /**
     * Calcula a pontuação bruta (0–100) da pergunta do usuário para o exercício.
     *
     * Postcondição: resultado em [0, 100]
     */
    fun scoreAnswer(
        userQuestion: String,
        exercise: Exercise
    ): Int
}
