package com.englishlearning.app.domain.model

/**
 * Modelos de domínio para Ditado (Spaced Writing).
 */

/**
 * Exercício de ditado.
 */
data class DictationExercise(
    val id: Long = 0,
    val sentence: String,
    val translation: String? = null,
    val levelId: Int,
    val difficulty: DictationDifficulty = DictationDifficulty.MEDIUM,
    val grammarTopic: String? = null,
    val keywords: List<String> = emptyList(),
    val sourceDrillId: Int? = null
)

/**
 * Nível de dificuldade do ditado.
 */
enum class DictationDifficulty(val value: Int, val label: String, val emoji: String) {
    EASY(1, "Fácil", "🟢"),
    MEDIUM(2, "Médio", "🟡"),
    HARD(3, "Difícil", "🔴"),
    EXPERT(4, "Expert", "💀")
}

/**
 * Resultado de uma tentativa de ditado.
 */
data class DictationResult(
    val exerciseId: Long,
    val expectedText: String,
    val userText: String,
    val score: Int, // 0-100
    val accuracy: Float, // Percentual de palavras corretas
    val mistakes: List<DictationMistake>,
    val timeTakenMs: Long,
    val hintsUsed: Int = 0
)

/**
 * Tipo de erro no ditado.
 */
data class DictationMistake(
    val position: Int,
    val expected: String,
    val actual: String?,
    val type: MistakeType
)

enum class MistakeType(val label: String, val emoji: String) {
    SPELLING("Ortografia", "✏️"),
    MISSING_WORD("Palavra faltando", "❓"),
    EXTRA_WORD("Palavra extra", "➕"),
    WRONG_WORD("Palavra errada", "❌"),
    PUNCTUATION("Pontuação", "🔣"),
    CAPITALIZATION("Maiúsculas/minúsculas", "🔤")
}

/**
 * Progresso do usuário em um exercício de ditado.
 */
data class DictationProgress(
    val exerciseId: Long,
    val attempts: Int = 0,
    val correctCount: Int = 0,
    val bestScore: Int = 0,
    val lastScore: Int = 0,
    val srsState: SrsState = SrsState.NEW,
    val dueDate: String,
    val lastReviewDate: String? = null
)

/**
 * Sessão de ditado.
 */
data class DictationSession(
    val exercises: List<DictationExercise>,
    val currentIndex: Int = 0,
    val results: List<DictationResult> = emptyList(),
    val startTime: Long = System.currentTimeMillis(),
    val mode: DictationMode = DictationMode.STANDARD
) {
    val currentExercise: DictationExercise? get() = exercises.getOrNull(currentIndex)
    val progress: Float get() = if (exercises.isEmpty()) 0f else currentIndex.toFloat() / exercises.size
    val isComplete: Boolean get() = currentIndex >= exercises.size
    val averageScore: Float get() = if (results.isEmpty()) 0f else results.map { it.score }.average().toFloat()
}

/**
 * Modo de ditado.
 */
enum class DictationMode(val label: String, val description: String) {
    STANDARD("Padrão", "Ouvir e escrever a frase completa"),
    FILL_BLANKS("Preencher lacunas", "Complete as palavras que faltam"),
    SPEED("Velocidade", "Ditado cronometrado"),
    PROGRESSIVE("Progressivo", "Dificuldade aumenta gradualmente")
}

/**
 * Estatísticas de ditado do usuário.
 */
data class DictationStatistics(
    val totalExercises: Int = 0,
    val completedExercises: Int = 0,
    val averageScore: Float = 0f,
    val bestScore: Int = 0,
    val totalAttempts: Int = 0,
    val masteredCount: Int = 0,
    val dueToday: Int = 0,
    val commonMistakes: Map<MistakeType, Int> = emptyMap()
)

/**
 * Dica para ditado.
 */
sealed class DictationHint {
    data class PlaySlower(val speedFactor: Float = 0.75f) : DictationHint()
    data class ShowWordCount(val count: Int) : DictationHint()
    data class ShowFirstLetter(val word: String, val letter: Char) : DictationHint()
    data class ShowTranslation(val translation: String) : DictationHint()
    data class RepeatWord(val word: String) : DictationHint()
}
