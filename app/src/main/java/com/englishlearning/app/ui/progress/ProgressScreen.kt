package com.englishlearning.app.ui.progress

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.englishlearning.app.domain.model.Achievement
import com.englishlearning.app.domain.model.UserStats
import com.englishlearning.app.ui.summary.toDisplayName

/**
 * Conjunto de todas as chaves de conquistas possíveis no aplicativo.
 *
 * Requisito 8.4: exibir conquistas desbloqueadas E bloqueadas.
 */
private val ALL_ACHIEVEMENT_KEYS = listOf(
    "first_correct",
    "streak_7",
    "streak_30",
    "level_1_complete",
    "level_2_complete",
    "accuracy_90_session"
)

/**
 * Tela de progresso do usuário.
 *
 * Exibe:
 * - Streak atual e recorde (Req. 5.5)
 * - Estatísticas gerais: XP total, exercícios concluídos, acertos (Req. 8.4)
 * - Lista de todas as conquistas, desbloqueadas e bloqueadas (Req. 8.4)
 *
 * Ao abrir a tela, marca todas as conquistas como vistas (Req. 8.5).
 *
 * @param navController NavController para navegação.
 * @param viewModel     [ProgressViewModel] injetado pelo Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    navController: NavController,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Req. 8.5: marcar conquistas como vistas ao abrir a tela
    LaunchedEffect(Unit) {
        viewModel.markAchievementsAsSeen()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Meu Progresso") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Seção de streak
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    StreakSection(
                        currentStreak = uiState.currentStreak,
                        longestStreak = uiState.longestStreak
                    )
                }

                // Seção de estatísticas gerais
                uiState.userStats?.let { stats ->
                    item {
                        StatsSection(userStats = stats)
                    }
                }

                // Cabeçalho da seção de conquistas
                item {
                    Text(
                        text = "Conquistas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Lista de todas as conquistas (desbloqueadas + bloqueadas)
                // Req. 8.4: exibir todas as conquistas
                items(ALL_ACHIEVEMENT_KEYS) { key ->
                    val unlocked = uiState.achievements.firstOrNull { it.achievementKey == key }
                    AchievementItem(
                        achievementKey = key,
                        unlockedAchievement = unlocked
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

/**
 * Seção de streak com ícone de fogo, streak atual e recorde.
 *
 * Requisito 5.5: exibir currentStreak e longestStreak.
 */
@Composable
private fun StreakSection(currentStreak: Int, longestStreak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StreakStat(
                icon = "🔥",
                label = "Streak Atual",
                value = "$currentStreak ${if (currentStreak == 1) "dia" else "dias"}"
            )

            VerticalDivider(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                thickness = 1.dp
            )

            StreakStat(
                icon = "⭐",
                label = "Recorde",
                value = "$longestStreak ${if (longestStreak == 1) "dia" else "dias"}"
            )
        }
    }
}

/**
 * Estatística de streak individual (ícone + rótulo + valor).
 */
@Composable
private fun StreakStat(icon: String, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Seção de estatísticas gerais do usuário: XP, exercícios e acertos.
 */
@Composable
private fun StatsSection(userStats: UserStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Estatísticas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            HorizontalDivider()

            StatRow(label = "XP Total", value = "${userStats.totalXp} XP")
            StatRow(
                label = "Exercícios concluídos",
                value = "${userStats.totalExercisesCompleted}"
            )
            StatRow(label = "Total de acertos", value = "${userStats.totalCorrectAnswers}")

            val accuracyPercent = if (userStats.totalExercisesCompleted > 0) {
                (userStats.overallAccuracyRate * 100).toInt()
            } else 0
            StatRow(label = "Taxa de acerto geral", value = "$accuracyPercent%")
        }
    }
}

/**
 * Linha de estatística com rótulo e valor.
 */
@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Item de conquista individual.
 *
 * - Desbloqueada: ícone de troféu dourado + nome da conquista (Req. 8.4).
 * - Bloqueada: ícone de cadeado + nome em cinza (Req. 8.4).
 *
 * @param achievementKey        Chave da conquista.
 * @param unlockedAchievement   A conquista desbloqueada, ou null se ainda bloqueada.
 */
@Composable
private fun AchievementItem(
    achievementKey: String,
    unlockedAchievement: Achievement?
) {
    val isUnlocked = unlockedAchievement != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isUnlocked) 1f else 0.5f),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ícone: troféu para desbloqueada, cadeado para bloqueada
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (isUnlocked)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Conquista desbloqueada",
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Conquista bloqueada",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievementKey.toDisplayName(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isUnlocked) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isUnlocked)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isUnlocked) {
                    Text(
                        text = "Desbloqueada",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    Text(
                        text = "Ainda não desbloqueada",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Indicador de nova conquista (Req. 8.5: isNew indica não visualizada)
            if (isUnlocked && unlockedAchievement.isNew) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = MaterialTheme.colorScheme.error,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
