package com.englishlearning.app.ui.srs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.englishlearning.app.domain.gamification.SrsRating
import com.englishlearning.app.domain.gamification.SrsState
import com.englishlearning.app.domain.model.SrsCard
import com.englishlearning.app.domain.model.SrsCardType
import java.time.LocalDate

// ---------------------------------------------------------------------------
// SrsReviewScreen — Ponto de entrada
// ---------------------------------------------------------------------------

/**
 * Tela de revisão por repetição espaçada (Active Recall).
 *
 * Carrega os cards devidos ao ser lançada e delega a renderização para
 * os composables de cada estado.
 *
 * Requisitos: 16.6, 16.7, 17.1–17.7, 18.4.
 *
 * @param onNavigateBack Callback para voltar à HomeScreen.
 * @param viewModel      ViewModel injetado via Hilt.
 */
@Composable
fun SrsReviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: SrsReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Carrega os cards devidos ao entrar na tela (Req. 16.7)
    LaunchedEffect(Unit) {
        viewModel.loadDueCards()
    }

    when (val state = uiState) {
        is SrsReviewUiState.Loading -> SrsLoadingScreen()

        is SrsReviewUiState.Empty -> SrsEmptyScreen(onNavigateBack = onNavigateBack)

        is SrsReviewUiState.ShowingCard -> SrsCardScreen(
            state = state,
            onReveal = viewModel::revealAnswer,
            onRate = viewModel::submitRating,
            onNavigateBack = onNavigateBack
        )

        is SrsReviewUiState.SessionComplete -> SrsSessionCompleteScreen(
            stats = state.stats,
            onNavigateBack = onNavigateBack
        )

        is SrsReviewUiState.Error -> SrsErrorScreen(
            message = state.message,
            onRetry = viewModel::retry,
            onNavigateBack = onNavigateBack
        )
    }
}

// ---------------------------------------------------------------------------
// Loading
// ---------------------------------------------------------------------------

/**
 * Estado de carregamento: indicador circular centralizado.
 */
@Composable
private fun SrsLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// ---------------------------------------------------------------------------
// Empty state — sem cards devidos (Req. 16.6)
// ---------------------------------------------------------------------------

/**
 * Estado vazio: exibido quando não há cards devidos hoje.
 *
 * Requisito 16.6: badge na HomeScreen visível apenas quando dueCardCount > 0;
 * se o usuário entrar na tela sem cards, este estado é exibido.
 */
