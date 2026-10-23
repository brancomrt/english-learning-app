package com.englishlearning.app.ui.learning

/**
 * Conteúdo teórico para cada conceito gramatical.
 * Mapeado a partir do campo grammar_focus dos exercícios.
 */
data class GrammarTheory(
    val title: String,
    val explanation: String,
    val structure: String,
    val examples: List<String>,
    val tip: String? = null,
    val britishVsAmerican: String? = null // Diferenças entre inglês britânico e americano
)

object GrammarTheoryProvider {

    fun getTheory(grammarFocus: String): GrammarTheory {
        val key = grammarFocus.lowercase().trim()
        return theories.entries
            .firstOrNull { key.contains(it.key) }
            ?.value
            ?: defaultTheory(grammarFocus)
    }

    private fun defaultTheory(focus: String) = GrammarTheory(
        title = focus.split("-").first().trim()
            .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
        explanation = "Este exercício foca em: $focus.",
        structure = "Consulte seu material de referência para mais detalhes.",
        examples = emptyList()
    )

    private val theories: Map<String, GrammarTheory> = mapOf(

        // ── Present Simple ────────────────────────────────────────────────
        "present simple" to GrammarTheory(
            title = "Present Simple (Presente Simples)",
            explanation = "Usado para fatos, hábitos, rotinas e verdades gerais. É o tempo verbal mais básico do inglês.",
            structure = """
                (+) Subject + verb (3ª pessoa: verb + s/es)
                (-) Subject + do/does + not + verb
                (?) Do/Does + subject + verb?
            """.trimIndent(),
            examples = listOf(
                "I work from home every day.",
                "She doesn't like coffee.",
                "Do you speak English?",
                "He starts work at nine."
            ),
            tip = "3ª pessoa singular (he/she/it): adicione -s ou -es ao verbo.",
            britishVsAmerican = """
                🇬🇧 Britânico: Verbos coletivos podem ser plural → "The team are working..."
                🇺🇸 Americano: Verbos coletivos geralmente singular → "The team is working..."
                
                🇬🇧 "Have you got...?" é mais comum
                🇺🇸 "Do you have...?" é mais comum
            """.trimIndent()
        ),

        // ── Present Continuous ────────────────────────────────────────────
        "present continuous" to GrammarTheory(
            title = "Present Continuous (Presente Contínuo)",
            explanation = "Usado para ações que estão acontecendo agora ou planos futuros próximos.",
            structure = """
                (+) Subject + am/is/are + verb-ing
                (-) Subject + am/is/are + not + verb-ing
                (?) Am/Is/Are + subject + verb-ing?
            """.trimIndent(),
            examples = listOf(
                "I'm working on the bug right now.",
                "We are deploying the update tonight.",
                "Are you going to the conference?",
                "The server is running out of memory."
            ),
            tip = "Contrações: I'm, you're, he's, she's, it's, we're, they're."
        ),

        // ── Present Perfect ───────────────────────────────────────────────
        "present perfect" to GrammarTheory(
            title = "Present Perfect (Presente Perfeito)",
            explanation = "Conecta o passado ao presente. Usado para experiências, ações recentes ou estados que continuam.",
            structure = """
                (+) Subject + have/has + past participle
                (-) Subject + have/has + not + past participle
                (?) Have/Has + subject + past participle?
            """.trimIndent(),
            examples = listOf(
                "I have already pushed the fix.",
                "She hasn't reviewed the PR yet.",
                "Have you ever used Kubernetes?",
                "The pipeline has failed three times today."
            ),
            tip = "Palavras-chave: already, yet, ever, never, just, for, since.",
            britishVsAmerican = """
                🇬🇧 Britânico: Usa Present Perfect para ações recentes
                → "I've just finished." / "Have you eaten yet?"
                
                🇺🇸 Americano: Frequentemente usa Past Simple em vez de Present Perfect
                → "I just finished." / "Did you eat yet?"
                
                Ambas as formas são corretas, mas refletem preferências regionais.
            """.trimIndent()
        ),

        // ── Present Perfect Continuous ────────────────────────────────────
        "present perfect continuous" to GrammarTheory(
            title = "Present Perfect Continuous",
            explanation = "Enfatiza a duração de uma ação que começou no passado e continua ou acabou recentemente.",
            structure = """
                (+) Subject + have/has + been + verb-ing
                (-) Subject + have/has + not + been + verb-ing
                (?) How long + have/has + subject + been + verb-ing?
            """.trimIndent(),
            examples = listOf(
                "The container has been running for 48 hours.",
                "The team has been dealing with tech debt for months.",
                "How long have you been working here?",
                "She hasn't been sleeping well since the incident."
            ),
            tip = "Contrações: I've been, he's been, they've been."
        ),

        // ── Past Simple ───────────────────────────────────────────────────
        "past simple" to GrammarTheory(
            title = "Past Simple (Passado Simples)",
            explanation = "Usado para ações completadas em um momento específico do passado.",
            structure = """
                (+) Subject + verb-ed (ou irregular)
                (-) Subject + did + not + base verb
                (?) Did + subject + base verb?
            """.trimIndent(),
            examples = listOf(
                "The server crashed last night.",
                "We deployed the fix on Friday.",
                "Why did the pipeline fail?",
                "The attacker gained access through SQL injection."
            ),
            tip = "Verbos irregulares comuns: go→went, find→found, break→broke, write→wrote.",
            britishVsAmerican = """
                🇬🇧 Britânico: Alguns particípios terminam em -t
                → learnt, dreamt, burnt, spelt, leapt
                
                🇺🇸 Americano: Usa formas regulares -ed
                → learned, dreamed, burned, spelled, leaped
                
                🇬🇧 "got" como particípio: "I've got a new laptop."
                🇺🇸 "gotten" como particípio: "I've gotten better at coding."
            """.trimIndent()
        ),

        // ── Passive Voice ─────────────────────────────────────────────────
        "passive voice" to GrammarTheory(
            title = "Voz Passiva (Passive Voice)",
            explanation = "Usada quando o foco é na ação ou no objeto, não em quem a pratica. Muito comum em inglês técnico e formal.",
            structure = """
                Presente:  Subject + am/is/are + past participle
                Passado:   Subject + was/were + past participle
                Futuro:    Subject + will be + past participle
                Perf.:     Subject + have/has been + past participle
            """.trimIndent(),
            examples = listOf(
                "The ticket was assigned to the infra team.",
                "The vulnerability was found during the pentest.",
                "The contract has been signed.",
                "The update will be released next month."
            ),
            tip = "Para perguntar: When was it...? / Has it been...? / Will it be...?"
        ),

        // ── Modal Verbs ───────────────────────────────────────────────────
        "modal verbs" to GrammarTheory(
            title = "Verbos Modais (Modal Verbs)",
            explanation = "Expressam possibilidade, permissão, obrigação ou recomendação. Nunca conjugados — sempre seguidos do verbo base.",
            structure = """
                Subject + modal + base verb
                can / could / may / might / will / would
                shall / should / must / need to
            """.trimIndent(),
            examples = listOf(
                "You should restart the server.",
                "Can you reset my password?",
                "We must back up the data daily.",
                "You need to uninstall the old driver first.",
                "All endpoints should be authenticated."
            ),
            tip = "Should = recomendação | Must = obrigação forte | Can = capacidade/permissão.",
            britishVsAmerican = """
                🇬🇧 Britânico: "Shall" é comum para ofertas e sugestões
                → "Shall I open the window?" / "Shall we begin?"
                
                🇺🇸 Americano: "Shall" é raro e formal; prefere "Should" ou "Will"
                → "Should I open the window?" / "Do you want to begin?"
                
                🇬🇧 "Needn't" (need not) é usado
                🇺🇸 "Don't need to" é mais comum
            """.trimIndent()
        ),

        // ── Modal Perfect ─────────────────────────────────────────────────
        "modal perfect" to GrammarTheory(
            title = "Modal Perfect (Passado Hipotético)",
            explanation = "Expressa situações hipotéticas ou arrependimentos sobre o passado. Combina modal + have + particípio.",
            structure = """
                Subject + modal + have + past participle
                should have / could have / would have / must have
            """.trimIndent(),
            examples = listOf(
                "We should have completed the epic by Q3.",
                "You could have used a more efficient algorithm.",
                "The bug must have been introduced in the last commit.",
                "I would have caught it if we had proper monitoring."
            ),
            tip = "Contrações: should've, could've, would've (muito usadas na fala)."
        ),

        // ── Third Conditional ─────────────────────────────────────────────
        "third conditional" to GrammarTheory(
            title = "Third Conditional (3º Condicional)",
            explanation = "Usado para situações hipotéticas no passado — algo que não aconteceu e suas consequências imaginárias.",
            structure = """
                If + subject + had + past participle,
                subject + would/could/might + have + past participle
            """.trimIndent(),
            examples = listOf(
                "If we had implemented monitoring earlier, we would have detected it sooner.",
                "If the tests had passed, the deploy would have succeeded.",
                "If I had reviewed the PR, I could have caught the bug.",
                "If she had been notified, she would have fixed it."
            ),
            tip = "Contrações comuns: If we'd implemented... / we would've detected..."
        ),

        // ── Tag Questions ─────────────────────────────────────────────────
        "tag questions" to GrammarTheory(
            title = "Tag Questions (Perguntas de Confirmação)",
            explanation = "Adicionadas ao final de uma frase para confirmar ou buscar concordância. Frase positiva → tag negativa, e vice-versa.",
            structure = """
                Frase positiva + , + auxiliar negativo + pronome?
                Frase negativa + , + auxiliar positivo + pronome?
            """.trimIndent(),
            examples = listOf(
                "It's a lovely city, isn't it?",
                "You've deployed the fix, haven't you?",
                "The server is running, isn't it?",
                "They don't use Kubernetes, do they?"
            ),
            tip = "Negações: isn't, aren't, doesn't, don't, didn't, won't, haven't, hasn't.",
            britishVsAmerican = """
                🇬🇧 Britânico: Tag questions são MUITO comuns no dia a dia
                → "Nice day, isn't it?" / "You're coming, aren't you?"
                
                🇺🇸 Americano: Menos frequentes; muitas vezes substituídas por "right?"
                → "Nice day, right?" / "You're coming, right?"
                
                🇬🇧 "...have you?" / "...haven't you?" (com Present Perfect)
                🇺🇸 "...did you?" / "...didn't you?" (com Past Simple)
            """.trimIndent()
        ),

        // ── Comparatives ─────────────────────────────────────────────────
        "comparatives" to GrammarTheory(
            title = "Comparativos (Comparatives)",
            explanation = "Usados para comparar duas coisas. Adjetivos curtos: + -er. Adjetivos longos: more + adjetivo.",
            structure = """
                Curtos (1-2 sílabas): adj + -er + than
                Longos (3+ sílabas):  more + adj + than
                Irregular: good→better, bad→worse, far→farther
            """.trimIndent(),
            examples = listOf(
                "The SSD is faster than the HDD.",
                "This framework is more efficient than the old one.",
                "Microservices are harder to debug than monoliths.",
                "The new API is better than the previous version."
            ),
            tip = "Which + comparative: Which is faster, A or B?"
        ),

        // ── Concession Clauses ────────────────────────────────────────────
        "concession" to GrammarTheory(
            title = "Orações de Concessão (Concession Clauses)",
            explanation = "Introduzem um contraste ou ideia oposta. Equivalem ao 'embora' e 'apesar de' do português.",
            structure = """
                While / Although / Even though + clause, + main clause
                Despite / In spite of + noun/gerund, + main clause
            """.trimIndent(),
            examples = listOf(
                "While scaling horizontally improves throughput, it doesn't reduce latency.",
                "Although the test passed locally, it failed in CI.",
                "Even though the code looks clean, the performance is poor.",
                "Despite the fix, the bug kept appearing."
            ),
            tip = "While e although são intercambiáveis no início da frase."
        ),

        // ── Short Answers ─────────────────────────────────────────────────
        "short answers" to GrammarTheory(
            title = "Respostas Curtas (Short Answers)",
            explanation = "No inglês, respondemos Yes/No repetindo o auxiliar da pergunta — nunca apenas 'Yes' ou 'No' sozinhos em contexto formal.",
            structure = """
                Yes, + subject + auxiliary.
                No, + subject + auxiliary + not. (ou contração)
            """.trimIndent(),
            examples = listOf(
                "Do you work here? → Yes, I do. / No, I don't.",
                "Has it been deployed? → Yes, it has. / No, it hasn't.",
                "Did the rollback work? → Yes, it did. / No, it didn't.",
                "Are you the team lead? → Yes, I am. / No, I'm not."
            ),
            tip = "Nunca repita o verbo principal: 'Yes, I do.' ✓  'Yes, I work.' ✗"
        ),

        // ── Future Passive ────────────────────────────────────────────────
        "future passive" to GrammarTheory(
            title = "Futuro Passivo (Future Passive)",
            explanation = "Combina o futuro com a voz passiva. Usado quando não se sabe ou não importa quem vai executar a ação.",
            structure = """
                (+) Subject + will be + past participle
                (-) Subject + will not (won't) be + past participle
                (?) Will + subject + be + past participle?
            """.trimIndent(),
            examples = listOf(
                "The instance will be scaled up during peak hours.",
                "The update will be released next Friday.",
                "When will the feature be deployed?",
                "The database won't be migrated until the tests pass."
            ),
            tip = "Contração: won't be = will not be."
        )
    )
}
