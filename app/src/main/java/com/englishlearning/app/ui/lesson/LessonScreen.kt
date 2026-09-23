package com.englishlearning.app.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.englishlearning.app.domain.lesson.LessonContent
import com.englishlearning.app.domain.model.QuestionType

// ---------------------------------------------------------------------------
// LessonScreen — Ponto de entrada
// ---------------------------------------------------------------------------

/**
 * Tela de Mini-Lição Gramatical (LessonScreen).
 *
 * Exibe uma lição rápida sobre o tipo de pergunta que o usuário praticará na
 * sessão seguinte. A lição inclui título, explicação da regra gramatical em
 * português, fórmula estrutural e exemplos em inglês.
 *
 * Fluxo de navegação (Requisitos 15.4–15.8, 15.12):
 * - Se a categoria possui um QuestionType mapeado em [LessonContentProvider],
 *   a tela exibe a lição completa.
 * - Botão "Começar" (Req. 15.5): registra lição como vista e chama [onStartLesson].
 * - Botão "Pular" (Req. 15.6): registra lição como vista e chama [onSkipLesson].
 * - Se questionType não possui mapeamento (Req. 15.12), navega diretamente via
 *   [onStartLesson] sem exibir conteúdo.
 *
 * Textos de interface e explicações em português brasileiro (Req. 15.11).
 * Fórmulas estruturais e exemplos mantidos em inglês (Req. 15.11).
 *
 * @param levelId       ID do nível atual (repassado para a navegação de volta).
 * @param categoryId    ID da categoria selecionada.
 * @param questionType  String do QuestionType da categoria (ex: "WH_WHAT").
 * @param onStartLesson Callback ao pressionar "Começar" ou quando não há lição mapeada.
 * @param onSkipLesson  Callback ao pressionar "Pular".
 * @param viewModel     ViewModel injetado via Hilt.
 */
