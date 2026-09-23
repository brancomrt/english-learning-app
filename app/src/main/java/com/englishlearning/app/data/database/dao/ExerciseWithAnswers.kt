package com.englishlearning.app.data.database.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.englishlearning.app.data.database.entity.AcceptedAnswerEntity
import com.englishlearning.app.data.database.entity.ExerciseEntity
import com.englishlearning.app.data.database.entity.HintEntity

/**
 * Modelo de leitura que agrega um exercício com suas respostas aceitas e dicas.
 * Usado pela UI/ViewModel para exibir o exercício completo na sessão de aprendizado.
 *
 * Requisito 1.1: exibição da resposta do exercício (exercise.answerText).
 * Requisito 3.1: dicas progressivas (hints ordenadas por hint_order).
 * Requisito 2.5: validação semântica usando acceptedAnswers.
 */
data class ExerciseWithAnswers(

    @Embedded
    val exercise: ExerciseEntity,

    /**
     * Respostas aceitas vinculadas a este exercício.
     * A resposta canônica principal tem isPrimary = true.
     */
    @Relation(
        parentColumn = "id",
        entityColumn = "exercise_id"
    )
    val acceptedAnswers: List<AcceptedAnswerEntity>,

    /**
     * Dicas progressivas vinculadas a este exercício.
     * Devem ser ordenadas por hint_order ao exibir ao usuário.
     */
    @Relation(
        parentColumn = "id",
        entityColumn = "exercise_id"
    )
    val hints: List<HintEntity>
)
