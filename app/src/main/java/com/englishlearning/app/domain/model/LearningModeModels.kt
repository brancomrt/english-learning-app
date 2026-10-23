package com.englishlearning.app.domain.model

import java.time.LocalDate

/**
 * Modos de aprendizado disponíveis no app.
 * Cada modo oferece uma abordagem diferente para maximizar a retenção.
 */
enum class LearningMode {
    /** Modo padrão: texto visível, digita ou fala a resposta */
    STANDARD,
    
    /** Listening First: ouve primeiro, tenta responder sem ver o texto */
    LISTENING_FIRST,
    
    /** Shadowing/Echo: ouve e repete imediatamente para treinar pronúncia */
    SHADOWING,
    
    /** Active Recall com dicas progressivas (primeira letra, estrutura, etc) */
    ACTIVE_RECALL,
    
    /** Microlearning: sessões curtas de 2-5 minutos */
    MICROLEARNING,
    
    /** Conversation Chains: mini-diálogos conectados */
    CONVERSATION
}

/**
 * Configuração de sessão de aprendizado avançada.
 */
data class AdvancedSessionConfig(
    val mode: LearningMode = LearningMode.STANDARD,
    val exerciseLimit: Int = 10,
    val timeLimitMinutes: Int? = null,
    val enableInterleaving: Boolean = false,
    val showProgressiveHints: Boolean = true
)

// ============================================================================
// Active Recall - Dicas Progressivas
// ============================================================================

/**
 * Tipo de dica progressiva para Active Recall.
 * Reveladas em ordem crescente de informação.
 */
enum class ProgressiveHintType {
    /** Primeira letra da resposta: "I..." */
    FIRST_LETTER,
    
    /** Número de palavras: "3 palavras" */
    WORD_COUNT,
    
    /** Estrutura com blanks: "It's a ___" */
    STRUCTURE,
    
    /** Dica gramatical: "Use presente simples" */
    GRAMMAR_TIP,
    
    /** Resposta completa (última opção) */
    FULL_ANSWER
}

/**
 * Dica progressiva gerada dinamicamente.
 */
data class ProgressiveHint(
    val type: ProgressiveHintType,
    val text: String,
    val order: Int
)

// ============================================================================
// Conversation Chains - Mini-diálogos
// ============================================================================

/**
 * Uma cadeia de exercícios conectados formando um mini-diálogo.
 */
data class ConversationChain(
    val id: Long,
    val title: String,
    val description: String,
    val levelId: Int,
    val categoryId: Int,
    val exercises: List<ChainExercise>,
    val contextStory: String? = null
)

/**
 * Exercício dentro de uma conversation chain.
 * Inclui o contexto do diálogo até este ponto.
 */
data class ChainExercise(
    val exerciseId: Long,
    val orderInChain: Int,
    val speakerRole: String, // "A" ou "B" para indicar quem fala
    val previousContext: List<DialogueLine>,
    val exercise: Exercise
)

/**
 * Linha de diálogo já respondida (contexto).
 */
data class DialogueLine(
    val speakerRole: String,
    val text: String,
    val isUserResponse: Boolean = false
)

// ============================================================================
// Daily Challenges
// ============================================================================

/**
 * Tipo de desafio diário.
 */
enum class ChallengeType {
    /** Completar X exercícios */
    EXERCISE_COUNT,
    
    /** Acertar X em sequência sem errar */
    STREAK_COUNT,
    
    /** Completar exercícios de X categorias diferentes */
    CATEGORY_VARIETY,
    
    /** Completar uma sessão de Shadowing */
    SHADOWING_SESSION,
    
    /** Completar uma sessão de Listening First */
    LISTENING_SESSION,
    
    /** Revisar X cards SRS */
    SRS_REVIEW,
    
    /** Completar uma conversation chain */
    CONVERSATION_CHAIN,
    
    /** Estudar por X minutos */
    TIME_BASED
}

/**
 * Desafio diário disponível para o usuário.
 */
data class DailyChallenge(
    val id: Long = 0,
    val type: ChallengeType,
    val title: String,
    val description: String,
    val targetValue: Int,
    val currentProgress: Int = 0,
    val xpReward: Int,
    val bonusXp: Int = 0, // XP extra por completar antes do fim do dia
    val date: LocalDate,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val iconEmoji: String = "🎯"
) {
    val progressPercent: Float
        get() = if (targetValue > 0) (currentProgress.toFloat() / targetValue).coerceIn(0f, 1f) else 0f
    
    val isActive: Boolean
        get() = !isCompleted && date == LocalDate.now()
}

