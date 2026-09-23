package com.englishlearning.app.ui.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.englishlearning.app.domain.model.Level
import com.englishlearning.app.domain.model.UserStats

// ---------------------------------------------------------------------------
// Navigation route constants
// ---------------------------------------------------------------------------

/** Rotas de navegação usadas pela HomeScreen. */
object HomeNavRoutes {
    const val LEVEL_SELECT = "level_select"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"

    /** Rota para a tela de revisão SRS. Requisito 16.6, 18.4. */
    const val SRS_REVIEW = "srs_review"
    const val VOCABULARY = "vocabulary"
}

// ---------------------------------------------------------------------------
// HomeScreen — Ponto de entrada
// ---------------------------------------------------------------------------

/**
 * Tela inicial do aplicativo (dashboard).
 *
 * Coleta [HomeViewModel.uiState] via [collectAsStateWithLifecycle] e delega
 * a renderização para [HomeContent], [LoadingIndicator] ou [ErrorScreen]
 * conforme o estado atual.
 *
 * Requisito 5.6: exibe o streak atual do usuário de forma proeminente.
 * Requisito 12.1: tela inicial após Splash/Onboarding.
 * Requisito 16.6: badge "Revisar Agora" com contagem de SRS cards devidos.
 * Requisito 18.4: seção "Revisar Agora" visível apenas quando dueCardCount > 0.
 *
 * @param navController Controlador de navegação usado para transições entre telas.
 * @param viewModel     ViewModel injetado via Hilt; provê o estado da tela.
 */
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> LoadingIndicator()
        uiState.error != null -> ErrorScreen(
            message = uiState.error!!,
            onRetry = viewModel::retry
        )
        else -> HomeContent(
            uiState = uiState,
            onStudyClick = { navController.navigate(HomeNavRoutes.LEVEL_SELECT) },
            onProgressClick = { navController.navigate(HomeNavRoutes.PROGRESS) },
            onSettingsClick = { navController.navigate(HomeNavRoutes.SETTINGS) },
            onReviewClick = { navController.navigate(HomeNavRoutes.SRS_REVIEW) },
            onVocabularyClick = { navController.navigate(HomeNavRoutes.VOCABULARY) }
        )
    }
}

// ---------------------------------------------------------------------------
// HomeContent — Conteúdo principal
// ---------------------------------------------------------------------------

/**
 * Conteúdo da HomeScreen quando os dados estão carregados com sucesso.
 *
 * Exibe:
 * - Barra superior com o nome do app e ações de progresso e configurações.
 * - Card de streak com emoji de fogo e dias seguidos (Req. 5.6).
 * - Card de estatísticas gerais (XP total, nível atual, exercícios concluídos).
 * - Card "Revisar Agora" com badge de contagem — visível apenas quando dueCardCount > 0 (Req. 16.6, 18.4).
 * - Botão "Continuar Estudando" para navegar ao seletor de níveis.
 * - Linha de acesso rápido aos níveis desbloqueados (quando disponíveis) (Req. 6.3).
 *
 * @param uiState         Estado atual da HomeScreen.
 * @param onStudyClick    Callback ao pressionar "Continuar Estudando".
 * @param onProgressClick Callback ao pressionar o ícone de progresso.
 * @param onSettingsClick Callback ao pressionar o ícone de configurações.
 * @param onReviewClick   Callback ao pressionar "Revisar Agora" (Req. 16.6, 18.4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onStudyClick: () -> Unit,
    onProgressClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onReviewClick: () -> Unit,
    onVocabularyClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "English Learning",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onProgressClick) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Ver progresso"
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações"
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Saudação com nome de usuário (futuro) ou mensagem padrão
            Text(
                text = "Bom estudo! 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            // Card de streak (Req. 5.6)
            StreakCard(currentStreak = uiState.currentStreak)

            // Card de estatísticas gerais
            uiState.userStats?.let { stats ->
                StatsCard(stats = stats)
            }

            // Card "Revisar Agora" — visível apenas quando há cards devidos (Req. 16.6, 18.4)
            if (uiState.dueCardCount > 0) {
                SrsReviewCard(
                    dueCardCount = uiState.dueCardCount,
                    onReviewClick = onReviewClick
                )
            }

            // Botão principal "Continuar Estudando"
            Button(
                onClick = onStudyClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Continuar Estudando",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            // Botão de Vocabulário
            FilledTonalButton(
                onClick = onVocabularyClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📚 Vocabulário com Imagens",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Acesso rápido aos níveis desbloqueados (Req. 6.3)
            if (uiState.unlockedLevels.isNotEmpty()) {
                UnlockedLevelsRow(
                    levels = uiState.unlockedLevels,
                    onLevelClick = onStudyClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// SrsReviewCard — "Revisar Agora" com badge (Req. 16.6, 18.4)
// ---------------------------------------------------------------------------

/**
 * Card de acesso rápido à revisão SRS, exibido apenas quando [dueCardCount] > 0.
 *
 * Mostra um badge com a contagem de cards devidos e um botão "Revisar Agora".
 *
 * Requisito 16.6: exibir na HomeScreen um badge com a contagem de SRS cards devidos.
 * Requisito 18.4: seção "Revisar Agora" exibida somente quando há cards devidos.
 *
 * @param dueCardCount  Número de cards com dueDate <= hoje.
 * @param onReviewClick Callback ao pressionar o card/botão de revisão.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SrsReviewCard(
    dueCardCount: Int,
    onReviewClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(
                                text = if (dueCardCount > 99) "99+" else dueCardCount.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Column {
                    Text(
                        text = "Revisar Agora",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "$dueCardCount card${if (dueCardCount != 1) "s" else ""} para revisar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            FilledTonalButton(
                onClick = onReviewClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Text(
                    text = "Revisar",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// StreakCard — Exibe o streak do usuário (Req. 5.6)
// ---------------------------------------------------------------------------

/**
 * Card que exibe o streak diário atual do usuário de forma proeminente.
 *
 * Requisito 5.6: streak exibido na HomeScreen.
 *
 * @param currentStreak Número de dias consecutivos de estudo.
 */
