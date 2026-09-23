package com.englishlearning.app.domain.model

/**
 * Tipos gramaticais de perguntas suportados pelo aplicativo.
 * Requisito 1.4: suporte a 13 tipos de perguntas.
 */
enum class QuestionType {
    WH_WHAT,      // What is your name?
    WH_WHERE,     // Where do you live?
    WH_WHEN,      // When did it happen?
    WH_WHO,       // Who called you?
    WH_WHY,       // Why are you late?
    WH_HOW,       // How do you feel?
    WH_HOW_MUCH,  // How much does it cost?
    WH_HOW_MANY,  // How many people are there?
    WH_WHICH,     // Which one do you prefer?
    YES_NO,       // Do you like coffee?
    TAG,          // You like coffee, don't you?
    INDIRECT,     // Could you tell me where he lives?
    CHOICE;       // Do you prefer tea or coffee?

    fun displayName(): String = when (this) {
        WH_WHAT     -> "Wh-question (What)"
        WH_WHERE    -> "Wh-question (Where)"
        WH_WHEN     -> "Wh-question (When)"
        WH_WHO      -> "Wh-question (Who)"
        WH_WHY      -> "Wh-question (Why)"
        WH_HOW      -> "Wh-question (How)"
        WH_HOW_MUCH -> "Wh-question (How much)"
        WH_HOW_MANY -> "Wh-question (How many)"
        WH_WHICH    -> "Wh-question (Which)"
        YES_NO      -> "Yes/No question"
        TAG         -> "Tag question"
        INDIRECT    -> "Indirect question"
        CHOICE      -> "Choice question"
    }
}
