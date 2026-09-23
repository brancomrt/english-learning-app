package com.englishlearning.app.data.database.dao

/**
 * Holder de dados para resultado agregado de estatísticas do usuário.
 * NÃO é uma entidade Room — é usado apenas como tipo de retorno de query de agregação.
 *
 * Requisito 9.3: agrega totais de XP, exercícios, respostas corretas e tentativas.
 */
data class UserStatsEntity(
    /** Total de XP acumulado pelo usuário em todas as tentativas. */
    val totalXp: Int,

    /** Total de exercícios únicos completados pelo usuário. */
    val totalExercises: Int,

    /** Total de tentativas com status CORRECT. */
    val correctAnswers: Int,

    /** Total de todas as tentativas registradas. */
    val totalAttempts: Int
)
