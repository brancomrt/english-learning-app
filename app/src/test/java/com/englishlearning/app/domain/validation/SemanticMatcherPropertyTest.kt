package com.englishlearning.app.domain.validation

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.pair
import io.kotest.property.forAll

/**
 * Property-based tests for [SemanticMatcher].
 *
 * Validates: Requisitos 10.6, 10.7
 */
class SemanticMatcherPropertyTest : FreeSpec({

    val matcher = SemanticMatcher()

    // ---------------------------------------------------------------------------
    // Propriedade 2: Simetria do Score Semântico
    // score(a, b) == score(b, a) para quaisquer strings a e b
    // Valida: Requisitos 10.7
    // ---------------------------------------------------------------------------
    "Propriedade 2 - Simetria do Score Semântico: score(a, b) == score(b, a)" {
        forAll(
            iterations = 1000,
            Arb.pair(Arb.string(), Arb.string())
        ) { (a, b) ->
            val scoreAB = matcher.score(a, b)
            val scoreBA = matcher.score(b, a)
            // Compare with a small tolerance to handle floating point rounding
            kotlin.math.abs(scoreAB - scoreBA) < 1e-6f
        }
    }

    // ---------------------------------------------------------------------------
    // Propriedade 3: Score de Strings Idênticas é 1.0
    // score(x, x) == 1.0f para qualquer string não-vazia x
    // Valida: Requisitos 10.6
    // ---------------------------------------------------------------------------
    "Propriedade 3 - Score de Strings Idênticas é 1.0: score(x, x) == 1.0f" {
        forAll(
            iterations = 1000,
            Arb.string(minSize = 1)
        ) { x ->
            matcher.score(x, x) == 1.0f
        }
    }
})
