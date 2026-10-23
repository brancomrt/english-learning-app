package com.englishlearning.app.domain.usecase

import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.ProgressiveHint
import com.englishlearning.app.domain.model.ProgressiveHintType
import javax.inject.Inject

/**
 * Use case para gerar dicas progressivas de Active Recall.
 * 
 * Gera dicas em ordem crescente de informação:
 * 1. Primeira letra
 * 2. Número de palavras
 * 3. Estrutura com blanks
 * 4. Dica gramatical
 * 5. Resposta completa (último recurso)
 */
class GenerateProgressiveHintsUseCase @Inject constructor() {
    
    /**
     * Gera lista de dicas progressivas para um exercício.
     * 
     * @param exercise O exercício para o qual gerar dicas
     * @return Lista ordenada de dicas progressivas
     */
    operator fun invoke(exercise: Exercise): List<ProgressiveHint> {
        val primaryAnswer = exercise.acceptedAnswers
            .firstOrNull { it.isPrimary }?.questionText
            ?: exercise.acceptedAnswers.firstOrNull()?.questionText
            ?: return emptyList()
        
        val hints = mutableListOf<ProgressiveHint>()
        var order = 1
        
        // 1. Primeira letra
        val firstLetter = primaryAnswer.firstOrNull { it.isLetter() }
        if (firstLetter != null) {
            hints.add(
                ProgressiveHint(
                    type = ProgressiveHintType.FIRST_LETTER,
                    text = "Começa com \"${firstLetter.uppercaseChar()}...\"",
                    order = order++
                )
            )
        }
        
        // 2. Número de palavras
        val wordCount = primaryAnswer.split(" ").filter { it.isNotBlank() }.size
        hints.add(
            ProgressiveHint(
                type = ProgressiveHintType.WORD_COUNT,
                text = "$wordCount palavra${if (wordCount > 1) "s" else ""}",
                order = order++
            )
        )
        
        // 3. Estrutura com blanks
        val structure = generateStructure(primaryAnswer)
        hints.add(
            ProgressiveHint(
                type = ProgressiveHintType.STRUCTURE,
                text = structure,
                order = order++
            )
        )
        
        // 4. Dica gramatical baseada no grammarFocus
        val grammarTip = generateGrammarTip(exercise.grammarFocus, exercise.questionType)
        if (grammarTip != null) {
            hints.add(
                ProgressiveHint(
                    type = ProgressiveHintType.GRAMMAR_TIP,
                    text = grammarTip,
                    order = order++
                )
            )
        }
        
        // 5. Resposta completa (último recurso)
        hints.add(
            ProgressiveHint(
                type = ProgressiveHintType.FULL_ANSWER,
                text = primaryAnswer,
                order = order
            )
        )
        
        return hints
    }
    
    /**
     * Gera estrutura com blanks mostrando apenas palavras curtas.
     * Ex: "It's a pen" → "It's _ ___"
     */
    private fun generateStructure(answer: String): String {
        val words = answer.split(" ")
        return words.joinToString(" ") { word ->
            // Mostra palavras curtas (artigos, pronomes, etc)
            val cleanWord = word.filter { it.isLetterOrDigit() || it == '\'' }
            when {
                cleanWord.length <= 2 -> word
                word.any { it == '\'' } && cleanWord.length <= 4 -> word // contrações: it's, don't
                isCommonWord(cleanWord.lowercase()) -> word
                else -> "_".repeat(word.length.coerceAtMost(5))
            }
        }
    }
    
    /**
     * Verifica se é uma palavra comum que pode ser mostrada.
     */
    private fun isCommonWord(word: String): Boolean {
        val commonWords = setOf(
            "a", "an", "the", "is", "are", "am", "was", "were",
            "do", "does", "did", "have", "has", "had",
            "i", "you", "he", "she", "it", "we", "they",
            "my", "your", "his", "her", "its", "our", "their",
            "this", "that", "these", "those",
            "in", "on", "at", "to", "for", "of", "with", "by",
            "and", "or", "but", "not", "no", "yes",
            "what", "where", "when", "who", "why", "how", "which"
        )
        return word in commonWords
    }
    
    /**
     * Gera dica gramatical baseada no foco do exercício.
     */
    private fun generateGrammarTip(grammarFocus: String, questionType: com.englishlearning.app.domain.model.QuestionType): String? {
        val focus = grammarFocus.lowercase()
        
        return when {
            // Tempos verbais
            "present simple" in focus || "simple present" in focus ->
                "Use presente simples: sujeito + verbo (he/she/it + verbo+s)"
            
            "present continuous" in focus || "present progressive" in focus ->
                "Use presente contínuo: am/is/are + verbo-ing"
            
            "past simple" in focus || "simple past" in focus ->
                "Use passado simples: verbo no passado (regular: +ed)"
            
            "past continuous" in focus ->
                "Use passado contínuo: was/were + verbo-ing"
            
            "present perfect" in focus ->
                "Use presente perfeito: have/has + particípio passado"
            
            "future" in focus || "will" in focus || "going to" in focus ->
                "Use futuro: will + verbo ou going to + verbo"
            
            // Estruturas de pergunta
            "yes/no" in focus || "yes no" in focus ->
                "Pergunta Yes/No: auxiliar + sujeito + verbo?"
            
            "wh-question" in focus || "wh question" in focus ->
                "Pergunta Wh-: What/Where/When/etc + auxiliar + sujeito + verbo?"
            
            "tag question" in focus ->
                "Tag question: afirmativa → tag negativa, negativa → tag positiva"
            
            // Outros
            "negative" in focus ->
                "Forma negativa: use don't/doesn't/didn't/isn't/aren't/etc"
            
            "possessive" in focus ->
                "Possessivos: my/your/his/her/its/our/their ou 's para posse"
            
            "comparative" in focus ->
                "Comparativo: adj curto + -er ou more + adj longo"
            
            "superlative" in focus ->
                "Superlativo: the + adj curto + -est ou the most + adj longo"
            
            "modal" in focus ->
                "Verbos modais: can/could/may/might/must/should + verbo base"
            
            else -> when (questionType) {
                com.englishlearning.app.domain.model.QuestionType.WH_WHAT -> "Use 'What' para perguntar sobre coisas/informações"
                com.englishlearning.app.domain.model.QuestionType.WH_WHERE -> "Use 'Where' para perguntar sobre lugares"
                com.englishlearning.app.domain.model.QuestionType.WH_WHEN -> "Use 'When' para perguntar sobre tempo"
                com.englishlearning.app.domain.model.QuestionType.WH_WHO -> "Use 'Who' para perguntar sobre pessoas"
                com.englishlearning.app.domain.model.QuestionType.WH_WHY -> "Use 'Why' para perguntar motivos"
                com.englishlearning.app.domain.model.QuestionType.WH_HOW -> "Use 'How' para perguntar maneira/modo"
                com.englishlearning.app.domain.model.QuestionType.YES_NO -> "Inicie com auxiliar: Do/Does/Did/Is/Are/etc"
                com.englishlearning.app.domain.model.QuestionType.TAG -> "Tag: use o auxiliar oposto ao da frase principal"
                com.englishlearning.app.domain.model.QuestionType.CHOICE -> "Pergunta de escolha: A or B?"
                else -> null
            }
        }
    }
}
