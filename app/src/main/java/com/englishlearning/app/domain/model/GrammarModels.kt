package com.englishlearning.app.domain.model

/**
 * Modelos de domínio para Mini-lições de Gramática.
 */

/**
 * Lição de gramática.
 */
data class GrammarLesson(
    val id: Long = 0,
    val levelId: Int,
    val grammarTopic: String,
    val title: String,
    val explanation: String,
    val formula: String? = null,
    val examples: List<GrammarExample> = emptyList(),
    val iconEmoji: String = "📚",
    val isViewed: Boolean = false,
    val isCompleted: Boolean = false
)

/**
 * Exemplo interativo de gramática.
 */
data class GrammarExample(
    val id: Long = 0,
    val sentence: String,
    val translation: String,
    val highlight: String? = null,
    val note: String? = null,
    val isCorrect: Boolean = true
)

/**
 * Categoria de tópicos gramaticais.
 */
enum class GrammarCategory(val label: String, val emoji: String, val topics: List<String>) {
    TENSES(
        "Tempos Verbais", "⏰",
        listOf(
            "present simple", "present continuous", "past simple", "past continuous",
            "present perfect", "past perfect", "future simple", "future continuous",
            "present perfect continuous", "past perfect continuous", "future perfect"
        )
    ),
    MODALS(
        "Verbos Modais", "🎯",
        listOf(
            "can", "could", "may", "might", "must", "shall", "should",
            "will", "would", "ought to", "need", "dare"
        )
    ),
    CONDITIONALS(
        "Condicionais", "🔀",
        listOf(
            "zero conditional", "first conditional", "second conditional",
            "third conditional", "mixed conditionals"
        )
    ),
    PASSIVE(
        "Voz Passiva", "🔄",
        listOf(
            "passive present", "passive past", "passive future",
            "passive perfect", "passive modals"
        )
    ),
    REPORTED_SPEECH(
        "Discurso Indireto", "💬",
        listOf(
            "reported statements", "reported questions", "reported commands",
            "reporting verbs"
        )
    ),
    ARTICLES(
        "Artigos", "📄",
        listOf(
            "definite article", "indefinite article", "zero article"
        )
    ),
    PREPOSITIONS(
        "Preposições", "📍",
        listOf(
            "prepositions of time", "prepositions of place", "prepositions of movement",
            "prepositional phrases"
        )
    ),
    PRONOUNS(
        "Pronomes", "👤",
        listOf(
            "personal pronouns", "possessive pronouns", "reflexive pronouns",
            "relative pronouns", "demonstrative pronouns"
        )
    ),
    CLAUSES(
        "Orações", "🔗",
        listOf(
            "relative clauses", "noun clauses", "adverb clauses",
            "participle clauses"
        )
    ),
    COMPARATIVES(
        "Comparativos", "⚖️",
        listOf(
            "comparative adjectives", "superlative adjectives",
            "as...as", "comparisons"
        )
    )
}

/**
 * Progresso em uma lição de gramática.
 */
data class GrammarProgress(
    val lessonId: Long,
    val isViewed: Boolean = false,
    val isCompleted: Boolean = false,
    val viewedAt: Long? = null,
    val completedAt: Long? = null,
    val examplesViewed: Int = 0,
    val quizScore: Int? = null
)

/**
 * Quiz de gramática após a lição.
 */
data class GrammarQuiz(
    val lessonId: Long,
    val questions: List<GrammarQuizQuestion>,
    val currentIndex: Int = 0,
    val answers: List<Boolean> = emptyList()
) {
    val currentQuestion: GrammarQuizQuestion? get() = questions.getOrNull(currentIndex)
    val isComplete: Boolean get() = currentIndex >= questions.size
    val score: Int get() = answers.count { it } * 100 / answers.size.coerceAtLeast(1)
}

/**
 * Questão de quiz de gramática.
 */
data class GrammarQuizQuestion(
    val id: Long = 0,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String? = null
)

/**
 * Estatísticas de gramática do usuário.
 */
data class GrammarStatistics(
    val totalLessons: Int = 0,
    val viewedLessons: Int = 0,
    val completedLessons: Int = 0,
    val averageQuizScore: Float = 0f,
    val strongTopics: List<String> = emptyList(),
    val weakTopics: List<String> = emptyList(),
    val progressByCategory: Map<GrammarCategory, Float> = emptyMap()
)

/**
 * Sugestão de lição de gramática baseada em erros.
 */
data class GrammarSuggestion(
    val lesson: GrammarLesson,
    val reason: SuggestionReason,
    val errorCount: Int = 0,
    val relatedDrillIds: List<Int> = emptyList()
)

enum class SuggestionReason(val label: String) {
    FREQUENT_ERRORS("Erros frequentes neste tópico"),
    NEW_TOPIC("Novo tópico para aprender"),
    REVIEW_NEEDED("Revisão recomendada"),
    PREREQUISITE("Pré-requisito para próximo nível")
}

/**
 * Conteúdo formatado para exibição da lição.
 */
data class FormattedLessonContent(
    val title: String,
    val sections: List<LessonSection>
)

sealed class LessonSection {
    data class Text(val content: String) : LessonSection()
    data class Formula(val formula: String, val description: String? = null) : LessonSection()
    data class Example(val example: GrammarExample) : LessonSection()
    data class Comparison(val correct: String, val incorrect: String, val note: String) : LessonSection()
    data class Tips(val tips: List<String>) : LessonSection()
    data class InteractiveExample(val sentence: String, val blanks: List<BlankOption>) : LessonSection()
}

data class BlankOption(
    val position: Int,
    val correctAnswer: String,
    val options: List<String>
)
