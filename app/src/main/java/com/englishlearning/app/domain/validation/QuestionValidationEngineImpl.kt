package com.englishlearning.app.domain.validation

import com.englishlearning.app.domain.model.AcceptedAnswer
import com.englishlearning.app.domain.model.Correction
import com.englishlearning.app.domain.model.ErrorType
import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.Hint
import com.englishlearning.app.domain.model.HintType
import com.englishlearning.app.domain.model.ExerciseMode
import com.englishlearning.app.domain.model.ValidationResult
import com.englishlearning.app.domain.model.ValidationStatus

/**
 * Implementação do motor de validação de perguntas da mecânica invertida.
 *
 * O pipeline de validação é:
 * 1. Normalizar a entrada do usuário
 * 2. Detectar o tipo gramatical da pergunta
 * 3. Rejeitar se o tipo diverge do esperado pelo exercício
 * 4. Verificar a estrutura gramatical (verbo auxiliar, ordem das palavras)
 * 5. Calcular similaridade semântica contra todas as respostas aceitas
 * 6. Derivar o ValidationStatus e score final a partir dos resultados anteriores
 *
 * Requisitos: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10
 */
class QuestionValidationEngineImpl(
    private val textNormalizer: TextNormalizer,
    private val grammarParser: GrammarParser,
    private val semanticMatcher: SemanticMatcher
) : QuestionValidationEngine {

    /**
     * Executa o pipeline completo de validação.
     *
     * Precondição: exercise.acceptedAnswers não vazio
     * Postcondição: result.score em [0, 100]
     *
     * Requisitos: 2.1–2.10
     */
    override fun validate(
        userQuestion: String,
        exercise: Exercise
    ): ValidationResult {
        val normalized = textNormalizer.normalize(userQuestion)

        // Modo ANSWER_FROM_QUESTION: o usuário formula uma resposta, não uma pergunta.
        // Pula a verificação de tipo/estrutura gramatical e vai direto ao matching semântico.
        if (exercise.exerciseMode == ExerciseMode.ANSWER_FROM_QUESTION) {
            return validateAnswer(normalized, exercise)
        }

        // Passo 1: verificar tipo gramatical — Requisito 2.2, 2.3
        val detectedType = grammarParser.detectQuestionType(normalized)
        if (detectedType != exercise.questionType) {
            return ValidationResult(
                status = ValidationStatus.INCORRECT,
                score = 0,
                matchedAnswer = null,
                errorType = ErrorType.WRONG_QUESTION_TYPE,
                corrections = grammarParser.suggestTypeCorrection(normalized, exercise.questionType),
                hints = exercise.hints.take(1)
            )
        }

        // Passo 2: verificar estrutura gramatical — Requisito 2.4
        val structureResult = grammarParser.checkStructure(normalized, exercise.questionType)

        // Passo 3: matching semântico contra todas as respostas aceitas — Requisito 2.5, 2.6
        val bestMatch = exercise.acceptedAnswers
            .map { accepted ->
                val normalizedAccepted = textNormalizer.normalize(accepted.questionText)
                Pair(accepted, semanticMatcher.score(normalized, normalizedAccepted))
            }
            .maxByOrNull { it.second }

        // bestMatch nunca é null pois exercise.acceptedAnswers é garantidamente não vazio
        return computeFinalResult(structureResult, bestMatch!!, normalized, exercise)
    }

    /**
     * Valida a resposta do usuário no modo ANSWER_FROM_QUESTION.
     * Usa apenas matching semântico — sem verificação de tipo/estrutura gramatical,
     * pois o usuário está formulando uma resposta (frase declarativa), não uma pergunta.
     */
    private fun validateAnswer(normalized: String, exercise: Exercise): ValidationResult {
        if (normalized.isEmpty()) {
            return ValidationResult(
                status = ValidationStatus.INCORRECT,
                score = 0,
                matchedAnswer = null,
                errorType = ErrorType.SEMANTIC_MISMATCH,
                corrections = emptyList(),
                hints = exercise.hints.take(1)
            )
        }

        val bestMatch = exercise.acceptedAnswers
            .map { accepted ->
                val normalizedAccepted = textNormalizer.normalize(accepted.questionText)
                Pair(accepted, semanticMatcher.score(normalized, normalizedAccepted))
            }
            .maxByOrNull { it.second }
            ?: return ValidationResult(
                status = ValidationStatus.INCORRECT,
                score = 0,
                matchedAnswer = null,
                errorType = ErrorType.SEMANTIC_MISMATCH,
                corrections = emptyList(),
                hints = exercise.hints.take(1)
            )

        val (acceptedAnswer, semanticScore) = bestMatch
        val threshold = acceptedAnswer.matchThreshold

        return when {
            semanticScore >= threshold -> ValidationResult(
                status = ValidationStatus.CORRECT,
                score = minOf(100, (semanticScore * 100).toInt()),
                matchedAnswer = acceptedAnswer,
                errorType = null,
                corrections = emptyList(),
                hints = emptyList()
            )
            semanticScore >= threshold * CLOSE_THRESHOLD_FACTOR -> ValidationResult(
                status = ValidationStatus.TOO_CLOSE,
                score = minOf(50, (semanticScore * 50).toInt()),
                matchedAnswer = acceptedAnswer,
                errorType = ErrorType.SEMANTIC_MISMATCH,
                corrections = listOf(
                    Correction(
                        type = ErrorType.SEMANTIC_MISMATCH,
                        messageKey = "error_answer_close",
                        suggestion = "Quase certo! Tente ser mais preciso na resposta."
                    )
                ),
                hints = exercise.hints.take(2)
            )
            else -> ValidationResult(
                status = ValidationStatus.INCORRECT,
                score = 0,
                matchedAnswer = null,
                errorType = ErrorType.SEMANTIC_MISMATCH,
                corrections = listOf(
                    Correction(
                        type = ErrorType.SEMANTIC_MISMATCH,
                        messageKey = "error_answer_wrong",
                        suggestion = "Resposta incorreta. Revise as dicas e tente novamente."
                    )
                ),
                hints = exercise.hints.take(1)
            )
        }
    }

    /**
     * Deriva o ValidationResult final a partir da verificação de estrutura
     * e do melhor match semântico encontrado.
     *
     * Regras de decisão:
     * - score >= threshold AND estrutura válida → CORRECT
     * - score >= threshold * CLOSE_THRESHOLD_FACTOR → TOO_CLOSE  (Requisito 2.8)
     * - caso contrário → INCORRECT                               (Requisito 2.6)
     *
     * Postcondição: result.score em [0, 100]                     (Requisito 2.9)
     */
    private fun computeFinalResult(
        structureResult: StructureCheckResult,
        bestMatch: Pair<AcceptedAnswer, Float>,
        normalized: String,
        exercise: Exercise
    ): ValidationResult {
        val (acceptedAnswer, semanticScore) = bestMatch
        val threshold = acceptedAnswer.matchThreshold

        return when {
            // Resposta correta: score acima do threshold E estrutura válida — Requisito 2.7
            semanticScore >= threshold && structureResult.isValid -> ValidationResult(
                status = ValidationStatus.CORRECT,
                score = calculateScore(semanticScore, structureResult, exercise),
                matchedAnswer = acceptedAnswer,
                errorType = null,
                corrections = emptyList(),
                hints = emptyList()
            )

            // Resposta quase certa: score está dentro da faixa de tolerância — Requisito 2.8
            semanticScore >= threshold * CLOSE_THRESHOLD_FACTOR -> ValidationResult(
                status = ValidationStatus.TOO_CLOSE,
                score = calculateScore(semanticScore, structureResult, exercise) / 2,
                matchedAnswer = acceptedAnswer,
                errorType = structureResult.errorType,
                corrections = structureResult.corrections,
                hints = exercise.hints.take(2)
            )

            // Resposta incorreta — Requisito 2.6, 2.10
            else -> ValidationResult(
                status = ValidationStatus.INCORRECT,
                score = 0,
                matchedAnswer = null,
                errorType = deriveErrorType(structureResult, semanticScore, threshold),
                corrections = buildCorrections(structureResult, normalized, exercise),
                hints = exercise.hints.take(1)
            )
        }
    }

    /**
     * Calcula o score final (0–100) combinando similaridade semântica,
     * bônus de estrutura correta e multiplicador de dificuldade.
     *
     * Precondição: semanticScore em [0.0, 1.0]
     * Postcondição: resultado em [0, 100]
     */
    private fun calculateScore(
        semanticScore: Float,
        structureResult: StructureCheckResult,
        exercise: Exercise
    ): Int {
        val base = (semanticScore * 100).toInt()
        val structureBonus = if (structureResult.isValid) STRUCTURE_BONUS else 0
        val difficultyMultiplier = exercise.difficultyScore / 10.0
        return minOf(100, ((base + structureBonus) * difficultyMultiplier).toInt())
    }

    /**
     * Determina o tipo de erro mais adequado para feedback ao usuário.
     *
     * Prioriza erros estruturais sobre erros semânticos, pois são mais
     * acionáveis para o aprendiz.
     */
    private fun deriveErrorType(
        structureResult: StructureCheckResult,
        semanticScore: Float,
        threshold: Float
    ): ErrorType {
        // Erro estrutural tem prioridade sobre erro semântico
        if (!structureResult.isValid && structureResult.errorType != null) {
            return structureResult.errorType
        }
        // Nenhum erro estrutural → o problema é semântico
        return ErrorType.SEMANTIC_MISMATCH
    }

    /**
     * Constrói a lista de correções para exibir ao usuário em caso de resposta incorreta.
     *
     * Combina correções estruturais com uma correção semântica genérica quando necessário.
     * Requisito 2.10: retornar lista de Corrections com sugestões textuais.
     */
    private fun buildCorrections(
        structureResult: StructureCheckResult,
        normalized: String,
        exercise: Exercise
    ): List<Correction> {
        val corrections = mutableListOf<Correction>()

        // Adicionar correções estruturais do GrammarParser
        corrections.addAll(structureResult.corrections)

        // Adicionar correção semântica se não houver erro estrutural
        if (structureResult.isValid) {
            corrections.add(
                Correction(
                    type = ErrorType.SEMANTIC_MISMATCH,
                    messageKey = "error_semantic_mismatch",
                    suggestion = "Sua pergunta não corresponde ao significado esperado. Tente reformulá-la."
                )
            )
        }

        return corrections
    }

    /**
     * Gera dicas progressivas com base no estado atual da resposta do usuário.
     *
     * A lógica de revelação progressiva segue:
     * - Tipo errado: apenas primeira dica (estrutural)
     * - Estrutura incorreta: duas primeiras dicas
     * - Erro semântico (tipo e estrutura corretos): até três dicas
     * - Dicas do tipo ANSWER apenas quando todas as anteriores já foram exibidas
     *
     * Requisitos: 3.1, 3.2, 3.3
     */
    override fun generateHints(
        userQuestion: String,
        exercise: Exercise
    ): List<Hint> {
        val normalized = textNormalizer.normalize(userQuestion)
        val sortedHints = exercise.hints.sortedBy { it.hintOrder }

        if (normalized.isEmpty()) {
            // Sem entrada: exibir apenas a primeira dica não-reveladora
            return sortedHints
                .filter { it.hintType != HintType.ANSWER }
                .take(1)
        }

        val detectedType = grammarParser.detectQuestionType(normalized)

        // Tipo incorreto: exibir apenas primeira dica estrutural
        if (detectedType != exercise.questionType) {
            return sortedHints.take(1)
        }

        val structureResult = grammarParser.checkStructure(normalized, exercise.questionType)

        // Estrutura incorreta: exibir até 2 dicas
        if (!structureResult.isValid) {
            return sortedHints.take(2)
        }

        // Tipo e estrutura corretos mas semanticamente errado: exibir até 3 dicas
        // incluindo dicas do tipo ANSWER somente se todas as não-reveladoras já foram exibidas
        val nonAnswerHints = sortedHints.filter { it.hintType != HintType.ANSWER }
        return if (nonAnswerHints.size <= 2) {
            // Todas as dicas normais já foram mostradas: liberar dicas ANSWER
            sortedHints.take(3)
        } else {
            nonAnswerHints.take(3)
        }
    }

    /**
     * Retorna o score bruto (0–100) da pergunta do usuário para o exercício,
     * sem derivar o ValidationStatus completo.
     *
     * Postcondição: resultado em [0, 100]           (Requisito 2.9)
     */
    override fun scoreAnswer(
        userQuestion: String,
        exercise: Exercise
    ): Int {
        val normalized = textNormalizer.normalize(userQuestion)

        val detectedType = grammarParser.detectQuestionType(normalized)
        if (detectedType != exercise.questionType) return 0

        val structureResult = grammarParser.checkStructure(normalized, exercise.questionType)

        val bestMatch = exercise.acceptedAnswers
            .map { accepted ->
                Pair(accepted, semanticMatcher.score(normalized, textNormalizer.normalize(accepted.questionText)))
            }
            .maxByOrNull { it.second }
            ?: return 0

        val (_, semanticScore) = bestMatch
        return calculateScore(semanticScore, structureResult, exercise)
    }

    companion object {
        /** Fator do threshold para detectar resposta "quase certa" (TOO_CLOSE). Requisito 2.8 */
        const val CLOSE_THRESHOLD_FACTOR = 0.8f

        /** Bônus adicionado ao score base quando a estrutura gramatical está correta. */
        const val STRUCTURE_BONUS = 10
    }
}
