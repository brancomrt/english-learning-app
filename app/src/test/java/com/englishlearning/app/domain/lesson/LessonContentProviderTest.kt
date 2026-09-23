package com.englishlearning.app.domain.lesson

import com.englishlearning.app.domain.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para [LessonContentProvider] e [LessonContent].
 *
 * Verifica:
 * - Todos os 13 QuestionTypes retornam LessonContent não-nulo.
 * - Cada LessonContent possui exemplos, fórmula e explicação não-vazios.
 * - Ao menos 2 exemplos são fornecidos por lição.
 * - O questionType de cada lição corresponde ao tipo solicitado.
 * - forCategory é equivalente a forQuestionType.
 */
class LessonContentProviderTest {

    private val allTypes = QuestionType.values().toList()

    // ── Completude ────────────────────────────────────────────────────────────

    @Test
    fun `forQuestionType returns non-null for all 13 supported QuestionTypes`() {
        allTypes.forEach { type ->
            assertNotNull(
                "LessonContent para $type não deveria ser null",
                LessonContentProvider.forQuestionType(type)
            )
        }
    }

    @Test
    fun `all 13 QuestionTypes are covered`() {
        assertEquals(
            "Existem exatamente 13 QuestionTypes",
            13,
            allTypes.size
        )
        allTypes.forEach { type ->
            assertNotNull(
                "Nenhum mapeamento encontrado para $type",
                LessonContentProvider.forQuestionType(type)
            )
        }
    }

    // ── Conteúdo não-vazio ────────────────────────────────────────────────────

    @Test
    fun `each LessonContent has non-empty ruleExplanationPt`() {
        allTypes.forEach { type ->
            val content = LessonContentProvider.forQuestionType(type)!!
            assertTrue(
                "ruleExplanationPt de $type está vazio",
                content.ruleExplanationPt.isNotBlank()
            )
        }
    }

    @Test
    fun `each LessonContent has non-empty structuralFormula`() {
        allTypes.forEach { type ->
            val content = LessonContentProvider.forQuestionType(type)!!
            assertTrue(
                "structuralFormula de $type está vazia",
                content.structuralFormula.isNotBlank()
            )
        }
    }

    @Test
    fun `each LessonContent has non-empty titlePt`() {
        allTypes.forEach { type ->
            val content = LessonContentProvider.forQuestionType(type)!!
            assertTrue(
                "titlePt de $type está vazio",
                content.titlePt.isNotBlank()
            )
        }
    }

    @Test
    fun `each LessonContent has at least one example`() {
        allTypes.forEach { type ->
            val content = LessonContentProvider.forQuestionType(type)!!
            assertTrue(
                "examples de $type está vazia",
                content.examples.isNotEmpty()
            )
        }
    }

    @Test
    fun `each LessonContent has at least two examples`() {
        allTypes.forEach { type ->
            val content = LessonContentProvider.forQuestionType(type)!!
            assertTrue(
                "examples de $type deve ter ao menos 2 itens, encontrado: ${content.examples.size}",
                content.examples.size >= 2
            )
        }
    }

    @Test
    fun `all examples are non-blank strings`() {
        allTypes.forEach { type ->
            val content = LessonContentProvider.forQuestionType(type)!!
            content.examples.forEachIndexed { idx, example ->
                assertTrue(
                    "Exemplo $idx de $type está em branco",
                    example.isNotBlank()
                )
            }
        }
    }

    // ── Correspondência de tipo ───────────────────────────────────────────────

    @Test
    fun `returned LessonContent questionType matches the requested QuestionType`() {
        allTypes.forEach { type ->
            val content = LessonContentProvider.forQuestionType(type)!!
            assertEquals(
                "questionType do LessonContent de $type não corresponde",
                type,
                content.questionType
            )
        }
    }

    // ── Alias forCategory ─────────────────────────────────────────────────────

    @Test
    fun `forCategory returns same result as forQuestionType`() {
        allTypes.forEach { type ->
            assertEquals(
                "forCategory e forQuestionType diferem para $type",
                LessonContentProvider.forQuestionType(type),
                LessonContentProvider.forCategory(type)
            )
        }
    }

    // ── Verificações específicas por tipo ─────────────────────────────────────

