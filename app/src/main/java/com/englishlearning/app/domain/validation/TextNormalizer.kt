package com.englishlearning.app.domain.validation

/**
 * Normaliza texto para comparação na validação de perguntas/respostas.
 *
 * Expande contrações do inglês antes de remover pontuação, garantindo que
 * "what's" e "what is", "don't" e "do not" sejam tratados como equivalentes.
 *
 * Requisitos: 10.1–10.4
 */
class TextNormalizer {

    private val contractions: Map<String, String> = mapOf(
        // Negativas
        "aren't" to "are not",
        "can't" to "cannot",
        "couldn't" to "could not",
        "didn't" to "did not",
        "doesn't" to "does not",
        "don't" to "do not",
        "hadn't" to "had not",
        "hasn't" to "has not",
        "haven't" to "have not",
        "isn't" to "is not",
        "mightn't" to "might not",
        "mustn't" to "must not",
        "needn't" to "need not",
        "shouldn't" to "should not",
        "wasn't" to "was not",
        "weren't" to "were not",
        "won't" to "will not",
        "wouldn't" to "would not",
        "shan't" to "shall not",
        // Pronome + auxiliar
        "i'm" to "i am",
        "i've" to "i have",
        "i'd" to "i would",
        "i'll" to "i will",
        "you're" to "you are",
        "you've" to "you have",
        "you'd" to "you would",
        "you'll" to "you will",
        "he's" to "he is",
        "he'd" to "he would",
        "he'll" to "he will",
        "she's" to "she is",
        "she'd" to "she would",
        "she'll" to "she will",
        "it's" to "it is",
        "it'd" to "it would",
        "it'll" to "it will",
        "we're" to "we are",
        "we've" to "we have",
        "we'd" to "we would",
        "we'll" to "we will",
        "they're" to "they are",
        "they've" to "they have",
        "they'd" to "they would",
        "they'll" to "they will",
        "there's" to "there is",
        "there're" to "there are",
        // Wh-word + auxiliar
        "what's" to "what is",
        "what're" to "what are",
        "what've" to "what have",
        "what'll" to "what will",
        "where's" to "where is",
        "where're" to "where are",
        "when's" to "when is",
        "who's" to "who is",
        "who'd" to "who would",
        "who'll" to "who will",
        "who've" to "who have",
        "why's" to "why is",
        "why'd" to "why did",
        "how's" to "how is",
        "how'd" to "how did",
        "how'll" to "how will",
        // Outros comuns
        "that's" to "that is",
        "that'd" to "that would",
        "here's" to "here is",
        "let's" to "let us",
        "o'clock" to "oclock"
    )

    /**
     * Normaliza o texto: expande contrações, converte para minúsculas,
     * remove pontuação e compacta espaços.
     */
    fun normalize(text: String): String {
        return text
            .trim()
            .lowercase()
            .expandContractions()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.expandContractions(): String {
        var result = this
        // Ordena por comprimento decrescente para evitar substituições parciais
        contractions.entries
            .sortedByDescending { it.key.length }
            .forEach { (contraction, expansion) ->
                result = result.replace(
                    Regex("\\b${Regex.escape(contraction)}\\b"),
                    expansion
                )
            }
        return result
    }

    /**
     * Extrai palavras-chave do texto, removendo stop words e palavras curtas.
     */
    fun extractKeywords(text: String): Set<String> {
        val stopWords = setOf(
            "a", "an", "the", "is", "are", "was", "were",
            "do", "does", "did", "will", "would", "can", "could"
        )
        return normalize(text)
            .split(" ")
            .filter { it.length > 2 && it !in stopWords }
            .toSet()
    }
}
