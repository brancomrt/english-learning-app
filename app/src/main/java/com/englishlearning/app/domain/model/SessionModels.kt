package com.englishlearning.app.domain.model

/**
 * Configuração inicial de uma sessão de aprendizado.
 */
data class SessionConfig(
    val levelId: Int,
    val categoryId: Int,
    val maxExercises: Int = 10,
    val userId: Long = 1L
)

/**
 * Estado de uma sessão de aprendizado em andamento.
 * Requisito 13.2: LearningViewModel mantém estado como StateFlow.
 * Requisito 13.5: estado preservado ao navegar para outra tela.
 */
data class Session(
    val id: Long = 0,
    val config: SessionConfig,
    val exercises: List<Exercise>,
    val currentIndex: Int = 0,
    val sessionScore: Int = 0,
    val attemptCount: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val isPaused: Boolean = false
) {
    val currentExercise: Exercise? get() = exercises.getOrNull(currentIndex)
    val remainingCount: Int get() = exercises.size - currentIndex - 1
    val isComplete: Boolean get() = currentIndex >= exercises.size
}

/**
 * Resultado de uma tentativa individual em um exercício.
 */
data class AttemptResult(
    val exerciseId: Long,
    val userId: Long,
    val userAnswer: String,
    val validationResult: ValidationResult,
    val xpEarned: Int,
    val attemptedAt: Long = System.currentTimeMillis()
)

/**
 * Resumo ao final de uma sessão de aprendizado.
 * Requisito 13.4: inclui total de exercícios, acertos, XP total e tempo decorrido.
 * Requisito 13.6: exibir na SessionSummaryScreen.
 */
data class SessionSummary(
    val totalExercises: Int,
    val correctAnswers: Int,
    val totalXpEarned: Int,
    val durationMillis: Long,
    val levelId: Int,
    val categoryId: Int,
    val achievementsUnlocked: List<Achievement> = emptyList()
) {
    val accuracyRate: Float
        get() = if (totalExercises > 0) correctAnswers.toFloat() / totalExercises else 0f
}
