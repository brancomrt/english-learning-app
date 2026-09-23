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

        val words1 = text1.split(" ").toSet()
        val words2 = text2.split(" ").toSet()

        // Jaccard similarity: interseção / união dos conjuntos de palavras
        val intersection = words1.intersect(words2).size.toFloat()
        val union = words1.union(words2).size.toFloat()
        val jaccardScore = intersection / union

        // Bigram similarity: interseção / max(tamanho) dos conjuntos de bigramas
        val bigrams1 = toBigrams(text1)
        val bigrams2 = toBigrams(text2)
        val bigramIntersection = bigrams1.intersect(bigrams2).size.toFloat()
        val bigramUnion = maxOf(bigrams1.size, bigrams2.size).toFloat()
        val bigramScore = if (bigramUnion > 0) bigramIntersection / bigramUnion else 0f

        // Peso: 60% Jaccard + 40% bigrams
        return (jaccardScore * 0.6f + bigramScore * 0.4f).coerceIn(0f, 1f)
    }

    /**
     * Gera o conjunto de bigramas (pares de palavras consecutivas) de um texto.
     *
     * Ex: "what is your name" → {"what is", "is your", "your name"}
     */
    private fun toBigrams(text: String): Set<String> {
        val words = text.split(" ")
        return words.zipWithNext { a, b -> "$a $b" }.toSet()
    }
}