@Composable
fun StreakCard(currentStreak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🔥",
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$currentStreak",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (currentStreak == 1) "dia seguido" else "dias seguidos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// StatsCard — Estatísticas gerais do usuário
// ---------------------------------------------------------------------------

/**
 * Card que exibe as estatísticas gerais do usuário: XP total, nível atual e
 * total de exercícios concluídos.
 *
 * @param stats Estatísticas do usuário obtidas via [HomeViewModel.uiState].
 */
@Composable
fun StatsCard(stats: UserStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Suas Estatísticas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "XP Total",
                    value = stats.totalXp.toString()
                )
                StatItem(
                    label = "Nível Atual",
                    value = stats.currentLevelId.toString()
                )
                StatItem(
                    label = "Exercícios",
                    value = stats.totalExercisesCompleted.toString()
                )
            }
        }
    }
}

/**
 * Item de estatística individual exibido dentro do [StatsCard].
 *
 * @param label Rótulo descritivo da estatística.
 * @param value Valor numérico formatado como String.
 */
@Composable
fun StatItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// UnlockedLevelsRow — Acesso rápido a níveis desbloqueados (Req. 6.3)
// ---------------------------------------------------------------------------

/**
 * Linha de acesso rápido com cards dos níveis desbloqueados.
 *
 * Requisito 6.3: exibir níveis desbloqueados na tela inicial.
 *
 * @param levels      Lista de níveis desbloqueados ([Level.isUnlocked] == true).
 * @param onLevelClick Callback ao pressionar um card de nível.
 */
@Composable
fun UnlockedLevelsRow(
    levels: List<Level>,
    onLevelClick: () -> Unit
) {
    Column {
        Text(
            text = "Seus Níveis",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(items = levels, key = { it.id }) { level ->
                LevelQuickAccessCard(
                    level = level,
                    onClick = onLevelClick
                )
            }
        }
    }
}

/**
 * Card de acesso rápido para um nível desbloqueado.
 *
 * Exibe o nome do nível (em inglês, Req. 12.5) e um botão para iniciar
 * ou continuar os estudos naquele nível.
 *
 * Requisito 12.5: nomes dos níveis exibidos em inglês (nameKey do conteúdo).
 *
 * @param level   Nível desbloqueado a ser exibido.
 * @param onClick Callback ao pressionar o card.
 */
@Composable
fun LevelQuickAccessCard(
    level: Level,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ícone de nível desbloqueado vs bloqueado
            if (level.isUnlocked) {
                Text(
                    text = "⭐",
                    style = MaterialTheme.typography.titleLarge
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Nível bloqueado",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            // Nome do nível em inglês (Req. 12.5)
            Text(
                text = level.nameKey,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            FilledTonalButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Estudar",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// LoadingIndicator — Estado de carregamento
// ---------------------------------------------------------------------------

/**
 * Indicador de carregamento exibido enquanto os dados da HomeScreen estão
 * sendo buscados do repositório.
 */
@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// ---------------------------------------------------------------------------
// ErrorScreen — Estado de erro (Req. 12.7)
// ---------------------------------------------------------------------------

/**
 * Tela de erro exibida quando o carregamento dos dados da HomeScreen falha.
 *
 * Oferece uma opção de retry para o usuário tentar novamente.
 *
 * Requisito 12.7: exibir mensagem de erro amigável com opção de retry.
 *
 * @param message Mensagem de erro a ser exibida.
 * @param onRetry Callback ao pressionar o botão de nova tentativa.
 */
@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
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
                text = "Não foi possível carregar os dados",
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
            TextButton(onClick = onRetry) {
                Text(text = "Recarregar")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    MaterialTheme {
        HomeContent(
            uiState = HomeUiState(
                isLoading = false,
                currentStreak = 7,
                userStats = UserStats(
                    userId = 1L,
                    totalXp = 1250,
                    totalExercisesCompleted = 42,
                    totalCorrectAnswers = 38,
                    currentStreak = 7,
                    longestStreak = 14,
                    currentLevelId = 2
                ),
                dueCardCount = 5,
                unlockedLevels = listOf(
                    Level(
                        id = 1,
                        nameKey = "Beginner",
                        descriptionKey = "level_1_desc",
                        minScore = 0,
                        maxScore = 500,
                        isUnlocked = true,
                        sortOrder = 1
                    ),
                    Level(
                        id = 2,
                        nameKey = "Elementary",
                        descriptionKey = "level_2_desc",
                        minScore = 500,
                        maxScore = 1500,
                        isUnlocked = true,
                        sortOrder = 2
                    )
                )
            ),
            onStudyClick = {},
            onProgressClick = {},
            onSettingsClick = {},
            onReviewClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StreakCardPreview() {
    MaterialTheme {
        StreakCard(currentStreak = 5)
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingIndicatorPreview() {
    MaterialTheme {
        LoadingIndicator()
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorScreenPreview() {
    MaterialTheme {
        ErrorScreen(
            message = "Erro ao carregar dados",
            onRetry = {}
        )
    }
}
