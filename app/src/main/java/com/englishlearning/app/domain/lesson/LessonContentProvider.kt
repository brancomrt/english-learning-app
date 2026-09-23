package com.englishlearning.app.domain.lesson

import com.englishlearning.app.domain.model.QuestionType

/**
 * Provedor estático de [LessonContent] mapeado por [QuestionType].
 *
 * Definido inteiramente em código Kotlin, sem dependência de arquivos JSON ou rede.
 *
 * Requisito 15.9: conteúdo embutido em código Kotlin.
 * Requisito 15.10: cobre todos os 13 QuestionTypes suportados.
 * Requisito 15.12: retorna null para tipos sem mapeamento (não deve ocorrer com os 13 tipos
 *   suportados, mas garante comportamento seguro caso novos tipos sejam adicionados).
 */
object LessonContentProvider {

    private val contentMap: Map<QuestionType, LessonContent> = mapOf(
        QuestionType.WH_WHAT     to LessonContent.WhatLesson,
        QuestionType.WH_WHERE    to LessonContent.WhereLesson,
        QuestionType.WH_WHEN     to LessonContent.WhenLesson,
        QuestionType.WH_WHO      to LessonContent.WhoLesson,
        QuestionType.WH_WHY      to LessonContent.WhyLesson,
        QuestionType.WH_HOW      to LessonContent.HowLesson,
        QuestionType.WH_HOW_MUCH to LessonContent.HowMuchLesson,
        QuestionType.WH_HOW_MANY to LessonContent.HowManyLesson,
        QuestionType.WH_WHICH    to LessonContent.WhichLesson,
        QuestionType.YES_NO      to LessonContent.YesNoLesson,
        QuestionType.TAG         to LessonContent.TagLesson,
        QuestionType.INDIRECT    to LessonContent.IndirectLesson,
        QuestionType.CHOICE      to LessonContent.ChoiceLesson
    )

    /**
     * Retorna o [LessonContent] correspondente ao [QuestionType] informado,
     * ou `null` se não houver mapeamento (ex.: tipo adicionado futuramente).
     */
    fun forQuestionType(type: QuestionType): LessonContent? = contentMap[type]

    /**
     * Alias de [forQuestionType] para uso por categoria.
     */
    fun forCategory(questionType: QuestionType): LessonContent? = forQuestionType(questionType)
}
