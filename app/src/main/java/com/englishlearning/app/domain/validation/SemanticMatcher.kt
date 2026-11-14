package com.englishlearning.app.domain.validation

/**
 * Calcula similaridade semântica entre dois textos normalizados.
 *
 * Combina Jaccard similarity (peso 60%) com bigram similarity (peso 40%)
 * para produzir um score de correspondência entre textos.
 *
 * Requisito 10.5: combinação Jaccard 60% + bigrams 40%
 * Requisito 10.6: retorna 1.0f para strings idênticas
 * Requisito 10.7: é simétrico — score(a, b) == score(b, a)
 */
class SemanticMatcher {

    /**
     * Mapa conservador de sinônimos e equivalências comuns em inglês.
     * Aplicado de forma simétrica em ambos os textos antes do cálculo de
     * Jaccard e bigramas para aceitar variações naturais de vocabulário.
     */
    private val synonyms: Map<String, String> = mapOf(
        "profession" to "job",
        "occupation" to "job",
        "career" to "job",
        "price" to "cost",
        "begin" to "start",
        "commence" to "start",
        "end" to "finish",
        "complete" to "finish",
        "large" to "big",
        "little" to "small",
        "tiny" to "small",
        "film" to "movie",
        "purchase" to "buy",
        "assist" to "help",
        "require" to "need",
        "residence" to "home",
        "talk" to "speak",
        "town" to "city",
        "glad" to "happy",
        "unhappy" to "sad",
        "correct" to "right",
        "wrong" to "incorrect",
        "fast" to "quick",
        "rapid" to "quick",
        "automobile" to "car",
        "vehicle" to "car",
        "kids" to "children",
        "child" to "kid",
        "phone" to "telephone",
        "cellphone" to "phone",
        "mobile" to "phone",
        "photo" to "picture",
        "image" to "picture",
        "reply" to "answer",
        "respond" to "answer",
        "error" to "mistake",
        "bug" to "issue",
        "defect" to "issue",
        "problem" to "issue",
        "method" to "way",
        "approach" to "way",
        "cause" to "reason",
        "timetable" to "schedule",
        "colleague" to "coworker",
        "boss" to "manager",
        "client" to "customer"
    )

    private val optionalFillers: Set<String> = setOf("a", "an", "the", "please")

    /**
     * Calcula score de similaridade entre dois textos normalizados.
     *
     * Precondição: text1 e text2 são strings normalizadas (lowercase, sem pontuação extra)
     * Postcondição: resultado em [0.0, 1.0]
     *   - 1.0 = idênticos
     *   - 0.0 = sem palavras em comum
     */
    fun score(text1: String, text2: String): Float {
        if (text1 == text2) return 1.0f
        if (text1.isEmpty() || text2.isEmpty()) return 0.0f

        val tokens1 = canonicalizeTokens(text1)
        val tokens2 = canonicalizeTokens(text2)
        if (tokens1 == tokens2) return 1.0f

        val words1 = tokens1.toSet()
        val words2 = tokens2.toSet()

        // Jaccard similarity: interseção / união dos conjuntos de palavras
        val intersection = words1.intersect(words2).size.toFloat()
        val union = words1.union(words2).size.toFloat()
        val jaccardScore = intersection / union

        // Bigram similarity: interseção / max(tamanho) dos conjuntos de bigramas
        val bigrams1 = toBigrams(tokens1)
        val bigrams2 = toBigrams(tokens2)
        val bigramIntersection = bigrams1.intersect(bigrams2).size.toFloat()
        val bigramUnion = maxOf(bigrams1.size, bigrams2.size).toFloat()
        val bigramScore = if (bigramUnion > 0) bigramIntersection / bigramUnion else 0f

        // Peso: 60% Jaccard + 40% bigrams
        return (jaccardScore * 0.6f + bigramScore * 0.4f).coerceIn(0f, 1f)
    }

    private fun canonicalizeTokens(text: String): List<String> {
        val mapped = text.split(" ")
            .filter { it.isNotEmpty() }
            .map { token -> synonyms[token] ?: token }
        val withoutFillers = mapped.filter { it !in optionalFillers }
        return if (withoutFillers.isNotEmpty()) withoutFillers else mapped
    }

    /**
     * Gera o conjunto de bigramas (pares de palavras consecutivas) de uma lista de tokens.
     *
     * Ex: ["what", "is", "your", "name"] → {"what is", "is your", "your name"}
     */
    private fun toBigrams(words: List<String>): Set<String> =
        words.zipWithNext { a, b -> "$a $b" }.toSet()
}
