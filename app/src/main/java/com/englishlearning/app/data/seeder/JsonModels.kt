package com.englishlearning.app.data.seeder

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelos JSON para desserialização de conteúdo dos assets.
 *
 * Estes modelos espelham a estrutura dos arquivos JSON em assets/content/
 * e são mapeados para entidades Room pelo DatabaseSeeder.
 *
 * Requisito 11.1: app lê arquivos JSON dos assets e popula Room na primeira inicialização.
 * Requisito 11.2: serialização via kotlinx.serialization.
 * Requisito 11.3: round-trip de serialização/desserialização preserva equivalência.
 */

// ---------------------------------------------------------------------------
// Exercise models
// ---------------------------------------------------------------------------

/**
 * Modelo JSON para um exercício da mecânica invertida.
 * Corresponde a [com.englishlearning.app.data.database.entity.ExerciseEntity].
 *
 * Requisito 9.2: conteúdo populado a partir de arquivos JSON nos assets.
 */
@Serializable
data class ExerciseJsonModel(
    val id: Long,
    @SerialName("level_id") val levelId: Int,
    @SerialName("category_id") val categoryId: Int,
    @SerialName("answer_text") val answerText: String,
    @SerialName("prompt_text") val promptText: String? = null,
    @SerialName("context_text") val contextText: String? = null,
    @SerialName("question_type") val questionType: String,
    @SerialName("exercise_mode") val exerciseMode: String = "QUESTION_FROM_ANSWER",
    @SerialName("grammar_focus") val grammarFocus: String,
    @SerialName("difficulty_score") val difficultyScore: Int,
    @SerialName("xp_reward") val xpReward: Int = 10,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("accepted_answers") val acceptedAnswers: List<AcceptedAnswerJsonModel> = emptyList(),
    val hints: List<HintJsonModel> = emptyList()
)

/**
 * Modelo JSON para uma resposta aceita de um exercício.
 * Corresponde a [com.englishlearning.app.data.database.entity.AcceptedAnswerEntity].
 */
@Serializable
data class AcceptedAnswerJsonModel(
    val id: Long = 0,
    @SerialName("question_text") val questionText: String,
    @SerialName("is_primary") val isPrimary: Boolean = false,
    @SerialName("match_threshold") val matchThreshold: Float = 0.85f
)

/**
 * Modelo JSON para uma dica progressiva de um exercício.
 * Corresponde a [com.englishlearning.app.data.database.entity.HintEntity].
 *
 * Requisito 3.2: dicas reveladas progressivamente em ordem de hintOrder.
 */
@Serializable
data class HintJsonModel(
    val id: Long = 0,
    @SerialName("hint_order") val hintOrder: Int,
    @SerialName("hint_text") val hintText: String,
    @SerialName("hint_type") val hintType: String = "structural"
)

// ---------------------------------------------------------------------------
// Level and Category models
// ---------------------------------------------------------------------------

/**
 * Modelo JSON para um nível de dificuldade.
 * Corresponde a [com.englishlearning.app.data.database.entity.LevelEntity].
 *
 * Arquivo JSON esperado: assets/content/levels.json
 *
 * Requisito 6.1: conteúdo organizado em níveis progressivos.
 */
@Serializable
data class LevelJsonModel(
    val id: Int,
    @SerialName("name_key") val nameKey: String,
    @SerialName("description_key") val descriptionKey: String,
    @SerialName("min_score") val minScore: Int = 0,
    @SerialName("max_score") val maxScore: Int = 100,
    @SerialName("is_unlocked") val isUnlocked: Boolean = false,
    @SerialName("sort_order") val sortOrder: Int = 0
)

/**
 * Modelo JSON para uma categoria temática dentro de um nível.
 * Corresponde a [com.englishlearning.app.data.database.entity.CategoryEntity].
 *
 * Arquivo JSON esperado: assets/content/categories.json
 *
 * Requisito 7.1: exercícios organizados em categorias temáticas.
 */
@Serializable
data class CategoryJsonModel(
    val id: Int,
    @SerialName("level_id") val levelId: Int,
    @SerialName("name_key") val nameKey: String,
    @SerialName("description_key") val descriptionKey: String,
    val icon: String = "📚",
    @SerialName("theme_color_hex") val themeColorHex: String = "#4CAF50",
    @SerialName("sort_order") val sortOrder: Int = 0
)

// ---------------------------------------------------------------------------
// Vocabulary card models
// ---------------------------------------------------------------------------

/**
 * Wrapper para o array de cards dentro do arquivo JSON de vocabulário.
 *
 * Arquivo esperado: assets/content/vocabulary/level_N_vocab.json
 * Estrutura: `{ "cards": [ ... ] }`
 *
 * Requisito 16.9: VocabularyCards nos arquivos JSON de conteúdo.
 * Requisito 16.10: suporte a VocabularyCards no sistema SRS.
 */
@Serializable
data class VocabularyFileJsonModel(
    val cards: List<VocabularyJsonModel>
)

/**
 * Modelo JSON para um vocabulary card.
 * Corresponde a [com.englishlearning.app.data.database.entity.VocabularyCardEntity].
 *
 * Requisito 16.9: campos word (String inglês), definition (String PT-BR),
 *                 exampleSentence (String inglês), levelId (Int), categoryId (Int).
 */
@Serializable
data class VocabularyJsonModel(
    val word: String,
    val definition: String,
    @SerialName("example_sentence") val exampleSentence: String,
    @SerialName("level_id") val levelId: Int,
    @SerialName("category_id") val categoryId: Int,
    @SerialName("audio_path") val audioPath: String? = null,
    @SerialName("image_url") val imageUrl: String? = null
)

// ---------------------------------------------------------------------------
// Grammar Lesson models (v1.28.0)
// ---------------------------------------------------------------------------

/**
 * Modelo JSON para uma lição de gramática.
 * Corresponde a [com.englishlearning.app.data.database.entity.GrammarLessonEntity].
 */
@Serializable
data class GrammarLessonJsonModel(
    val id: Long,
    val levelId: Int,
    val grammarTopic: String,
    val title: String,
    val explanation: String,
    val formula: String? = null,
    val sortOrder: Int = 0,
    val iconEmoji: String = "📚",
    val examples: List<GrammarExampleJsonModel> = emptyList()
)

/**
 * Modelo JSON para um exemplo de gramática.
 * Corresponde a [com.englishlearning.app.data.database.entity.GrammarExampleEntity].
 */
@Serializable
data class GrammarExampleJsonModel(
    val sentence: String,
    val translation: String,
    val highlight: String? = null,
    val note: String? = null,
    val isCorrect: Boolean = true,
    val sortOrder: Int = 0
)

// ---------------------------------------------------------------------------
// Dictation models (v1.30.0) - Callan Method Style
// ---------------------------------------------------------------------------

/**
 * Modelo JSON para um exercício de ditado no estilo Callan.
 * Corresponde a [com.englishlearning.app.data.database.entity.DictationExerciseEntity].
 */
@Serializable
data class DictationExerciseJsonModel(
    val id: Long,
    val stage: Int,
    val title: String,
    val text: String,
    val translation: String? = null,
    val wordCount: Int = 0,
    val grammarFocus: String? = null
)
