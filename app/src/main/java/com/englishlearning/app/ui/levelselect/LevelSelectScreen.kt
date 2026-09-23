package com.englishlearning.app.ui.levelselect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.englishlearning.app.domain.model.Level

// ---------------------------------------------------------------------------
// LevelSelectScreen — Ponto de entrada
// ---------------------------------------------------------------------------

/**
 * Tela de seleção de nível de dificuldade.
 *
 * Coleta [LevelSelectViewModel.uiState] via [collectAsStateWithLifecycle] e
 * delega a renderização conforme o estado atual (carregando, lista, erro).
 *
 * Requisito 6.3: exibir quais níveis estão desbloqueados, em progresso e bloqueados.
 * Requisito 6.5: exibir a taxa de progresso por nível.
 *
 * @param onNavigateToCategorySelect  Callback invocado ao selecionar um nível desbloqueado.
 * @param onNavigateBack              Callback ao pressionar o botão Voltar.
 * @param viewModel                   ViewModel injetado via Hilt.
 */
@Composable
fun LevelSelectScreen(
    onNavigateToCategorySelect: (levelId: Int) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    viewModel: LevelSelectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LevelSelectScaffold(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onLevelSelected = onNavigateToCategorySelect,
        onRetry = viewModel::loadLevels
    )
}

// ---------------------------------------------------------------------------
// LevelSelectScaffold — Layout com Scaffold
// ---------------------------------------------------------------------------

/**
 * Scaffold da tela de seleção de nível com TopAppBar e conteúdo condicional.
 *
 * @param uiState           Estado atual da tela.
 * @param onNavigateBack    Callback ao pressionar o botão Voltar.
 * @param onLevelSelected   Callback ao pressionar um card de nível desbloqueado.
 * @param onRetry           Callback ao pressionar Tentar Novamente em caso de erro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectScaffold(
    uiState: LevelSelectUiState,
    onNavigateBack: () -> Unit,
    onLevelSelected: (levelId: Int) -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Selecionar Nível",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
        when {
            uiState.isLoading -> LevelSelectLoadingIndicator(
                modifier = Modifier.padding(innerPadding)
            )
            uiState.error != null -> LevelSelectErrorScreen(
                message = uiState.error,
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding)
            )
            else -> LevelSelectContent(
                levels = uiState.levels,
                onLevelSelected = onLevelSelected,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// LevelSelectContent — Lista de níveis
// ---------------------------------------------------------------------------

/**
 * Exibe a lista de níveis disponíveis como cards verticais.
 *
 * Requisito 6.3: cada card indica o status do nível (desbloqueado/em progresso/bloqueado).
 * Requisito 6.5: cada card exibe a taxa de progresso com LinearProgressIndicator.
 *
 * @param levels          Lista de [LevelWithProgress] carregada pelo ViewModel.
 * @param onLevelSelected Callback ao tocar em um nível desbloqueado.
 * @param modifier        Modifier de layout.
 */
@Composable
fun LevelSelectContent(
    levels: List<LevelWithProgress>,
    onLevelSelected: (levelId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = levels, key = { it.level.id }) { levelWithProgress ->
            LevelCard(
                levelWithProgress = levelWithProgress,
                onLevelSelected = onLevelSelected
            )
        }
    }
}

// ---------------------------------------------------------------------------
// LevelCard — Card de nível individual
// ---------------------------------------------------------------------------

/**
 * Determina o status de exibição de um nível baseado no desbloqueio e progresso.
 *
 * - LOCKED: nível bloqueado — não pode ser selecionado.
 * - UNLOCKED: nível desbloqueado, sem progresso ainda.
 * - IN_PROGRESS: nível desbloqueado com progresso parcial (> 0% e < 100%).
 * - COMPLETED: nível com 100% de progresso.
 */
private enum class LevelStatus { LOCKED, UNLOCKED, IN_PROGRESS, COMPLETED }

private fun LevelWithProgress.resolveStatus(): LevelStatus = when {
    !level.isUnlocked -> LevelStatus.LOCKED
    progressPercent >= 1.0f -> LevelStatus.COMPLETED
    progressPercent > 0f -> LevelStatus.IN_PROGRESS
    else -> LevelStatus.UNLOCKED
}

/**
 * Card de nível exibindo nome (em inglês — Req. 12.5), status, barra de progresso e
 * contagem de exercícios concluídos.
 *
 * Requisito 6.3: indicador visual de desbloqueado/em progresso/bloqueado.
 * Requisito 6.5: barra de progresso com percentual de exercícios concluídos.
 *
 * @param levelWithProgress Dados do nível com progresso calculado.
 * @param onLevelSelected   Callback ao tocar no card (ignorado quando bloqueado).
 */
