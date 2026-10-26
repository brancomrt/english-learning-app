package com.englishlearning.app.domain.model

/**
 * Modelos de domínio para Avaliação de Pronúncia.
 */

/**
 * Resultado de avaliação de pronúncia.
 */
data class PronunciationResult(
    val id: Long = 0,
    val exerciseId: Long,
    val exerciseType: PronunciationExerciseType,
    val expectedText: String,
    val recognizedText: String,
    val overallScore: Int, // 0-100
    val breakdown: PronunciationBreakdown,
    val wordAnalysis: List<WordPronunciationAnalysis>,
    val audioDurationMs: Long? = null,
    val confidence: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Tipo de exercício de pronúncia.
 */
enum class PronunciationExerciseType(val label: String) {
    DRILL("Drill Q&A"),
    DICTATION("Ditado"),
    SHADOWING("Shadowing"),
    FREE_SPEECH("Fala Livre")
}

/**
 * Detalhamento da pontuação de pronúncia.
 */
data class PronunciationBreakdown(
    val accuracyScore: Int,   // Palavras pronunciadas corretamente
    val fluencyScore: Int,    // Ritmo e fluidez
    val completenessScore: Int // Percentual de texto coberto
) {
    val averageScore: Int get() = (accuracyScore + fluencyScore + completenessScore) / 3
}

/**
 * Análise de pronúncia por palavra.
 */
data class WordPronunciationAnalysis(
    val word: String,
    val status: WordStatus,
    val confidence: Float? = null,
    val suggestedPronunciation: String? = null
)

/**
 * Status da palavra na pronúncia.
 */
enum class WordStatus(val label: String, val emoji: String, val color: Long) {
    CORRECT("Correto", "✓", 0xFF4CAF50),       // Green
    MISPRONOUNCED("Erro", "✗", 0xFFF44336),    // Red
    OMITTED("Omitido", "○", 0xFFFF9800),       // Orange
    INSERTED("Inserido", "+", 0xFF2196F3)      // Blue
}

/**
 * Feedback detalhado de pronúncia.
 */
data class PronunciationFeedback(
    val overallMessage: String,
    val tips: List<PronunciationTip>,
    val strongPoints: List<String>,
    val improvementAreas: List<String>,
    val suggestedExercises: List<Long> = emptyList()
)

/**
 * Dica de melhoria de pronúncia.
 */
data class PronunciationTip(
    val word: String,
    val tip: String,
    val ipaExpected: String? = null,
    val audioUrl: String? = null
)

/**
 * Configurações de avaliação de pronúncia.
 */
data class PronunciationSettings(
    val strictMode: Boolean = false,          // Mais rigoroso com erros
    val allowContractions: Boolean = true,    // "I'm" = "I am"
    val ignorePunctuation: Boolean = true,    // Ignorar pontuação
    val partialCredit: Boolean = true,        // Crédito parcial para palavras similares
    val minimumConfidence: Float = 0.5f       // Confiança mínima do reconhecimento
)

/**
 * Estatísticas de pronúncia do usuário.
 */
data class PronunciationStatistics(
    val totalAttempts: Int = 0,
    val averageScore: Float = 0f,
    val bestScore: Int = 0,
    val scoreDistribution: ScoreDistribution = ScoreDistribution(),
    val commonErrors: List<CommonPronunciationError> = emptyList(),
    val improvementTrend: ImprovementTrend = ImprovementTrend.STABLE,
    val lastAttemptAt: Long? = null
)

/**
 * Distribuição de pontuações.
 */
data class ScoreDistribution(
    val excellent: Int = 0,  // 90-100
    val good: Int = 0,       // 80-89
    val fair: Int = 0,       // 60-79
    val needsPractice: Int = 0 // < 60
) {
    val total: Int get() = excellent + good + fair + needsPractice
}

/**
 * Erro comum de pronúncia.
 */
data class CommonPronunciationError(
    val word: String,
    val errorCount: Int,
    val lastErrorAt: Long,
    val suggestedPractice: String? = null
)

/**
 * Tendência de melhoria.
 */
enum class ImprovementTrend(val label: String, val emoji: String) {
    IMPROVING("Melhorando", "📈"),
    STABLE("Estável", "➡️"),
    DECLINING("Em queda", "📉"),
    NEW("Novo", "🆕")
}

/**
 * Sessão de prática de pronúncia.
 */
data class PronunciationSession(
    val exercises: List<PronunciationExercise>,
    val currentIndex: Int = 0,
    val results: List<PronunciationResult> = emptyList(),
    val startTime: Long = System.currentTimeMillis(),
    val settings: PronunciationSettings = PronunciationSettings()
) {
    val currentExercise: PronunciationExercise? get() = exercises.getOrNull(currentIndex)
    val isComplete: Boolean get() = currentIndex >= exercises.size
    val averageScore: Float get() = if (results.isEmpty()) 0f else results.map { it.overallScore }.average().toFloat()
}

/**
 * Exercício de pronúncia genérico.
 */
data class PronunciationExercise(
    val id: Long,
    val text: String,
    val translation: String? = null,
    val audioUrl: String? = null,
    val type: PronunciationExerciseType,
    val difficulty: Int = 1
)

/**
 * Comparação visual entre esperado e reconhecido.
 */
data class PronunciationComparison(
    val segments: List<ComparisonSegment>
)

data class ComparisonSegment(
    val text: String,
    val status: WordStatus,
    val expected: String? = null
)
