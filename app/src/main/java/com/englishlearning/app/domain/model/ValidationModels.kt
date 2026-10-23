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
 * Cada tipo de erro inclui informações para feedback educativo.
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
    TENSE_MISMATCH;

    /**
     * Nome legível do tipo de erro em português brasileiro.
     */
    fun displayName(): String = when (this) {
        WRONG_QUESTION_TYPE  -> "Tipo de pergunta incorreto"
        MISSING_AUX_VERB     -> "Verbo auxiliar ausente"
        WRONG_AUX_VERB       -> "Verbo auxiliar incorreto"
        WRONG_WH_WORD        -> "Palavra interrogativa incorreta"
        INVERTED_WORD_ORDER  -> "Ordem das palavras invertida"
        MISSING_QUESTION_MARK -> "Ponto de interrogação ausente"
        SEMANTIC_MISMATCH    -> "Sentido diferente do esperado"
        EXTRA_WORDS          -> "Palavras extras desnecessárias"
        TENSE_MISMATCH       -> "Tempo verbal incorreto"
    }

    /**
     * Explicação detalhada do erro com dicas para correção.
     * Usado para feedback educativo mais rico.
     */
    fun detailedExplanation(): String = when (this) {
        WRONG_QUESTION_TYPE -> """
            |Você usou um tipo de pergunta diferente do esperado.
            |
            |📚 Tipos de perguntas em inglês:
            |• Yes/No questions: começam com verbo auxiliar (Do, Does, Did, Is, Are, etc.)
            |• Wh- questions: começam com What, Where, When, Why, Who, How, etc.
            |
            |💡 Dica: Verifique se a resposta pede "sim/não" ou uma informação específica.
        """.trimMargin()

        MISSING_AUX_VERB -> """
            |Faltou o verbo auxiliar na sua pergunta.
            |
            |📚 Regra: Em inglês, perguntas precisam de um verbo auxiliar antes do sujeito.
            |
            |Auxiliares comuns:
            |• Do/Does/Did - para verbos no presente e passado simples
            |• Is/Are/Was/Were - para o verbo "to be" e tempos contínuos
            |• Have/Has/Had - para tempos perfeitos
            |• Will/Would/Can/Could - para modais
            |
            |💡 Dica: A estrutura é: Auxiliar + Sujeito + Verbo principal
        """.trimMargin()

        WRONG_AUX_VERB -> """
            |Você usou um verbo auxiliar que não combina com o tempo verbal ou sujeito.
            |
            |📚 Regras de concordância:
            |• Does → he/she/it (presente)
            |• Do → I/you/we/they (presente)
            |• Did → todos os sujeitos (passado)
            |• Is → he/she/it (contínuo presente)
            |• Are → you/we/they (contínuo presente)
            |
            |💡 Dica: Verifique se o auxiliar concorda com o sujeito e o tempo verbal da resposta.
        """.trimMargin()

        WRONG_WH_WORD -> """
            |A palavra interrogativa (Wh- word) não corresponde à informação pedida.
            |
            |📚 Uso correto:
            |• What → coisas, objetos, ações ("O que/Qual")
            |• Where → lugar ("Onde")
            |• When → tempo ("Quando")
            |• Why → razão ("Por que")
            |• Who → pessoa como sujeito ("Quem")
            |• Whom → pessoa como objeto ("Quem" - formal)
            |• Which → escolha entre opções ("Qual")
            |• How → modo, maneira ("Como")
            |• How + adj → quantidade/intensidade (How old, How much, etc.)
            |
            |💡 Dica: Identifique que tipo de informação está sendo pedida na resposta.
        """.trimMargin()

        INVERTED_WORD_ORDER -> """
            |A ordem das palavras na sua pergunta está incorreta.
            |
            |📚 Ordem correta em perguntas:
            |• Yes/No: Auxiliar + Sujeito + Verbo + Complemento?
            |• Wh-: Wh-word + Auxiliar + Sujeito + Verbo + Complemento?
            |
            |Exemplos:
            |• ✅ "Do you like pizza?"
            |• ❌ "You do like pizza?"
            |• ✅ "Where does she live?"
            |• ❌ "Where she does live?"
            |
            |💡 Dica: Em perguntas, o auxiliar vem ANTES do sujeito (inversão).
        """.trimMargin()

        MISSING_QUESTION_MARK -> """
            |Sua pergunta está sem o ponto de interrogação.
            |
            |📚 Regra: Toda pergunta em inglês deve terminar com "?"
            |
            |💡 Dica: Não esqueça de adicionar "?" no final da sua pergunta!
        """.trimMargin()

        SEMANTIC_MISMATCH -> """
            |O sentido da sua pergunta não corresponde à resposta esperada.
            |
            |📚 Dica: Releia a resposta fornecida e pense:
            |• Que informação essa resposta está dando?
            |• Qual pergunta levaria naturalmente a essa resposta?
            |
            |Exemplo:
            |• Resposta: "I am 25 years old."
            |• Pergunta esperada: "How old are you?" (não "What is your age doing?")
            |
            |💡 Dica: A pergunta deve fazer sentido como algo que alguém realmente perguntaria.
        """.trimMargin()

        EXTRA_WORDS -> """
            |Sua pergunta contém palavras desnecessárias ou redundantes.
            |
            |📚 Em inglês, perguntas devem ser diretas e concisas:
            |• ✅ "What is your name?"
            |• ❌ "What is that your name is?"
            |
            |💡 Dica: Revise se todas as palavras são realmente necessárias. 
            |Remova repetições e palavras que não acrescentam significado.
        """.trimMargin()

        TENSE_MISMATCH -> """
            |O tempo verbal da sua pergunta não corresponde ao da resposta.
            |
            |📚 O tempo verbal da pergunta deve coincidir com a resposta:
            |
            |• Resposta no presente → pergunta no presente
            |  "I work here." → "Where do you work?"
            |
            |• Resposta no passado → pergunta no passado
            |  "I worked there." → "Where did you work?"
            |
            |• Resposta no futuro → pergunta no futuro
            |  "I will go tomorrow." → "When will you go?"
            |
            |💡 Dica: Identifique o tempo verbal na resposta e use o mesmo na pergunta.
        """.trimMargin()
    }

    /**
     * Exemplo prático do erro com correção.
     */
    fun exampleWithCorrection(): Pair<String, String> = when (this) {
        WRONG_QUESTION_TYPE -> Pair(
            "You live in Brazil?",
            "Do you live in Brazil?"
        )
        MISSING_AUX_VERB -> Pair(
            "Where you live?",
            "Where do you live?"
        )
        WRONG_AUX_VERB -> Pair(
            "Do she likes coffee?",
            "Does she like coffee?"
        )
        WRONG_WH_WORD -> Pair(
            "When is your name?",
            "What is your name?"
        )
        INVERTED_WORD_ORDER -> Pair(
            "Where she does work?",
            "Where does she work?"
        )
        MISSING_QUESTION_MARK -> Pair(
            "What is your name",
            "What is your name?"
        )
        SEMANTIC_MISMATCH -> Pair(
            "How are your name?",
            "What is your name?"
        )
        EXTRA_WORDS -> Pair(
            "What is it your name?",
            "What is your name?"
        )
        TENSE_MISMATCH -> Pair(
            "Where do you work? (para resposta no passado)",
            "Where did you work?"
        )
    }
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