@Composable
fun LevelCard(
    levelWithProgress: LevelWithProgress,
    onLevelSelected: (levelId: Int) -> Unit
) {
    val status = levelWithProgress.resolveStatus()
    val isLocked = status == LevelStatus.LOCKED

    // Cards bloqueados ficam levemente transparentes para indicar indisponibilidade
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isLocked) 0.55f else 1f),
        onClick = {
            if (!isLocked) {
                onLevelSelected(levelWithProgress.level.id)
            }
        },
        enabled = !isLocked,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLocked) 0.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                LevelStatus.LOCKED -> MaterialTheme.colorScheme.surfaceVariant
                LevelStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                LevelStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondaryContainer
                LevelStatus.UNLOCKED -> MaterialTheme.colorScheme.primaryContainer
            },
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Linha superior: ícone de status + nome do nível
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ícone de cadeado ou desbloqueio (Req. 6.3)
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (isLocked) "Nível bloqueado" else "Nível desbloqueado",
                    modifier = Modifier.size(24.dp),
                    tint = when (status) {
                        LevelStatus.LOCKED -> MaterialTheme.colorScheme.outline
                        LevelStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Nome do nível em inglês (Req. 12.5)
                    Text(
                        text = levelWithProgress.level.nameKey,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLocked) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    // Rótulo de status em português (Req. 12.4)
                    Text(
                        text = when (status) {
                            LevelStatus.LOCKED -> "Bloqueado"
                            LevelStatus.UNLOCKED -> "Disponível"
                            LevelStatus.IN_PROGRESS -> "Em progresso"
                            LevelStatus.COMPLETED -> "Concluído ✓"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (status) {
                            LevelStatus.LOCKED -> MaterialTheme.colorScheme.outline
                            LevelStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                            LevelStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondary
                            LevelStatus.UNLOCKED -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }

            // Barra de progresso — apenas quando o nível está desbloqueado (Req. 6.5)
            if (!isLocked) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { levelWithProgress.progressPercent },
                    modifier = Modifier.fillMaxWidth(),
                    color = when (status) {
                        LevelStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Contagem de exercícios concluídos (Req. 6.5)
                    Text(
                        text = "${levelWithProgress.completedExercises}/${levelWithProgress.totalExercises} exercícios",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Percentual de progresso
                    Text(
                        text = "${(levelWithProgress.progressPercent * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// LoadingIndicator — Estado de carregamento
// ---------------------------------------------------------------------------

/**
 * Indicador de carregamento centralizado na tela de seleção de nível.
 */
@Composable
fun LevelSelectLoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// ---------------------------------------------------------------------------
// ErrorScreen — Estado de erro (Req. 12.7)
// ---------------------------------------------------------------------------

/**
 * Tela de erro exibida quando o carregamento dos níveis falha.
 *
 * Requisito 12.7: mensagem de erro amigável com opção de retry.
 *
 * @param message  Mensagem de erro a ser exibida.
 * @param onRetry  Callback ao pressionar Tentar Novamente.
 * @param modifier Modifier de layout.
 */
@Composable
fun LevelSelectErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
                text = "Não foi possível carregar os níveis",
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
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun LevelSelectContentPreview() {    MaterialTheme {
        LevelSelectContent(
            levels = listOf(
                LevelWithProgress(
                    level = Level(
                        id = 1, nameKey = "Beginner", descriptionKey = "level_1_desc",
                        minScore = 0, maxScore = 500, isUnlocked = true, sortOrder = 1
                    ),
                    progressPercent = 0.65f,
                    completedExercises = 13,
                    totalExercises = 20
                ),
                LevelWithProgress(
                    level = Level(
                        id = 2, nameKey = "Elementary", descriptionKey = "level_2_desc",
                        minScore = 500, maxScore = 1500, isUnlocked = true, sortOrder = 2
                    ),
                    progressPercent = 0f,
                    completedExercises = 0,
                    totalExercises = 20
                ),
                LevelWithProgress(
                    level = Level(
                        id = 3, nameKey = "Intermediate", descriptionKey = "level_3_desc",
                        minScore = 1500, maxScore = 3000, isUnlocked = false, sortOrder = 3
                    ),
                    progressPercent = 0f,
                    completedExercises = 0,
                    totalExercises = 20
                )
            ),
            onLevelSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LevelCardLockedPreview() {
    MaterialTheme {
        LevelCard(
            levelWithProgress = LevelWithProgress(
                level = Level(
                    id = 3, nameKey = "Intermediate", descriptionKey = "level_3_desc",
                    minScore = 1500, maxScore = 3000, isUnlocked = false, sortOrder = 3
                ),
                progressPercent = 0f,
                completedExercises = 0,
                totalExercises = 20
            ),
            onLevelSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LevelSelectLoadingPreview() {
    MaterialTheme {
        LevelSelectLoadingIndicator()
    }
}