/**
 * Conjunto de desafios diários gerados para um dia.
 */
data class DailyChallengeSet(
    val date: LocalDate,
    val challenges: List<DailyChallenge>,
    val totalXpAvailable: Int,
    val completedCount: Int = 0
) {
    val allCompleted: Boolean
        get() = completedCount == challenges.size
}

// ============================================================================
// Shadowing / Echo Mode
// ============================================================================

/**
 * Estado de uma sessão de Shadowing.
 */
data class ShadowingState(
    val currentPhrase: String,
    val modelAudioPlayed: Boolean = false,
    val userAttemptText: String? = null,
    val similarityScore: Float? = null,
    val attemptCount: Int = 0,
    val isRecording: Boolean = false
)

/**
 * Resultado de uma tentativa de Shadowing.
 */
data class ShadowingResult(
    val originalText: String,
    val userSpokenText: String,
    val similarityScore: Float, // 0.0 a 1.0
    val feedback: ShadowingFeedback,
    val pronunciationTips: List<String> = emptyList()
)

/**
 * Feedback de uma tentativa de Shadowing.
 */
enum class ShadowingFeedback {
    EXCELLENT,  // >= 90%
    GOOD,       // >= 75%
    FAIR,       // >= 50%
    NEEDS_WORK  // < 50%
}

// ============================================================================
// Listening First Mode
// ============================================================================

/**
 * Estado de um exercício no modo Listening First.
 */
data class ListeningFirstState(
    val audioPlayed: Boolean = false,
    val playCount: Int = 0,
    val textRevealed: Boolean = false,
    val userAttempted: Boolean = false,
    val maxPlaysBeforeReveal: Int = 3
) {
    val canRevealText: Boolean
        get() = playCount >= maxPlaysBeforeReveal || userAttempted
}

// ============================================================================
// Microlearning
// ============================================================================

/**
 * Configuração de sessão de Microlearning (2-5 minutos).
 */
data class MicrolearningConfig(
    val durationMinutes: Int = 3,
    val exerciseCount: Int = 5,
    val focusOnWeakAreas: Boolean = true,
    val includeReview: Boolean = true,
    val mixCategories: Boolean = true // Interleaving
)

/**
 * Progresso de uma sessão de Microlearning.
 */
data class MicrolearningProgress(
    val startTime: Long,
    val targetDurationMs: Long,
    val exercisesCompleted: Int,
    val targetExercises: Int,
    val correctCount: Int,
    val currentStreak: Int
) {
    val elapsedMs: Long
        get() = System.currentTimeMillis() - startTime
    
    val timeRemainingMs: Long
        get() = (targetDurationMs - elapsedMs).coerceAtLeast(0)
    
    val isTimeUp: Boolean
        get() = elapsedMs >= targetDurationMs
    
    val progressPercent: Float
        get() = (exercisesCompleted.toFloat() / targetExercises).coerceIn(0f, 1f)
}

// ============================================================================
// Feedback Detalhado
// ============================================================================

/**
 * Explicação detalhada de um erro.
 */
data class DetailedErrorExplanation(
    val userAnswer: String,
    val correctAnswer: String,
    val errorCategory: DetailedErrorCategory,
    val explanation: String,
    val grammarRule: String?,
    val examples: List<String> = emptyList(),
    val relatedExercises: List<Long> = emptyList()
)

/**
 * Categoria de erro cometido pelo usuário (para feedback detalhado).
 */
enum class DetailedErrorCategory {
    VERB_TENSE,
    SUBJECT_VERB_AGREEMENT,
    WORD_ORDER,
    MISSING_ARTICLE,
    WRONG_PREPOSITION,
    SPELLING,
    CONTRACTION,
    PLURAL_SINGULAR,
    PRONOUN,
    OTHER
}

// ============================================================================
// Gamification Avançada
// ============================================================================

/**
 * Leaderboard entry (mesmo que local/fictício para motivação).
 */
data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val xpTotal: Int,
    val streakDays: Int,
    val isCurrentUser: Boolean = false,
    val avatarEmoji: String = "👤"
)

/**
 * Achievement milestone com progresso.
 */
data class AchievementMilestone(
    val id: Long,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val targetValue: Int,
    val currentValue: Int,
    val xpReward: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
) {
    val progressPercent: Float
        get() = if (targetValue > 0) (currentValue.toFloat() / targetValue).coerceIn(0f, 1f) else 0f
}