@Composable
fun LessonScreen(
    levelId: Int,
    categoryId: Int,
    questionType: String,
    onStartLesson: () -> Unit,
    onSkipLesson: () -> Unit,
    viewModel: LessonViewModel = hiltViewModel()
) {
    // Converte a string do questionType recebido da rota para enum, com fallback seguro
    val parsedQuestionType = runCatching {
        QuestionType.valueOf(questionType)
    }.getOrNull()

    // Carrega a lição ao entrar na tela
    LaunchedEffect(categoryId, parsedQuestionType) {
        if (parsedQuestionType != null) {
            viewModel.loadLesson(categoryId, parsedQuestionType)
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Req. 15.12: se não há QuestionType mapeado ou o tipo é inválido, pular diretamente
    if (parsedQuestionType == null || uiState.lessonContent == null) {
        LaunchedEffect(Unit) {
            onStartLesson()
        }
        return
    }

    LessonScaffold(
        lessonContent = uiState.lessonContent!!,
        onStartLesson = {
            // Req. 15.5 / 15.7: registrar como vista antes de navegar
            viewModel.markLessonSeen(categoryId)
            onStartLesson()
        },
        onSkipLesson = {
            // Req. 15.6 / 15.7: registrar como vista antes de navegar
            viewModel.markLessonSeen(categoryId)
            onSkipLesson()
        }
    )
}

// ---------------------------------------------------------------------------
// LessonScaffold — Layout principal
// ---------------------------------------------------------------------------

/**
 * Scaffold da LessonScreen com TopAppBar, conteúdo scrollável e botões de ação.
 *
 * @param lessonContent  Conteúdo da lição a ser exibido.
 * @param onStartLesson  Callback ao pressionar "Começar".
 * @param onSkipLesson   Callback ao pressionar "Pular".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScaffold(
    lessonContent: LessonContent,
    onStartLesson: () -> Unit,
    onSkipLesson: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mini-Lição",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Req. 15.5 / 15.6: botões "Começar" e "Pular" fixos na parte inferior
            LessonActionBar(
                onStartLesson = onStartLesson,
                onSkipLesson = onSkipLesson
            )
        }
    ) { innerPadding ->
        LessonContent(
            lessonContent = lessonContent,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// ---------------------------------------------------------------------------
// LessonContent — Conteúdo scrollável da lição
// ---------------------------------------------------------------------------

/**
 * Conteúdo principal da LessonScreen: título, explicação gramatical,
 * fórmula estrutural e exemplos.
 *
 * Requisito 15.4: exibir título, regra gramatical, fórmula estrutural e exemplos.
 * Requisito 15.11: interface em português; fórmulas e exemplos em inglês.
 *
 * @param lessonContent  Conteúdo da lição.
 * @param modifier       Modifier de layout.
 */
@Composable
fun LessonContent(
    lessonContent: LessonContent,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Título da lição em português (Req. 15.11)
        LessonTitleSection(title = lessonContent.titlePt)

        // Explicação da regra gramatical em português (Req. 15.4, 15.11)
        LessonRuleSection(ruleExplanation = lessonContent.ruleExplanationPt)

        // Fórmula estrutural em inglês (Req. 15.4, 15.11)
        LessonFormulaSection(formula = lessonContent.structuralFormula)

        // Exemplos de perguntas em inglês (Req. 15.4, 15.11)
        LessonExamplesSection(examples = lessonContent.examples)

        // Espaçamento extra antes dos botões da barra inferior
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ---------------------------------------------------------------------------
// LessonTitleSection — Título e ícone da lição
// ---------------------------------------------------------------------------

/**
 * Seção de título da lição com ícone decorativo de livro.
 *
 * @param title  Título da lição em português.
 */
@Composable
fun LessonTitleSection(title: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "📖",
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Aprenda antes de praticar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// LessonRuleSection — Explicação da regra gramatical (em português)
// ---------------------------------------------------------------------------

/**
 * Card com a explicação da regra gramatical em português brasileiro.
 *
 * Requisito 15.4: exibir a explicação da regra gramatical.
 * Requisito 15.11: explicações em português.
 *
 * @param ruleExplanation  Texto explicativo da regra em português.
 */
@Composable
fun LessonRuleSection(ruleExplanation: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Rótulo em português (Req. 15.11)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "📌", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Regra Gramatical",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
            Text(
                text = ruleExplanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
        }
    }
}

// ---------------------------------------------------------------------------
// LessonFormulaSection — Fórmula estrutural (em inglês)
// ---------------------------------------------------------------------------

/**
 * Card com a fórmula estrutural da pergunta em inglês.
 *
 * Requisito 15.4: exibir a fórmula estrutural.
 * Requisito 15.11: fórmula estrutural em inglês.
 *
 * @param formula  Fórmula estrutural em inglês (ex: "What + auxiliary + subject + verb?").
 */
@Composable
fun LessonFormulaSection(formula: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Rótulo em português (Req. 15.11)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🔧", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Estrutura",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            // Fórmula em inglês (Req. 15.11)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = formula,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// LessonExamplesSection — Exemplos em inglês
// ---------------------------------------------------------------------------

/**
 * Card com exemplos de perguntas em inglês correspondentes ao tipo gramatical.
 *
 * Requisito 15.4: exibir exemplos de perguntas.
 * Requisito 15.11: exemplos em inglês.
 *
 * @param examples  Lista de exemplos de perguntas em inglês.
 */
@Composable
fun LessonExamplesSection(examples: List<String>) {
    if (examples.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Rótulo em português (Req. 15.11)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "💡", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Exemplos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            // Exemplos em inglês, numerados (Req. 15.11)
            examples.forEachIndexed { index, example ->
                LessonExampleItem(
                    index = index + 1,
                    example = example
                )
            }
        }
    }
}

/**
 * Item de exemplo individual com número de ordem e texto em inglês.
 *
 * @param index    Número de ordem (1-based).
 * @param example  Texto do exemplo em inglês.
 */
@Composable
fun LessonExampleItem(
    index: Int,
    example: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Número do exemplo em destaque
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        // Exemplo em inglês com estilo itálico para diferenciar do texto de interface (Req. 15.11)
        Text(
            text = example,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------------------------------------------------------------------------
// LessonActionBar — Botões "Começar" e "Pular"
// ---------------------------------------------------------------------------

/**
 * Barra de ações fixada na parte inferior da tela com os botões "Começar" e "Pular".
 *
 * Requisito 15.5: botão "Começar" registra lição como vista e navega para a sessão.
 * Requisito 15.6: botão "Pular" registra lição como vista e navega para a sessão.
 * Requisito 15.11: textos de interface em português.
 *
 * @param onStartLesson  Callback ao pressionar "Começar".
 * @param onSkipLesson   Callback ao pressionar "Pular".
 */
@Composable
fun LessonActionBar(
    onStartLesson: () -> Unit,
    onSkipLesson: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Botão principal "Começar" (Req. 15.5)
        Button(
            onClick = onStartLesson,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Começar",
                style = MaterialTheme.typography.labelLarge
            )
        }

        // Botão secundário "Pular" (Req. 15.6)
        OutlinedButton(
            onClick = onSkipLesson,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Pular Lição",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun LessonScaffoldPreview() {
    MaterialTheme {
        LessonScaffold(
            lessonContent = LessonContent.WhatLesson,
            onStartLesson = {},
            onSkipLesson = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LessonFormulaPreview() {
    MaterialTheme {
        LessonFormulaSection(formula = "What + auxiliary verb + subject + main verb?")
    }
}

@Preview(showBackground = true)
@Composable
private fun LessonExamplesSectionPreview() {
    MaterialTheme {
        LessonExamplesSection(
            examples = listOf(
                "What is your name?",
                "What do you do for work?"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LessonActionBarPreview() {
    MaterialTheme {
        LessonActionBar(
            onStartLesson = {},
            onSkipLesson = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LessonTagPreview() {
    MaterialTheme {
        LessonScaffold(
            lessonContent = LessonContent.TagLesson,
            onStartLesson = {},
            onSkipLesson = {}
        )
    }
}
