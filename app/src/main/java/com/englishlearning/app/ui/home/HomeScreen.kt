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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.englishlearning.app.ui.components.ErrorState
import com.englishlearning.app.ui.components.HapticType
import com.englishlearning.app.ui.components.LoadingAnimation
import com.englishlearning.app.ui.components.rememberHapticFeedback

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
    const val DRILLING = "drilling"
    const val VOCABULARY = "vocabulary"

    /** Rota para a tela de revisão de erros. */
    const val ERROR_REVIEW = "error_review"
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
            onVocabularyClick = { navController.navigate(HomeNavRoutes.VOCABULARY) },
            onDrillingClick = { navController.navigate(HomeNavRoutes.DRILLING) },
            onErrorReviewClick = { navController.navigate(HomeNavRoutes.ERROR_REVIEW) }
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
 * - Card "Revisar Erros" com badge de contagem — visível apenas quando errorExerciseCount > 0.
 * - Botão "Continuar Estudando" para navegar ao seletor de níveis.
 * - Linha de acesso rápido aos níveis desbloqueados (quando disponíveis) (Req. 6.3).
 *
 * @param uiState           Estado atual da HomeScreen.
 * @param onStudyClick      Callback ao pressionar "Continuar Estudando".
 * @param onProgressClick   Callback ao pressionar o ícone de progresso.
 * @param onSettingsClick   Callback ao pressionar o ícone de configurações.
 * @param onReviewClick     Callback ao pressionar "Revisar Agora" (Req. 16.6, 18.4).
 * @param onErrorReviewClick Callback ao pressionar "Revisar Erros".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onStudyClick: () -> Unit,
    onProgressClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onReviewClick: () -> Unit,
    onVocabularyClick: () -> Unit = {},
    onDrillingClick: () -> Unit = {},
    onErrorReviewClick: () -> Unit = {}
) {
    val haptic = rememberHapticFeedback()
    
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

            // Hero card: nível CEFR, barra de XP, streak, exercícios (A + B)
            uiState.userStats?.let { stats ->
                HeroCard(currentStreak = uiState.currentStreak, stats = stats)
            } ?: StreakCard(currentStreak = uiState.currentStreak)

            // Card "Revisar Agora" — visível apenas quando há cards devidos (Req. 16.6, 18.4)
            if (uiState.dueCardCount > 0) {
                SrsReviewCard(
                    dueCardCount = uiState.dueCardCount,
                    onReviewClick = {
                        haptic.perform(HapticType.LIGHT)
                        onReviewClick()
                    }
                )
            }

            // Card "Revisar Erros" — visível apenas quando há exercícios com erros
            if (uiState.errorExerciseCount > 0) {
                ErrorReviewCard(
                    errorCount = uiState.errorExerciseCount,
                    onReviewClick = {
                        haptic.perform(HapticType.LIGHT)
                        onErrorReviewClick()
                    }
                )
            }

            // Botão principal "Continuar Estudando" com feedback háptico
            Button(
                onClick = {
                    haptic.perform(HapticType.MEDIUM)
                    onStudyClick()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Continuar Estudando",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            // Botões secundários lado a lado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        haptic.perform(HapticType.LIGHT)
                        onVocabularyClick()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("📚 Vocabulário", style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = {
                        haptic.perform(HapticType.LIGHT)
                        onDrillingClick()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("⚡ Drilling", style = MaterialTheme.typography.labelMedium)
                }
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
// HeroCard — Dashboard principal com nível CEFR + XP + streak  (A + B)
// ---------------------------------------------------------------------------

/**
 * Card herói que substitui o StreakCard + StatsCard.
 * Exibe o badge de nível CEFR, barra de XP, streak e exercícios completos
 * em um único bloco visual com fundo primary.
 */
@Composable
fun HeroCard(currentStreak: Int, stats: UserStats) {
    val cefr      = cefrLabel(stats.currentLevelId)
    val levelName = levelDisplayName(stats.currentLevelId)
    val (xpMin, xpMax) = xpRangeForLevel(stats.currentLevelId)
    val isMaxLevel = stats.currentLevelId >= 6
    val progress  = if (!isMaxLevel && xpMax > xpMin)
        ((stats.totalXp - xpMin).toFloat() / (xpMax - xpMin)).coerceIn(0f, 1f)
    else 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Linha superior: badge CEFR + stats ───────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge de nível CEFR
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text  = cefr,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text  = levelName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

                // Stats: streak e exercícios
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    HeroStat(
                        emoji = "🔥",
                        value = "$currentStreak",
                        label = if (currentStreak == 1) "dia" else "dias"
                    )
                    HeroStat(
                        emoji = "✏️",
                        value = "${stats.totalExercisesCompleted}",
                        label = "exercícios"
                    )
                }
            }

            // ── Barra de XP ───────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = "⭐ ${stats.totalXp} XP",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text  = if (isMaxLevel) "Nível máximo 🏆"
                                else "próximo: $xpMax XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.70f)
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color      = Color.White,
                    trackColor = Color.White.copy(alpha = 0.28f)
                )
            }
        }
    }
}

