package com.englishlearning.app.domain.validation

import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.string
import io.kotest.property.forAll

/**
 * Property-based tests for TextNormalizer.
 *
 * Validates: Requirements 10.4
 */
class TextNormalizerPropertyTest : FreeSpec({

    val normalizer = TextNormalizer()

    /**
     * Propriedade 1: Idempotência da Normalização
     *
     * Para qualquer string de entrada x, aplicar normalize duas vezes deve produzir
     * o mesmo resultado que aplicar uma vez: normalize(normalize(x)) == normalize(x).
     *
     * Valida: Requisitos 10.4
     */
    "Propriedade 1 - Idempotência da Normalização: normalize(normalize(x)) == normalize(x)" {
        forAll(
            PropTestConfig(iterations = 1000),
            Arb.string()
        ) { x ->
            val once = normalizer.normalize(x)
            val twice = normalizer.normalize(once)
            once == twice
        }
    }
})
