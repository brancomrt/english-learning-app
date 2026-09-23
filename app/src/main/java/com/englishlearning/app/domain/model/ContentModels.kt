package com.englishlearning.app.domain.model

/**
 * Modo de exercício — define o que o usuário vê e o que deve formular.
 */
enum class ExerciseMode {
    /** Mecânica invertida: exibe a resposta, usuário formula a pergunta. */
    QUESTION_FROM_ANSWER,
    /** Modo direto: exibe uma pergunta, usuário formula a resposta. */
    ANSWER_FROM_QUESTION
}

/**
 * Resposta aceita como correta para um exercício.
 * O campo [questionText] contém a pergunta aceita no modo QUESTION_FROM_ANSWER
 * ou a resposta aceita no modo ANSWER_FROM_QUESTION.
 */
data class AcceptedAnswer(
    val id: Long = 0,
    val exerciseId: Long,
    val questionText: String,   // Pergunta ou resposta aceita, dependendo do modo
    val isPrimary: Boolean,
    val matchThreshold: Float
)

/**
 * Unidade de estudo.
 *
 * - Modo QUESTION_FROM_ANSWER: [answerText] é exibido, usuário formula a pergunta.
 * - Modo ANSWER_FROM_QUESTION: [promptText] é exibido, usuário formula a resposta.
 *
 * Requisito 1.1: exibir o prompt correto na tela de aprendizado.
 */
data class Exercise(
    val id: Long = 0,
    val levelId: Int,
    val categoryId: Int,
    /** Resposta exibida no modo QUESTION_FROM_ANSWER. */
    val answerText: String,
    /** Pergunta exibida no modo ANSWER_FROM_QUESTION. Null nos exercícios legados. */
    val promptText: String? = null,
    val contextText: String?,
    val questionType: QuestionType,
    val exerciseMode: ExerciseMode = ExerciseMode.QUESTION_FROM_ANSWER,
    val grammarFocus: String,
    val difficultyScore: Int,
    val xpReward: Int,
    val acceptedAnswers: List<AcceptedAnswer>,
    val hints: List<Hint>
) {
    /**
     * Texto principal exibido ao usuário, dependendo do modo:
     * - QUESTION_FROM_ANSWER → [answerText]
     * - ANSWER_FROM_QUESTION → [promptText] (com fallback para [answerText])
     */
    val displayText: String
        get() = when (exerciseMode) {
            ExerciseMode.QUESTION_FROM_ANSWER -> answerText
            ExerciseMode.ANSWER_FROM_QUESTION -> promptText ?: answerText
        }
}

data class Level(
    val id: Int,
    val nameKey: String,
    val descriptionKey: String,
    val minScore: Int,
    val maxScore: Int,
    val isUnlocked: Boolean,
    val sortOrder: Int
)

data class Category(
    val id: Int,
    val levelId: Int,
    val nameKey: String,
    val descriptionKey: String,
    val icon: String,
    val themeColorHex: String,
    val sortOrder: Int
)