@Composable
private fun SrsEmptyScreen(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "✅",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "Nenhuma revisão pendente",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Você está em dia com suas revisões. Volte amanhã!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Voltar")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Card Review — estado principal (Req. 17.1–17.6)
// ---------------------------------------------------------------------------

/**
 * Tela de revisão activa de um card.
 *
 * Exibe:
 * - Header com progresso "Card X de Y" e botão voltar (Req. 17.6).
 * - Barra de progresso linear (Req. 17.6).
 * - Área do estímulo (visível sempre, Req. 17.1).
 * - Botão "Revelar Resposta" antes da revelação (Req. 17.2).
 * - Resposta + botões de avaliação após a revelação (Req. 17.2, 17.3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SrsCardScreen(
    state: SrsReviewUiState.ShowingCard,
    onReveal: () -> Unit,
    onRate: (SrsRating) -> Unit,
    onNavigateBack: () -> Unit
) {
    val progress = state.progress
    val progressFraction = if (progress.totalCards > 0)
        (progress.currentIndex.toFloat() / progress.totalCards.toFloat())
    else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Card ${progress.currentIndex + 1} de ${progress.totalCards}",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Barra de progresso linear (Req. 17.6)
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Área do estímulo — sempre visível (Req. 17.1)
                StimulusCard(stimulus = state.content.stimulus)

                AnimatedVisibility(
                    visible = !state.isRevealed,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    // Botão "Revelar Resposta" — pré-revelação (Req. 17.2)
                    Button(
                        onClick = onReveal,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Revelar Resposta",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                AnimatedVisibility(
                    visible = state.isRevealed,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Área de resposta revelada (Req. 17.2)
                        AnswerCard(content = state.content)

                        HorizontalDivider()

                        // Botões de avaliação (Req. 17.3)
                        RatingButtons(onRate = onRate)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// StimulusCard — estímulo do card (Req. 17.1)
// ---------------------------------------------------------------------------

/**
 * Card que exibe o estímulo do exercício — visível antes e após a revelação.
 *
 * Para GRAMMAR: texto da resposta que o usuário deve formular a pergunta.
 * Para VOCABULARY: a palavra em inglês que o usuário deve lembrar.
 *
 * Requisito 17.1: exibir apenas o estímulo sem revelar a resposta esperada.
 */
@Composable
private fun StimulusCard(stimulus: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Qual é a pergunta?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Text(
                text = stimulus,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
// AnswerCard — resposta revelada (Req. 17.2)
// ---------------------------------------------------------------------------

/**
 * Card que exibe a resposta esperada após o usuário pressionar "Revelar".
 *
 * Para GRAMMAR: pergunta aceita principal.
 * Para VOCABULARY: definição em PT-BR + sentença de exemplo (detail) em inglês.
 *
 * Requisito 17.2: botão "Revelar" exibe a resposta esperada.
 */
@Composable
private fun AnswerCard(content: ReviewCardContent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Resposta",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = content.answer,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            // Detalhe opcional (sentença de exemplo para cards de vocabulário)
            content.detail?.let { detail ->
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// RatingButtons — avaliação do usuário (Req. 17.3, 17.4, 17.5)
// ---------------------------------------------------------------------------

/**
 * Linha com os 4 botões de avaliação: AGAIN, HARD, GOOD, EASY.
 *
 * Cores seguem convenção:
 * - AGAIN: vermelho (errou, volta em breve — Req. 17.4)
 * - HARD:  laranja (difícil)
 * - GOOD:  verde  (acertou — Req. 17.5)
 * - EASY:  azul   (fácil, intervalo longo — Req. 17.5)
 *
 * Requisito 17.3: exibir 4 botões AGAIN / HARD / GOOD / EASY após revelação.
 */
@Composable
private fun RatingButtons(onRate: (SrsRating) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Como foi?",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // AGAIN — esqueceu, intervalo = 1 dia (Req. 17.4)
            RatingButton(
                label = "Errei",
                sublabel = "Amanhã",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                onClick = { onRate(SrsRating.AGAIN) },
                modifier = Modifier.weight(1f)
            )
            // HARD — difícil
            RatingButton(
                label = "Difícil",
                sublabel = "Breve",
                containerColor = Color(0xFFFFE0B2), // orange-100
                contentColor = Color(0xFF4E2600),
                onClick = { onRate(SrsRating.HARD) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // GOOD — acertou (Req. 17.5)
            RatingButton(
                label = "Acertei",
                sublabel = "Dias",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = { onRate(SrsRating.GOOD) },
                modifier = Modifier.weight(1f)
            )
            // EASY — muito fácil, intervalo longo (Req. 17.5)
            RatingButton(
                label = "Fácil",
                sublabel = "Semanas",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { onRate(SrsRating.EASY) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Botão de avaliação individual com rótulo principal e sub-rótulo de intervalo estimado.
 */
@Composable
private fun RatingButton(
    label: String,
    sublabel: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ---------------------------------------------------------------------------
// SessionComplete — tela de conclusão (Req. 17.7)
// ---------------------------------------------------------------------------

/**
 * Tela de conclusão exibida quando todos os cards devidos são revisados.
 *
 * Exibe:
 * - Total de cards revisados.
 * - Distribuição de ratings: Fáceis, Acertos, Difíceis e Erros.
 * - Botão "Voltar ao Início".
 *
 * Requisito 17.7: tela de conclusão com total revisado e distribuição de ratings.
 */
@Composable
private fun SrsSessionCompleteScreen(
    stats: ReviewSessionStats,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Revisão Concluída!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "${stats.totalReviewed} card${if (stats.totalReviewed != 1) "s" else ""} revisado${if (stats.totalReviewed != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Distribuição de ratings
            if (stats.totalReviewed > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Distribuição",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            RatingStat(
                                emoji = "😊",
                                label = "Fáceis",
                                count = stats.easyCount,
                                color = MaterialTheme.colorScheme.primary
                            )
                            RatingStat(
                                emoji = "✅",
                                label = "Acertos",
                                count = stats.goodCount,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            RatingStat(
                                emoji = "😰",
                                label = "Difíceis",
                                count = stats.hardCount,
                                color = Color(0xFFE65100) // tertiary orange
                            )
                            RatingStat(
                                emoji = "❌",
                                label = "Erros",
                                count = stats.againCount,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Voltar ao Início",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Item de estatística de rating individual para a tela de conclusão.
 */
@Composable
private fun RatingStat(
    emoji: String,
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// Error state (Req. 12.7)
// ---------------------------------------------------------------------------

/**
 * Estado de erro: mensagem amigável com opção de retry.
 *
 * Requisito 12.7: exibir mensagem de erro amigável com opção de retry.
 */
@Composable
private fun SrsErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "😕",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "Não foi possível carregar as revisões",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Tentar Novamente")
            }
            TextButton(onClick = onNavigateBack) {
                Text(text = "Voltar")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "Loading")
@Composable
private fun SrsLoadingPreview() {
    MaterialTheme { SrsLoadingScreen() }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun SrsEmptyPreview() {
    MaterialTheme { SrsEmptyScreen(onNavigateBack = {}) }
}

@Preview(showBackground = true, name = "Card — Before Reveal")
@Composable
private fun SrsCardBeforeRevealPreview() {
    MaterialTheme {
        SrsCardScreen(
            state = SrsReviewUiState.ShowingCard(
                card = SrsCard(
                    id = 1L,
                    userId = 1L,
                    cardType = SrsCardType.GRAMMAR,
                    refId = 1L,
                    stability = 2.4f,
                    difficulty = 5f,
                    elapsedDays = 2,
                    scheduledDays = 2,
                    reps = 1,
                    lapses = 0,
                    state = SrsState.REVIEW,
                    dueDate = LocalDate.now(),
                    lastReviewDate = null
                ),
                content = ReviewCardContent(
                    stimulus = "My name is Maria.",
                    answer = "What is your name?"
                ),
                isRevealed = false,
                progress = ReviewProgress(currentIndex = 0, totalCards = 5)
            ),
            onReveal = {},
            onRate = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Card — After Reveal")
@Composable
private fun SrsCardAfterRevealPreview() {
    MaterialTheme {
        SrsCardScreen(
            state = SrsReviewUiState.ShowingCard(
                card = SrsCard(
                    id = 1L,
                    userId = 1L,
                    cardType = SrsCardType.VOCABULARY,
                    refId = 2L,
                    stability = 5.8f,
                    difficulty = 4f,
                    elapsedDays = 5,
                    scheduledDays = 5,
                    reps = 2,
                    lapses = 0,
                    state = SrsState.REVIEW,
                    dueDate = LocalDate.now(),
                    lastReviewDate = null
                ),
                content = ReviewCardContent(
                    stimulus = "schedule",
                    answer = "agendar, programar",
                    detail = "I need to schedule a meeting for tomorrow."
                ),
                isRevealed = true,
                progress = ReviewProgress(currentIndex = 2, totalCards = 5)
            ),
            onReveal = {},
            onRate = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Session Complete")
@Composable
private fun SrsSessionCompletePreview() {
    MaterialTheme {
        SrsSessionCompleteScreen(
            stats = ReviewSessionStats(
                totalReviewed = 10,
                againCount = 2,
                hardCount = 1,
                goodCount = 5,
                easyCount = 2
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun SrsErrorPreview() {
    MaterialTheme {
        SrsErrorScreen(
            message = "Erro ao carregar revisões. Tente novamente.",
            onRetry = {},
            onNavigateBack = {}
        )
    }
}
