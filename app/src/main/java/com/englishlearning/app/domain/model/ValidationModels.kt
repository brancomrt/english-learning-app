package com.englishlearning.app.domain.model

/**
 * Status resultante da validação de uma pergunta do usuário.
 * Requisito 2.9: ValidationEngine retorna score em [0, 100] para qualquer entrada.
 */
enum class ValidationStatus {
    CORRECT,    // Totalmente correta
    PARTIAL,    // Parcialmente correta (aceita com penalidade)
    INCORRECT,  // Incorreta
    TOO_CLOSE   // Quase certa (threshold especial para dicas)
}

/**
 * Tipo de erro gramatical ou semântico detectado na pergunta do usuário.
 */
enum class ErrorType {
    WRONG_QUESTION_TYPE,
    MISSING_AUX_VERB,
    WRONG_AUX_VERB,
    WRONG_WH_WORD,
    INVERTED_WORD_ORDER,
    MISSING_QUESTION_MARK,
    SEMANTIC_MISMATCH,
    EXTRA_WORDS,
    TENSE_MISMATCH
}

/**
 * Sugestão de correção para um erro detectado na pergunta do usuário.
 */
data class Correction(
    val type: ErrorType,
    val messageKey: String,
    val suggestion: String
)

/**
 * Dica progressiva para auxiliar o usuário a formular a pergunta correta.
 * Requisito 3.2: dicas reveladas progressivamente por hintOrder.
 */
data class Hint(
    val id: Long = 0,
    val exerciseId: Long,
    val hintOrder: Int,       // 1 = mais suave, N = mais revelador
    val hintText: String,
    val hintType: HintType
)

/**
 * Categoria de dica conforme sua revelação.
 */
enum class HintType {
    STRUCTURAL,  // Dica sobre a estrutura gramatical
    VOCABULARY,  // Dica sobre o vocabulário necessário
    GRAMMAR,     // Dica sobre a regra gramatical
    ANSWER       // Dica reveladora (exibe parte da resposta)
}

/**
 * Resultado completo da validação de uma pergunta do usuário.
 * Requisito 2.9: score sempre em [0, 100].
 */
data class ValidationResult(
    val status: ValidationStatus,
    val score: Int,                     // 0–100
    val matchedAnswer: AcceptedAnswer?,
    val errorType: ErrorType?,
    val corrections: List<Correction>,
    val hints: List<Hint>
)
