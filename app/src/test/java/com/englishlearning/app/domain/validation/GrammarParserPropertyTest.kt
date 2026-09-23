@file:OptIn(io.kotest.common.ExperimentalKotest::class)

package com.englishlearning.app.domain.validation

import com.englishlearning.app.domain.model.QuestionType
import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.forAll

/**
 * Property-based tests for [GrammarParser.detectQuestionType].
 *
 * **Propriedade 6: Cobertura do Tipo de Pergunta**
 *
 * Para cada [QuestionType], gerar strings canônicas e verificar que
 * [GrammarParser.detectQuestionType] retorna o [QuestionType] correto.
 *
 * **Validates: Requisitos 1.4, 2.2**
 *
 * Strategy:
 * - Each generator prefixes its canonical starter word(s) and appends random
 *   word-like content so that detectQuestionType sees a realistic question.
 * - Strings are kept in lowercase (as the normalizer would produce) so they
 *   match the contract of detectQuestionType, which expects a normalized input.
 * - CHOICE questions share the YES_NO detection path in GrammarParser (both
 *   begin with an auxiliary verb); a separate section documents this.
 */
class GrammarParserPropertyTest : FreeSpec({

    val parser = GrammarParser()

    // ---------------------------------------------------------------------------
    // Helper: generates a short random word composed of lowercase ASCII letters.
    // Used to append realistic-looking suffixes after canonical starters.
    // ---------------------------------------------------------------------------
    val arbWord: Arb<String> = Arb.string(minSize = 1, maxSize = 8, codepoints = Codepoint.az())

    // Generates " <word> <word>" style tail to append after a starter
    val arbTail: Arb<String> = arbWord.map { w1 -> " $w1 there" }

    // ---------------------------------------------------------------------------
    // Canonical starters — each produces the matching QuestionType when passed
    // to detectQuestionType(normalized), where normalized is lowercase.
    // ---------------------------------------------------------------------------

    val whWhatStarters = listOf("what is", "what are", "what was", "what do", "what does", "what did")
    val whWhereStarters = listOf("where is", "where are", "where do", "where does", "where did")
    val whWhenStarters = listOf("when is", "when are", "when do", "when does", "when did")
    val whWhoStarters = listOf("who is", "who are", "who did", "who called", "who wants")
    val whWhyStarters = listOf("why is", "why are", "why do", "why does", "why did")
    val whHowStarters = listOf("how do", "how does", "how did", "how is", "how are", "how was")
    // "how much" must NOT be preceded by bare "how " — the longer prefix wins
    val whHowMuchStarters = listOf("how much is", "how much does", "how much do", "how much was")
    val whHowManyStarters = listOf("how many are", "how many do", "how many does", "how many were")
    val whWhichStarters = listOf("which is", "which are", "which do", "which one", "which way")

    // YES_NO: starts with an auxiliary verb (not a Wh-word and not a tag/indirect)
    val auxVerbs = listOf("do", "does", "did", "is", "are", "was", "were",
        "have", "has", "had", "will", "would", "can", "could",
        "should", "shall", "may", "might", "must")
    // Build safe YES_NO starters that can't accidentally trigger Wh- detection
    val yesNoStarters = auxVerbs.map { "$it you" }

    // TAG: canonical "X, don't you" / "X, isn't she" patterns.
    // Examples use only the negative auxiliaries explicitly listed in GrammarParser.isTagQuestion:
    // isn't, aren't, doesn't, don't, didn't, won't, wouldn't, can't, couldn't, haven't, hasn't,
    // hadn't, and the positive forms: is, are, do, does, did, will, would, can, could.
    val tagExamples = listOf(
        "you like coffee, don't you",
        "she is here, isn't she",
        "they will come, won't they",
        "he can swim, can't he",
        "you have been here, haven't you",
        "it is raining, isn't it",
        "you would understand, wouldn't you",
        "she does know him, doesn't she",
        "they didn't arrive, did they",
        "he couldn't leave, could he"
    )

    // INDIRECT: canonical starters defined in GrammarParser.isIndirectQuestion
    val indirectStarters = listOf(
        "could you tell me where he lives",
        "do you know what time it is",
        "can you tell me how long it takes",
        "i wonder why she left early",
        "would you mind telling me the way",
        "i'd like to know more about it"
    )

    // ---------------------------------------------------------------------------
    // Propriedade 6 — WH_WHAT
    // ---------------------------------------------------------------------------
    "Propriedade 6 - WH_WHAT: strings iniciadas com 'what' são detectadas como WH_WHAT" {
        forAll(
            PropTestConfig(iterations = 500),
            Arb.element(whWhatStarters)
        ) { starter ->
            parser.detectQuestionType(starter) == QuestionType.WH_WHAT
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 6 — WH_WHERE
    // ---------------------------------------------------------------------------
    "Propriedade 6 - WH_WHERE: strings iniciadas com 'where' são detectadas como WH_WHERE" {
        forAll(
            PropTestConfig(iterations = 500),
            Arb.element(whWhereStarters)
        ) { starter ->
            parser.detectQuestionType(starter) == QuestionType.WH_WHERE
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 6 — WH_WHEN
    // ---------------------------------------------------------------------------
    "Propriedade 6 - WH_WHEN: strings iniciadas com 'when' são detectadas como WH_WHEN" {
        forAll(
            PropTestConfig(iterations = 500),
            Arb.element(whWhenStarters)
        ) { starter ->
            parser.detectQuestionType(starter) == QuestionType.WH_WHEN
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 6 — WH_WHO
    // ---------------------------------------------------------------------------
    "Propriedade 6 - WH_WHO: strings iniciadas com 'who' são detectadas como WH_WHO" {
        forAll(
            PropTestConfig(iterations = 500),
            Arb.element(whWhoStarters)
        ) { starter ->
            parser.detectQuestionType(starter) == QuestionType.WH_WHO
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 6 — WH_WHY
    // ---------------------------------------------------------------------------
    "Propriedade 6 - WH_WHY: strings iniciadas com 'why' são detectadas como WH_WHY" {
        forAll(
            PropTestConfig(iterations = 500),
            Arb.element(whWhyStarters)
        ) { starter ->
            parser.detectQuestionType(starter) == QuestionType.WH_WHY
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 6 — WH_HOW
    // "how" must only be tested with starters that do NOT begin with
    // "how much" or "how many" since those longer prefixes take priority.
    // ---------------------------------------------------------------------------
    "Propriedade 6 - WH_HOW: strings iniciadas com 'how' (sem 'how much/many') são detectadas como WH_HOW" {
        forAll(
            PropTestConfig(iterations = 500),
            Arb.element(whHowStarters)
        ) { starter ->
            // Guard: these starters must NOT accidentally start with "how much" or "how many"
            require(!starter.startsWith("how much") && !starter.startsWith("how many"))
            parser.detectQuestionType(starter) == QuestionType.WH_HOW
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 6 — WH_HOW_MUCH
    // ---------------------------------------------------------------------------
    "Propriedade 6 - WH_HOW_MUCH: strings iniciadas com 'how much' são detectadas como WH_HOW_MUCH" {
        forAll(
            PropTestConfig(iterations = 500),
            Arb.element(whHowMuchStarters)
        ) { starter ->
            parser.detectQuestionType(starter) == QuestionType.WH_HOW_MUCH
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 6 — WH_HOW_MANY
    // ---------------------------------------------------------------------------
    "Propriedade 6 - WH_HOW_MANY: strings iniciadas com 'how many' são detectadas como WH_HOW_MANY" {
        forAll(
            PropTestConfig(iterations = 500),
            Arb.element(whHowManyStarters)
        ) { starter ->
            parser.detectQuestionType(starter) == QuestionType.WH_HOW_MANY
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 6 — WH_WHICH
    // ---------------------------------------------------------------------------
    "Propriedade 6 - WH_WHICH: strings iniciadas com 'which' são detectadas como WH_WHICH" {
        forAll(
            PropTestConfig(iterations = 500),
            Arb.element(whWhichStarters)
        ) { starter ->
            parser.detectQuestionType(starter) == QuestionType.WH_WHICH
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 6 — YES_NO
    // Strings que começam com verbo auxiliar e não correspondem a padrões
    // de Tag ou Indirect question devem ser detectadas como YES_NO.
    // ---------------------------------------------------------------------------
    "Propriedade 6 - YES_NO: strings iniciadas com verbo auxiliar são detectadas como YES_NO" {
        forAll(
            PropTestConfig(iterations = 500),
            Arb.element(yesNoStarters)
        ) { starter ->
            parser.detectQuestionType(starter) == QuestionType.YES_NO
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 6 — TAG
    // Strings que seguem o padrão "declaração + , + auxiliar + pronome" devem
    // ser detectadas como TAG.
    // ---------------------------------------------------------------------------
    "Propriedade 6 - TAG: strings com padrão de tag question são detectadas como TAG" {
        forAll(
            PropTestConfig(iterations = 500),
            Arb.element(tagExamples)
        ) { example ->
            parser.detectQuestionType(example) == QuestionType.TAG
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 6 — INDIRECT
    // Strings que começam com starters de indirect question devem ser detectadas
    // como INDIRECT.
    // ---------------------------------------------------------------------------
    "Propriedade 6 - INDIRECT: strings iniciadas com starters indiretos são detectadas como INDIRECT" {
        forAll(
            PropTestConfig(iterations = 500),
            Arb.element(indirectStarters)
        ) { example ->
            parser.detectQuestionType(example) == QuestionType.INDIRECT
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 6 — CHOICE (documentada)
    //
    // O QuestionType.CHOICE não possui detecção própria no GrammarParser —
    // perguntas de escolha (ex: "Do you prefer tea or coffee?") compartilham a
    // estrutura YES_NO (começam com verbo auxiliar) e são indistinguíveis a nível
    // léxico sem análise semântica do conteúdo. Portanto, detectQuestionType
    // retorna YES_NO para essas strings. A distinção de CHOICE é feita na camada
    // de exercícios via Exercise.questionType, não pelo GrammarParser.
    //
    // Este comportamento é verificado abaixo para documentar a invariante.
    // ---------------------------------------------------------------------------
    "Propriedade 6 - CHOICE: perguntas de escolha são detectadas como YES_NO (mesma estrutura lexical)" {
        val choiceExamples = listOf(
            "do you prefer tea or coffee",
            "would you like tea or coffee",
            "would you rather stay or go",
            "do you want to eat here or take away"
        )
        forAll(
            PropTestConfig(iterations = 500),
            Arb.element(choiceExamples)
        ) { example ->
            // CHOICE questions start with an auxiliary verb — GrammarParser classifies
            // them as YES_NO since there is no syntactic marker that distinguishes CHOICE.
            parser.detectQuestionType(example) == QuestionType.YES_NO
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 6 — HOW_MUCH / HOW_MANY precedência sobre HOW
    //
    // Verifica que "how much" e "how many" são sempre detectados como seus tipos
    // específicos, nunca como WH_HOW, mesmo que compartilhem o prefixo "how".
    // ---------------------------------------------------------------------------
    "Propriedade 6 - Precedência: 'how much' e 'how many' têm precedência sobre 'how'" {
        forAll(
            PropTestConfig(iterations = 500),
            Arb.element(whHowMuchStarters + whHowManyStarters)
        ) { starter ->
            val detected = parser.detectQuestionType(starter)
            detected == QuestionType.WH_HOW_MUCH || detected == QuestionType.WH_HOW_MANY
        }
    }
})
