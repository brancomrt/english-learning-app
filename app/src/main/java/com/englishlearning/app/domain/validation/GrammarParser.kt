package com.englishlearning.app.domain.validation

import com.englishlearning.app.domain.model.Correction
import com.englishlearning.app.domain.model.ErrorType
import com.englishlearning.app.domain.model.QuestionType

/**
 * Resultado da verificação de estrutura gramatical de uma pergunta.
 *
 * Requisito 2.4: GrammarParser verifica estrutura gramatical (verbo auxiliar, ordem das palavras).
 */
data class StructureCheckResult(
    val isValid: Boolean,
    val errorType: ErrorType?,
    val corrections: List<Correction>
)

/**
 * Analisa e verifica a estrutura gramatical das perguntas formuladas pelo usuário.
 *
 * Requisitos:
 * - 1.4: Suporte a todos os 13 tipos de perguntas
 * - 2.2: Detectar o tipo gramatical da pergunta submetida
 * - 2.4: Verificar estrutura gramatical (verbo auxiliar, ordem das palavras)
 */
class GrammarParser {

    /**
     * Mapeamento de palavras interrogativas para seus QuestionTypes.
     * Ordenado por comprimento decrescente para que "how much" e "how many"
     * sejam verificados antes de "how".
     */
    private val whWords: Map<String, QuestionType> = mapOf(
        "what" to QuestionType.WH_WHAT,
        "where" to QuestionType.WH_WHERE,
        "when" to QuestionType.WH_WHEN,
        "who" to QuestionType.WH_WHO,
        "why" to QuestionType.WH_WHY,
        "how much" to QuestionType.WH_HOW_MUCH,
        "how many" to QuestionType.WH_HOW_MANY,
        "how" to QuestionType.WH_HOW,
        "which" to QuestionType.WH_WHICH
    )

    /**
     * Conjunto completo de verbos auxiliares em inglês.
     */
    private val auxVerbs: Set<String> = setOf(
        "do", "does", "did", "is", "are", "was", "were",
        "have", "has", "had", "will", "would", "can", "could",
        "should", "shall", "may", "might", "must"
    )

    /**
     * Detecta o tipo gramatical de uma pergunta normalizada.
     *
     * A verificação segue a seguinte ordem:
     * 1. Wh-questions (por comprimento decrescente para "how much/many" antes de "how")
     * 2. Tag questions (padrão regex: "..., aux + pronoun")
     * 3. Indirect questions (starters conhecidos)
     * 4. Yes/No questions (começa com verbo auxiliar)
     * 5. Fallback para YES_NO
     *
     * Precondição: normalized é string normalizada (lowercase, sem pontuação extra)
     */
    fun detectQuestionType(normalized: String): QuestionType {
        // Verificar Wh-questions (ordem por comprimento para "how much" antes de "how")
        whWords.entries
            .sortedByDescending { it.key.length }
            .forEach { (word, type) ->
                if (normalized.startsWith(word)) return type
            }

        // Verificar Tag questions (padrão: "..., aux + pronome?")
        if (isTagQuestion(normalized)) return QuestionType.TAG

        // Verificar Indirect questions (starters conhecidos)
        if (isIndirectQuestion(normalized)) return QuestionType.INDIRECT

        // Verificar Yes/No question (começa com verbo auxiliar)
        val firstWord = normalized.split(" ").firstOrNull() ?: ""
        return if (firstWord in auxVerbs) QuestionType.YES_NO
        else QuestionType.YES_NO // fallback
    }

    /**
     * Verifica a estrutura gramatical da pergunta com base no tipo esperado.
     *
     * Delega para métodos especializados por tipo de pergunta.
     *
     * Precondição: normalized é string normalizada (lowercase, sem pontuação extra)
     */
    fun checkStructure(
        normalized: String,
        expectedType: QuestionType
    ): StructureCheckResult {
        val tokens = normalized.split(" ")
        return when (expectedType) {
            QuestionType.YES_NO -> checkYesNoStructure(tokens)
            QuestionType.TAG -> checkTagStructure(tokens)
            QuestionType.INDIRECT -> checkIndirectStructure(tokens)
            else -> checkWhStructure(tokens, expectedType)
        }
    }

