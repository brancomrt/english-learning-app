package com.englishlearning.app.ui.errorreview

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.englishlearning.app.data.database.entity.AttemptEntity
import com.englishlearning.app.domain.model.AcceptedAnswer
import com.englishlearning.app.domain.model.Exercise
import com.englishlearning.app.domain.model.QuestionType

// ---------------------------------------------------------------------------
// ErrorReviewScreen — Ponto de entrada
// ---------------------------------------------------------------------------

/**
 * Tela de revisão de erros.
 *
 * Exibe exercícios que o usuário errou anteriormente, permitindo
 * que ele pratique novamente esses exercícios específicos.
 *
 * @param onNavigateBack Callback para voltar à HomeScreen.
 * @param onStartExercise Callback para iniciar um exercício (navega para LearningSession).
 * @param viewModel ViewModel injetado via Hilt.
 */
@Composable
fun ErrorReviewScreen(
    onNavigateBack: () -> Unit,
    onStartExercise: (levelId: Int, categoryId: Int) -> Unit,
    viewModel: ErrorReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Carrega os exercícios com erros ao entrar na tela
    LaunchedEffect(Unit) {
        viewModel.loadErrorExercises()
    }

    when (val state = uiState) {
        is ErrorReviewUiState.Loading -> ErrorReviewLoadingScreen()

        is ErrorReviewUiState.Empty -> ErrorReviewEmptyScreen(onNavigateBack = onNavigateBack)

        is ErrorReviewUiState.Reviewing -> ErrorReviewContentScreen(
            state = state,
            onStartExercise = onStartExercise,
            onSkip = viewModel::skipExercise,
            onNavigateBack = onNavigateBack
        )

        is ErrorReviewUiState.SessionComplete -> ErrorReviewCompleteScreen(
            totalReviewed = state.totalReviewed,
            correctCount = state.correctCount,
            incorrectCount = state.incorrectCount,
            onNavigateBack = onNavigateBack
        )

        is ErrorReviewUiState.Error -> ErrorReviewErrorScreen(
            message = state.message,
            onRetry = viewModel::retry,
            onNavigateBack = onNavigateBack
        )
    }
}

// ---------------------------------------------------------------------------
// Loading
// ---------------------------------------------------------------------------

@Composable
private fun ErrorReviewLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

@Composable
private fun ErrorReviewEmptyScreen(onNavigateBack: () -> Unit) {
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
                text = "🎉",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "Nenhum erro pendente!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Você não tem exercícios errados para revisar.\nContinue praticando!",
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
// Content — Lista de exercícios com erros
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorReviewContentScreen(
    state: ErrorReviewUiState.Reviewing,
    onStartExercise: (levelId: Int, categoryId: Int) -> Unit,
    onSkip: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val progressFraction = if (state.totalCount > 0)
        (state.currentIndex.toFloat() / state.totalCount.toFloat())
    else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Revisão ${state.currentIndex + 1} de ${state.totalCount}",
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
            // Barra de progresso
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card do exercício atual
                ErrorExerciseCard(
                    item = state.currentItem,
                    onStartExercise = onStartExercise
                )

                // Última resposta do usuário (se disponível)
                state.currentItem.lastAttempt?.let { attempt ->
                    LastAttemptCard(attempt = attempt)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botões de ação
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Pular")
                    }
                    Button(
                        onClick = {
                            onStartExercise(
                                state.currentItem.exercise.levelId,
                                state.currentItem.exercise.categoryId
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Praticar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Error Exercise Card
// ---------------------------------------------------------------------------

@Composable
private fun ErrorExerciseCard(
    item: ErrorReviewItem,
    onStartExercise: (levelId: Int, categoryId: Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header com ícone de erro
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Exercício com erro",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f)
            )

            // Tipo de exercício
            Text(
                text = "Tipo: ${item.exercise.questionType.displayName()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Resposta (estímulo do exercício)
            Text(
                text = "Resposta:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = item.exercise.answerText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Pergunta esperada
            item.exercise.acceptedAnswers.firstOrNull { it.isPrimary }?.let { primaryAnswer ->
                Text(
                    text = "Pergunta esperada:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = primaryAnswer.questionText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Last Attempt Card
// ---------------------------------------------------------------------------

@Composable
private fun LastAttemptCard(attempt: AttemptEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Sua última tentativa:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "\"${attempt.userAnswer}\"",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Session Complete
// ---------------------------------------------------------------------------

@Composable
private fun ErrorReviewCompleteScreen(
    totalReviewed: Int,
    correctCount: Int,
    incorrectCount: Int,
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
                imageVector = Icons.Filled.CheckCircle,
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
                text = "$totalReviewed exercício${if (totalReviewed != 1) "s" else ""} revisado${if (totalReviewed != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Estatísticas
            if (totalReviewed > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            emoji = "✅",
                            label = "Acertos",
                            count = correctCount,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        StatItem(
                            emoji = "❌",
                            label = "Erros",
                            count = incorrectCount,
                            color = MaterialTheme.colorScheme.error
                        )
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

@Composable
private fun StatItem(
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
// Error state
// ---------------------------------------------------------------------------

@Composable
private fun ErrorReviewErrorScreen(
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
                text = "Não foi possível carregar os exercícios",
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
                    imageVector = Icons.Filled.Refresh,
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
private fun ErrorReviewLoadingPreview() {
    MaterialTheme { ErrorReviewLoadingScreen() }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun ErrorReviewEmptyPreview() {
    MaterialTheme { ErrorReviewEmptyScreen(onNavigateBack = {}) }
}

@Preview(showBackground = true, name = "Session Complete")
@Composable
private fun ErrorReviewCompletePreview() {
    MaterialTheme {
        ErrorReviewCompleteScreen(
            totalReviewed = 5,
            correctCount = 3,
            incorrectCount = 2,
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun ErrorReviewErrorPreview() {
    MaterialTheme {
        ErrorReviewErrorScreen(
            message = "Erro ao carregar exercícios.",
            onRetry = {},
            onNavigateBack = {}
        )
    }
}
