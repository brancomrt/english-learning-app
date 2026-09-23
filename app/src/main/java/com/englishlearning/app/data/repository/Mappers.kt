package com.englishlearning.app.data.repository

import com.englishlearning.app.data.database.dao.ExerciseWithAnswers
import com.englishlearning.app.data.database.entity.AcceptedAnswerEntity
import com.englishlearning.app.data.database.entity.CategoryEntity
import com.englishlearning.app.data.database.entity.HintEntity
import com.englishlearning.app.data.database.entity.LevelEntity
import com.englishlearning.app.domain.model.AcceptedAnswer
import com.englishlearning.app.domain.model.Category
import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.ExerciseMode
import com.englishlearning.app.domain.model.Hint
import com.englishlearning.app.domain.model.HintType
import com.englishlearning.app.domain.model.Level
import com.englishlearning.app.domain.model.QuestionType

fun ExerciseWithAnswers.toDomain(): Exercise {
    return Exercise(
        id = exercise.id,
        levelId = exercise.levelId,
        categoryId = exercise.categoryId,
        answerText = exercise.answerText,
        promptText = exercise.promptText,
        contextText = exercise.contextText,
        questionType = exercise.questionType.toQuestionType(),
        exerciseMode = exercise.exerciseMode.toExerciseMode(),
        grammarFocus = exercise.grammarFocus,
        difficultyScore = exercise.difficultyScore,
        xpReward = exercise.xpReward,
        acceptedAnswers = acceptedAnswers.map { it.toDomain() },
        hints = hints.sortedBy { it.hintOrder }.map { it.toDomain() }
    )
}

fun AcceptedAnswerEntity.toDomain(): AcceptedAnswer {
    return AcceptedAnswer(
        id = id,
        exerciseId = exerciseId,
        questionText = questionText,
        isPrimary = isPrimary,
        matchThreshold = matchThreshold
    )
}

fun HintEntity.toDomain(): Hint {
    return Hint(
        id = id,
        exerciseId = exerciseId,
        hintOrder = hintOrder,
        hintText = hintText,
        hintType = hintType.toHintType()
    )
}

fun LevelEntity.toDomain(): Level {
    return Level(
        id = id,
        nameKey = nameKey,
        descriptionKey = descriptionKey,
        minScore = minScore,
        maxScore = maxScore,
        isUnlocked = isUnlocked,
        sortOrder = sortOrder
    )
}

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        levelId = levelId,
        nameKey = nameKey,
        descriptionKey = descriptionKey,
        icon = icon,
        themeColorHex = themeColorHex,
        sortOrder = sortOrder
    )
}

private fun String.toQuestionType(): QuestionType {
    return try { QuestionType.valueOf(this) } catch (e: IllegalArgumentException) { QuestionType.YES_NO }
}

private fun String.toHintType(): HintType {
    return try { HintType.valueOf(this.uppercase()) } catch (e: IllegalArgumentException) { HintType.STRUCTURAL }
}

private fun String.toExerciseMode(): ExerciseMode {
    return try { ExerciseMode.valueOf(this) } catch (e: IllegalArgumentException) { ExerciseMode.QUESTION_FROM_ANSWER }
}