    /**
     * Verifica se a pergunta Yes/No possui estrutura correta.
     *
     * Estrutura esperada: AUX_VERB + SUBJECT + VERB + (complement)?
     * A pergunta deve começar com um verbo auxiliar.
     *
     * Precondição: tokens não vazio
     */
    private fun checkYesNoStructure(tokens: List<String>): StructureCheckResult {
        val firstToken = tokens.firstOrNull() ?: return StructureCheckResult(
            isValid = false,
            errorType = ErrorType.MISSING_AUX_VERB,
            corrections = emptyList()
        )
        val isValid = firstToken in auxVerbs
        return StructureCheckResult(
            isValid = isValid,
            errorType = if (!isValid) ErrorType.MISSING_AUX_VERB else null,
            corrections = if (!isValid) listOf(
                Correction(
                    type = ErrorType.MISSING_AUX_VERB,
                    messageKey = "error_missing_aux_verb",
                    suggestion = "Comece com um verbo auxiliar: do, does, is, are, will..."
                )
            ) else emptyList()
        )
    }

    /**
     * Verifica se a pergunta Wh- possui estrutura correta.
     *
     * Estrutura esperada: WH_WORD + AUX_VERB + SUBJECT + VERB + (complement)?
     * Para "how much" e "how many", o segundo token também faz parte da Wh-word.
     *
     * Precondição: tokens não vazio, type é um dos tipos Wh-
     */
    private fun checkWhStructure(
        tokens: List<String>,
        type: QuestionType
    ): StructureCheckResult {
        // Pergunta incompleta
        if (tokens.size < 3) return StructureCheckResult(
            isValid = false,
            errorType = ErrorType.INVERTED_WORD_ORDER,
            corrections = listOf(
                Correction(
                    type = ErrorType.INVERTED_WORD_ORDER,
                    messageKey = "error_incomplete_question",
                    suggestion = "A pergunta está incompleta."
                )
            )
        )

        // Pular os tokens que fazem parte da Wh-word composta (how much / how many = 2 tokens)
        val whWordTokenCount = when (type) {
            QuestionType.WH_HOW_MUCH, QuestionType.WH_HOW_MANY -> 2
            else -> 1
        }

        val afterWh = tokens.drop(whWordTokenCount)

        // A estrutura gramatical de WH-questions aceita duas formas:
        //  • WH + AUX + ...              ex: "Where do you live?"
        //  • WH + NOUN/PRONOUN + AUX + ...  ex: "Which one do you prefer?",
        //                                    ex: "How many books do you have?"
        // Portanto verificamos o auxiliar na posição 0 OU na posição 1 após a wh-phrase.
        //
        // Para WH_WHO também aceitamos subject questions (sem auxiliar):
        //  • "Who called you?" → verbo na posição 0, sem auxiliar — estrutura válida.
        val hasAux = when (type) {
            QuestionType.WH_WHO ->
                afterWh.getOrNull(0) in auxVerbs
                    || afterWh.getOrNull(1) in auxVerbs
                    || afterWh.size >= 2   // subject question: "Who called you?"
            else ->
                afterWh.getOrNull(0) in auxVerbs || afterWh.getOrNull(1) in auxVerbs
        }

        return StructureCheckResult(
            isValid = hasAux,
            errorType = if (!hasAux) ErrorType.MISSING_AUX_VERB else null,
            corrections = if (!hasAux) listOf(
                Correction(
                    type = ErrorType.MISSING_AUX_VERB,
                    messageKey = "error_wh_missing_aux",
                    suggestion = "Após a palavra interrogativa, use um verbo auxiliar."
                )
            ) else emptyList()
        )
    }

