package com.englishlearning.app.domain.model

/**
 * Modelos de domínio para o sistema de Flashcards de vocabulário.
 */

/**
 * Flashcard de vocabulário com SRS.
 */
data class Flashcard(
    val id: Long = 0,
    val word: String,
    val definition: String,
    val exampleSentence: String,
    val context: String? = null,
    val sourceDrillId: Int? = null,
    val levelId: Int,
    val partOfSpeech: PartOfSpeech? = null,
    val pronunciation: String? = null,
    val audioUrl: String? = null,
    val imageUrl: String? = null,
    val srsState: SrsState = SrsState.NEW,
    val dueDate: String,
    val stats: FlashcardStats = FlashcardStats()
)

/**
 * Categoria gramatical da palavra.
 */
enum class PartOfSpeech(val label: String, val emoji: String) {
    NOUN("Noun", "📦"),
    VERB("Verb", "🏃"),
    ADJECTIVE("Adjective", "🌈"),
    ADVERB("Adverb", "⚡"),
    PRONOUN("Pronoun", "👤"),
    PREPOSITION("Preposition", "📍"),
    CONJUNCTION("Conjunction", "🔗"),
    INTERJECTION("Interjection", "❗"),
    ARTICLE("Article", "📄"),
    DETERMINER("Determiner", "🎯"),
    PHRASAL_VERB("Phrasal Verb", "🔄"),
    IDIOM("Idiom", "💬")
}

/**
 * Estado do SRS do flashcard.
 */
enum class SrsState(val label: String) {
    NEW("Novo"),
    LEARNING("Aprendendo"),
    REVIEW("Revisão"),
    RELEARNING("Reaprendendo")
}

/**
 * Estatísticas do flashcard.
 */
data class FlashcardStats(
    val stability: Double = 0.0,
    val difficulty: Double = 5.0,
    val elapsedDays: Int = 0,
    val scheduledDays: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val lastReviewDate: String? = null
)

/**
 * Resposta de revisão do flashcard.
 */
enum class FlashcardRating(val value: Int, val label: String, val emoji: String) {
    AGAIN(1, "De novo", "🔄"),
    HARD(2, "Difícil", "😓"),
    GOOD(3, "Bom", "👍"),
    EASY(4, "Fácil", "⭐")
}

/**
 * Sessão de revisão de flashcards.
 */
data class FlashcardSession(
    val cards: List<Flashcard>,
    val currentIndex: Int = 0,
    val reviewedCount: Int = 0,
    val correctCount: Int = 0,
    val startTime: Long = System.currentTimeMillis(),
    val isShowingAnswer: Boolean = false
) {
    val currentCard: Flashcard? get() = cards.getOrNull(currentIndex)
    val progress: Float get() = if (cards.isEmpty()) 0f else currentIndex.toFloat() / cards.size
    val isComplete: Boolean get() = currentIndex >= cards.size
    val accuracy: Float get() = if (reviewedCount == 0) 0f else correctCount.toFloat() / reviewedCount
}

/**
 * Estatísticas globais de flashcards do usuário.
 */
data class FlashcardStatistics(
    val totalCards: Int = 0,
    val newCards: Int = 0,
    val learningCards: Int = 0,
    val reviewCards: Int = 0,
    val masteredCards: Int = 0,
    val dueToday: Int = 0,
    val averageDifficulty: Double = 0.0,
    val totalReviews: Int = 0,
    val streakDays: Int = 0
)

/**
 * Resultado da extração automática de vocabulário.
 */
data class VocabularyExtraction(
    val word: String,
    val frequency: Int,
    val drillIds: List<Int>,
    val suggestedDefinition: String? = null,
    val suggestedPartOfSpeech: PartOfSpeech? = null,
    val exampleSentences: List<String>
)