    @Test
    fun `WH_WHAT lesson content is correct`() {
        val content = LessonContentProvider.forQuestionType(QuestionType.WH_WHAT)!!
        assertEquals(QuestionType.WH_WHAT, content.questionType)
        assertTrue(content.examples.any { it.contains("What", ignoreCase = true) })
    }

    @Test
    fun `WH_WHERE lesson content is correct`() {
        val content = LessonContentProvider.forQuestionType(QuestionType.WH_WHERE)!!
        assertEquals(QuestionType.WH_WHERE, content.questionType)
        assertTrue(content.examples.any { it.contains("Where", ignoreCase = true) })
    }

    @Test
    fun `WH_WHEN lesson content is correct`() {
        val content = LessonContentProvider.forQuestionType(QuestionType.WH_WHEN)!!
        assertEquals(QuestionType.WH_WHEN, content.questionType)
        assertTrue(content.examples.any { it.contains("When", ignoreCase = true) })
    }

    @Test
    fun `WH_WHO lesson content is correct`() {
        val content = LessonContentProvider.forQuestionType(QuestionType.WH_WHO)!!
        assertEquals(QuestionType.WH_WHO, content.questionType)
        assertTrue(content.examples.any { it.contains("Who", ignoreCase = true) })
    }

    @Test
    fun `WH_WHY lesson content is correct`() {
        val content = LessonContentProvider.forQuestionType(QuestionType.WH_WHY)!!
        assertEquals(QuestionType.WH_WHY, content.questionType)
        assertTrue(content.examples.any { it.contains("Why", ignoreCase = true) })
    }

    @Test
    fun `WH_HOW lesson content is correct`() {
        val content = LessonContentProvider.forQuestionType(QuestionType.WH_HOW)!!
        assertEquals(QuestionType.WH_HOW, content.questionType)
        assertTrue(content.examples.any { it.contains("How", ignoreCase = true) })
    }

    @Test
    fun `WH_HOW_MUCH lesson content is correct`() {
        val content = LessonContentProvider.forQuestionType(QuestionType.WH_HOW_MUCH)!!
        assertEquals(QuestionType.WH_HOW_MUCH, content.questionType)
        assertTrue(content.examples.any { it.contains("How much", ignoreCase = true) })
    }

    @Test
    fun `WH_HOW_MANY lesson content is correct`() {
        val content = LessonContentProvider.forQuestionType(QuestionType.WH_HOW_MANY)!!
        assertEquals(QuestionType.WH_HOW_MANY, content.questionType)
        assertTrue(content.examples.any { it.contains("How many", ignoreCase = true) })
    }

    @Test
    fun `WH_WHICH lesson content is correct`() {
        val content = LessonContentProvider.forQuestionType(QuestionType.WH_WHICH)!!
        assertEquals(QuestionType.WH_WHICH, content.questionType)
        assertTrue(content.examples.any { it.contains("Which", ignoreCase = true) })
    }

    @Test
    fun `YES_NO lesson content is correct`() {
        val content = LessonContentProvider.forQuestionType(QuestionType.YES_NO)!!
        assertEquals(QuestionType.YES_NO, content.questionType)
        assertTrue(content.structuralFormula.isNotBlank())
    }

    @Test
    fun `TAG lesson content is correct`() {
        val content = LessonContentProvider.forQuestionType(QuestionType.TAG)!!
        assertEquals(QuestionType.TAG, content.questionType)
        // tag questions have a comma separating the statement from the tag
        assertTrue(content.examples.any { it.contains(",") })
    }

    @Test
    fun `INDIRECT lesson content is correct`() {
        val content = LessonContentProvider.forQuestionType(QuestionType.INDIRECT)!!
        assertEquals(QuestionType.INDIRECT, content.questionType)
        assertTrue(content.structuralFormula.isNotBlank())
    }

    @Test
    fun `CHOICE lesson content is correct`() {
        val content = LessonContentProvider.forQuestionType(QuestionType.CHOICE)!!
        assertEquals(QuestionType.CHOICE, content.questionType)
        // choice questions contain "or"
        assertTrue(content.examples.any { it.contains(" or ", ignoreCase = true) })
    }
}
