package com.englishlearning.app.domain.lesson

import com.englishlearning.app.domain.model.QuestionType

/**
 * Conteúdo de uma MiniLesson gramatical para um tipo de pergunta específico.
 *
 * Requisito 15.9: definido inteiramente em código Kotlin.
 * Requisito 15.10: cobre todos os 13 QuestionTypes suportados.
 * Requisito 15.11: explicações em português; fórmulas e exemplos em inglês.
 *
 * @property questionType   O tipo de pergunta ao qual esta lição corresponde.
 * @property titlePt        Título curto em português exibido na tela.
 * @property ruleExplanationPt Regra gramatical explicada em 2-3 frases em português.
 * @property structuralFormula Fórmula estrutural da pergunta em inglês.
 * @property examples       Lista de 2 exemplos de perguntas em inglês.
 * @property britishVsAmerican Diferenças entre inglês britânico e americano (opcional).
 */
sealed class LessonContent(
    val questionType: QuestionType,
    val titlePt: String,
    val ruleExplanationPt: String,
    val structuralFormula: String,
    val examples: List<String>,
    val britishVsAmerican: String? = null
) {
    // ---- Wh-questions ----

    object WhatLesson : LessonContent(
        questionType = QuestionType.WH_WHAT,
        titlePt = "Perguntas com WHAT",
        ruleExplanationPt = "Usamos 'What' para perguntar sobre coisas, ações ou informações em geral. " +
            "Depois de 'What', colocamos um verbo auxiliar (do/does/is/are) seguido do sujeito e do verbo principal. " +
            "Esse tipo de pergunta é muito comum no inglês cotidiano.",
        structuralFormula = "What + auxiliary verb + subject + main verb?",
        examples = listOf(
            "What is your name?",
            "What do you do for work?"
        ),
        britishVsAmerican = """
            🇬🇧 "What's your favourite colour?" (favourite com 'u')
            🇺🇸 "What's your favorite color?" (favorite sem 'u')
            
            🇬🇧 "What have you got?" é comum
            🇺🇸 "What do you have?" é preferido
        """.trimIndent()
    )

    object WhereLesson : LessonContent(
        questionType = QuestionType.WH_WHERE,
        titlePt = "Perguntas com WHERE",
        ruleExplanationPt = "Usamos 'Where' para perguntar sobre lugar ou localização. " +
            "A estrutura segue o mesmo padrão das Wh-questions: palavra interrogativa, auxiliar, sujeito e verbo. " +
            "Pode ser usada tanto no presente quanto no passado.",
        structuralFormula = "Where + auxiliary verb + subject + main verb?",
        examples = listOf(
            "Where do you live?",
            "Where is the nearest station?"
        )
    )

    object WhenLesson : LessonContent(
        questionType = QuestionType.WH_WHEN,
        titlePt = "Perguntas com WHEN",
        ruleExplanationPt = "Usamos 'When' para perguntar sobre tempo, data ou horário. " +
            "Combinamos 'When' com o auxiliar adequado ao tempo verbal da frase. " +
            "Essa estrutura funciona nos tempos presente, passado e futuro.",
        structuralFormula = "When + auxiliary verb + subject + main verb?",
        examples = listOf(
            "When does the class start?",
            "When were you born?"
        )
    )

    object WhoLesson : LessonContent(
        questionType = QuestionType.WH_WHO,
        titlePt = "Perguntas com WHO",
        ruleExplanationPt = "Usamos 'Who' para perguntar sobre pessoas. " +
            "Quando 'Who' é o sujeito da frase, não precisamos de verbo auxiliar. " +
            "Quando 'Who' é o objeto, usamos a estrutura normal com auxiliar.",
        structuralFormula = "Who + verb / Who + auxiliary verb + subject + main verb?",
        examples = listOf(
            "Who is your English teacher?",
            "Who called you this morning?"
        )
    )

    object WhyLesson : LessonContent(
        questionType = QuestionType.WH_WHY,
        titlePt = "Perguntas com WHY",
        ruleExplanationPt = "Usamos 'Why' para perguntar sobre motivos ou razões. " +
            "A resposta geralmente começa com 'Because' (porque). " +
            "Segue a estrutura padrão das Wh-questions com verbo auxiliar.",
        structuralFormula = "Why + auxiliary verb + subject + main verb?",
        examples = listOf(
            "Why are you late today?",
            "Why did she leave so early?"
        )
    )

    object HowLesson : LessonContent(
        questionType = QuestionType.WH_HOW,
        titlePt = "Perguntas com HOW",
        ruleExplanationPt = "Usamos 'How' para perguntar sobre maneira, modo ou estado. " +
            "Pode ser combinado com adjetivos (how old, how long) ou usado sozinho. " +
            "É uma das palavras interrogativas mais versáteis do inglês.",
        structuralFormula = "How + auxiliary verb + subject + main verb?",
        examples = listOf(
            "How do you get to work?",
            "How was your day today?"
        )
    )

    object HowMuchLesson : LessonContent(
        questionType = QuestionType.WH_HOW_MUCH,
        titlePt = "Perguntas com HOW MUCH",
        ruleExplanationPt = "Usamos 'How much' para perguntar sobre quantidade de substantivos incontáveis (dinheiro, tempo, água). " +
            "Também é usado para perguntar sobre preço ou quantidade de algo não numerável. " +
            "Diferente de 'How many', 'How much' não usa plurais.",
        structuralFormula = "How much + (noun) + auxiliary verb + subject + verb?",
        examples = listOf(
            "How much does it cost?",
            "How much time do we have left?"
        ),
        britishVsAmerican = """
            🇬🇧 "How much is it?" → resposta em pounds (£)
            🇺🇸 "How much is it?" → resposta em dollars ($)
            
            Vocabulário de dinheiro:
            🇬🇧 pounds, pence, quid (informal)
            🇺🇸 dollars, cents, bucks (informal)
        """.trimIndent()
    )

    object HowManyLesson : LessonContent(
        questionType = QuestionType.WH_HOW_MANY,
        titlePt = "Perguntas com HOW MANY",
        ruleExplanationPt = "Usamos 'How many' para perguntar sobre quantidade de substantivos contáveis (pessoas, livros, carros). " +
            "O substantivo que segue 'How many' sempre fica no plural. " +
            "É muito útil para contextos em que precisamos de um número específico.",
        structuralFormula = "How many + plural noun + auxiliary verb + subject + verb?",
        examples = listOf(
            "How many people are there in your family?",
            "How many books did you read last year?"
        )
    )

    object WhichLesson : LessonContent(
        questionType = QuestionType.WH_WHICH,
        titlePt = "Perguntas com WHICH",
        ruleExplanationPt = "Usamos 'Which' para perguntar sobre uma escolha entre opções limitadas e definidas. " +
            "Diferente de 'What', 'Which' pressupõe um conjunto restrito de alternativas. " +
            "Pode ser seguido de um substantivo ou usado sozinho.",
        structuralFormula = "Which + (noun) + auxiliary verb + subject + main verb?",
        examples = listOf(
            "Which color do you prefer?",
            "Which bus goes to the city center?"
        )
    )

    // ---- Other question types ----

    object YesNoLesson : LessonContent(
        questionType = QuestionType.YES_NO,
        titlePt = "Perguntas de Sim ou Não",
        ruleExplanationPt = "As perguntas de Sim/Não (Yes/No questions) começam sempre com um verbo auxiliar " +
            "(do, does, did, is, are, was, were, will, can, etc.). " +
            "A resposta é sempre 'Yes' ou 'No', geralmente seguida de um sujeito e auxiliar.",
        structuralFormula = "Auxiliary verb + subject + main verb + (complement)?",
        examples = listOf(
            "Do you speak English?",
            "Are you a student?"
        )
    )

    object TagLesson : LessonContent(
        questionType = QuestionType.TAG,
        titlePt = "Perguntas com Tag Question",
        ruleExplanationPt = "As tag questions são pequenas perguntas adicionadas ao final de uma afirmação para pedir confirmação. " +
            "Se a afirmação é positiva, a tag é negativa; se a afirmação é negativa, a tag é positiva. " +
            "O auxiliar e o pronome da tag devem corresponder ao sujeito e tempo verbal da afirmação.",
        structuralFormula = "Statement + , + auxiliary (negative/positive) + pronoun?",
        examples = listOf(
            "It's cold today, isn't it?",
            "You like coffee, don't you?"
        ),
        britishVsAmerican = """
            🇬🇧 Tag questions são MUITO comuns e naturais
            → "Lovely day, isn't it?" / "You're from Brazil, aren't you?"
            
            🇺🇸 Menos frequentes; "right?" é mais usado
            → "Lovely day, right?" / "You're from Brazil, right?"
            
            🇬🇧 Entonação descendente = espera concordância
            🇬🇧 Entonação ascendente = pergunta genuína
        """.trimIndent()
    )

    object IndirectLesson : LessonContent(
        questionType = QuestionType.INDIRECT,
        titlePt = "Perguntas Indiretas",
        ruleExplanationPt = "As perguntas indiretas são mais formais e educadas do que as diretas. " +
            "Começam com frases introdutórias como 'Could you tell me...', 'Do you know...' ou 'I wonder...'. " +
            "Importante: após a frase introdutória, a ordem das palavras é a de uma afirmação (sujeito + verbo), não de uma pergunta.",
        structuralFormula = "Introductory phrase + question word + subject + verb?",
        examples = listOf(
            "Could you tell me where the library is?",
            "Do you know what time it is?"
        ),
        britishVsAmerican = """
            🇬🇧 Britânicos usam mais perguntas indiretas por polidez
            → "I was wondering if you could help me..."
            → "Would you mind telling me...?"
            
            🇺🇸 Americanos tendem a ser mais diretos
            → "Can you help me?" / "Where is...?"
            
            Ambos aceitam as duas formas, mas a frequência varia.
        """.trimIndent()
    )

    object ChoiceLesson : LessonContent(
        questionType = QuestionType.CHOICE,
        titlePt = "Perguntas de Escolha (Or Questions)",
        ruleExplanationPt = "As perguntas de escolha apresentam duas ou mais opções usando a conjunção 'or'. " +
            "Estruturalmente, seguem o padrão das perguntas Yes/No, mas com alternativas ligadas por 'or'. " +
            "O ouvinte deve escolher uma das opções apresentadas, não apenas responder sim ou não.",
        structuralFormula = "Auxiliary verb + subject + verb + option A + or + option B?",
        examples = listOf(
            "Would you like tea or coffee?",
            "Do you prefer morning or evening classes?"
        )
    )
}