    /**
     * Verifica se a Tag question possui estrutura correta.
     *
     * Delega para [isTagQuestion] para validar o padrão regex.
     */
    private fun checkTagStructure(tokens: List<String>): StructureCheckResult {
        val isValid = isTagQuestion(tokens.joinToString(" "))
        return StructureCheckResult(
            isValid = isValid,
            errorType = if (!isValid) ErrorType.INVERTED_WORD_ORDER else null,
            corrections = if (!isValid) listOf(
                Correction(
                    type = ErrorType.INVERTED_WORD_ORDER,
                    messageKey = "error_tag_structure",
                    suggestion = "Tag questions seguem o padrão: declaração + , + auxiliar + pronome? (ex: You like coffee, don't you?)"
                )
            ) else emptyList()
        )
    }

    /**
     * Verifica se a Indirect question possui estrutura correta.
     *
     * Indirect questions têm ordem de palavras normal (não invertida) e devem
     * ter pelo menos 5 tokens para ser consideradas completas.
     */
    private fun checkIndirectStructure(tokens: List<String>): StructureCheckResult {
        val isValid = tokens.size >= 5
        return StructureCheckResult(
            isValid = isValid,
            errorType = if (!isValid) ErrorType.INVERTED_WORD_ORDER else null,
            corrections = if (!isValid) listOf(
                Correction(
                    type = ErrorType.INVERTED_WORD_ORDER,
                    messageKey = "error_indirect_incomplete",
                    suggestion = "Perguntas indiretas devem ser mais completas. Ex: Could you tell me where he lives?"
                )
            ) else emptyList()
        )
    }

    /**
     * Verifica se o texto corresponde ao padrão de Tag question.
     *
     * Suporta dois formatos:
     * 1. Texto com vírgula e contrações: "You like coffee, don't you?"
     * 2. Texto normalizado (vírgula e contrações expandidas removidas):
     *    "you like coffee do not you" — produzido pelo TextNormalizer
     */
    fun isTagQuestion(text: String): Boolean {
        // Padrão primário: texto original com vírgula e contrações
        val tagWithComma = Regex(
            """.+,\s*(isn't|aren't|doesn't|don't|didn't|won't|wouldn't|can't|couldn't|haven't|hasn't|hadn't|is|are|do|does|did|will|would|can|could)\s+(it|he|she|they|you|we|i)\s*$"""
        )
        if (tagWithComma.containsMatchIn(text)) return true

        // Padrão secundário: texto normalizado (sem vírgula, contrações expandidas)
        // ex: "they will come will not they", "you like coffee do not you"
        val tagNormalized = Regex(
            """.*\s+(will not|is not|are not|do not|does not|did not|would not|could not|cannot|have not|has not|had not|was not|were not|might not|should not|is|are|do|does|did|will|would|can|could)\s+(it|he|she|they|you|we|i)\s*$"""
        )
        return tagNormalized.containsMatchIn(text)
    }

    /**
     * Verifica se o texto corresponde ao padrão de Indirect question.
     *
     * Starters reconhecidos:
     * - "could you tell me"
     * - "do you know"
     * - "can you tell me"
     * - "i wonder"
     * - "would you mind telling me"
     * - "i'd like to know"
     */
    fun isIndirectQuestion(text: String): Boolean {
        val indirectStarters = listOf(
            "could you tell me",
            "do you know",
            "can you tell me",
            "i wonder",
            "would you mind telling me",
            "i'd like to know"
        )
        return indirectStarters.any { text.startsWith(it) }
    }

    /**
     * Gera sugestões de correção para o tipo de pergunta incorreto.
     *
     * Precondição: normalized é a pergunta normalizada do usuário
     * Postcondição: retorna lista com pelo menos uma Correction
     */
    fun suggestTypeCorrection(
        normalized: String,
        expectedType: QuestionType
    ): List<Correction> = listOf(
        Correction(
            type = ErrorType.WRONG_QUESTION_TYPE,
            messageKey = "error_wrong_type",
            suggestion = "Este exercício espera uma ${expectedType.displayName()}."
        )
    )
}