@Composable
private fun HeroStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Text(
            text  = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.75f)
        )
    }
}

// ── Helpers de nível ──────────────────────────────────────────────────────────

/** Rótulo CEFR baseado no levelId (1–6). */
private fun cefrLabel(levelId: Int) = when (levelId) {
    1    -> "A1"
    2    -> "A2"
    3    -> "A2+"
    4    -> "B1"
    5    -> "B2"
    else -> "C1"
}

/** Nome legível do nível. */
private fun levelDisplayName(levelId: Int) = when (levelId) {
    1    -> "Iniciante"
    2    -> "Elementar"
    3    -> "Pré-Intermediário"
    4    -> "Intermediário"
    5    -> "Avançado"
    else -> "Proficiente"
}

/** Faixa de XP de início e fim para o nível atual (para a barra de progresso). */
private fun xpRangeForLevel(levelId: Int): Pair<Int, Int> = when (levelId) {
    1    -> Pair(0,     1_000)
    2    -> Pair(1_000, 2_500)
    3    -> Pair(2_500, 5_000)
    4    -> Pair(5_000, 10_000)
    5    -> Pair(10_000, 20_000)
    else -> Pair(20_000, 20_000)   // nível máximo
}

// ---------------------------------------------------------------------------
// SrsReviewCard — "Revisar Agora" com badge (Req. 16.6, 18.4)
// ---------------------------------------------------------------------------
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
// ErrorReviewCard — "Revisar Erros" com badge
// ---------------------------------------------------------------------------
/**
 * Card que exibe a contagem de exercícios com erros pendentes para revisão.
 *
 * Visível apenas quando errorCount > 0. Ao clicar, navega para a tela de
 * revisão de erros onde o usuário pode praticar os exercícios que errou.
 *
 * @param errorCount    Número de exercícios com erros pendentes.
 * @param onReviewClick Callback ao pressionar "Revisar".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorReviewCard(
    errorCount: Int,
    onReviewClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
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
                                text = if (errorCount > 99) "99+" else errorCount.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                ) {
                    Text(
                        text = "❌",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                Column {
                    Text(
                        text = "Revisar Erros",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "$errorCount exercício${if (errorCount != 1) "s" else ""} para revisar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }
            FilledTonalButton(
                onClick = onReviewClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
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
            // Nome do nível com badge CEFR
            Text(
                text = cefrLabel(level.id),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = levelDisplayName(level.id),
                style = MaterialTheme.typography.bodySmall,
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
// LoadingIndicator — Estado de carregamento com animação Lottie
// ---------------------------------------------------------------------------

/**
 * Indicador de carregamento exibido enquanto os dados da HomeScreen estão
 * sendo buscados do repositório.
 *
 * Utiliza animação Lottie para uma experiência visual mais agradável.
 */
@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingAnimation(size = 120.dp)
    }
}

// ---------------------------------------------------------------------------
// ErrorScreen — Estado de erro com animação (Req. 12.7)
// ---------------------------------------------------------------------------

/**
 * Tela de erro exibida quando o carregamento dos dados da HomeScreen falha.
 *
 * Oferece uma opção de retry para o usuário tentar novamente.
 * Utiliza o componente ErrorState com animação Lottie.
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
    ErrorState(
        message = message,
        onRetry = onRetry
    )
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
