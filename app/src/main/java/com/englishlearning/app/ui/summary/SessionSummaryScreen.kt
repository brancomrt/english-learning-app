package com.englishlearning.app.ui.summary

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.englishlearning.app.domain.model.Achievement
import com.englishlearning.app.domain.model.SessionSummary
import com.englishlearning.app.ui.components.CelebrationAnimation
import com.englishlearning.app.ui.components.HapticType
import com.englishlearning.app.ui.components.rememberHapticFeedback
import com.englishlearning.app.ui.learning.LearningUiState
import com.englishlearning.app.ui.learning.LearningViewModel
import kotlin.math.roundToInt

/**
 * Tela de resumo ao final de uma sessão de aprendizado.
 *
 * Exibe:
 * - Total de exercícios e acertos (Req. 13.4)
 * - XP ganho na sessão (Req. 13.4)
 * - Tempo decorrido (Req. 13.4)
 * - Conquistas desbloqueadas na sessão (Req. 8.3)
 * - Botão "Nova Sessão" e botão "Ir para Início" (Req. 13.6)
 *
 * @param navController NavController para navegação entre telas.
 * @param viewModel     [LearningViewModel] compartilhado com a sessão de aprendizado.
 */
@Composable
fun SessionSummaryScreen(
    navController: NavController,
    viewModel: LearningViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val summary = (uiState as? LearningUiState.SessionComplete)?.summary ?: return
    val haptic = rememberHapticFeedback()

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Animação de celebração Lottie
                CelebrationAnimation(size = 180.dp)

                // Título com emoji de celebração
                Text(
                    text = "🎉 Parabéns!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sessão concluída",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Card com estatísticas principais da sessão
            item {
                SessionStatsCard(summary = summary)
            }

            // Conquistas desbloqueadas nesta sessão (Req. 8.3)
            if (summary.achievementsUnlocked.isNotEmpty()) {
                item {
                    Text(
                        text = "🏆 Conquistas Desbloqueadas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(summary.achievementsUnlocked) { achievement ->
                    SessionAchievementItem(achievement = achievement)
                }
            }

            // Botões de navegação (Req. 13.6)
            item {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        haptic.perform(HapticType.MEDIUM)
                        // Volta para a tela de seleção de categoria para nova sessão
                        navController.popBackStack(
                            route = "category_select",
                            inclusive = false
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Nova Sessão")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        haptic.perform(HapticType.LIGHT)
                        // Navega para a tela inicial
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Ir para Início")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Card com as estatísticas principais da sessão: acertos, XP e tempo.
 *
 * Requisito 13.4: total de exercícios, respostas corretas, XP total e tempo decorrido.
 */
@Composable
private fun SessionStatsCard(summary: SessionSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Acertos
            SessionStatRow(
                label = "Acertos",
                value = "${summary.correctAnswers} de ${summary.totalExercises}"
            )

            // Taxa de acerto em percentual
            val accuracyPercent = (summary.accuracyRate * 100).roundToInt()
            SessionStatRow(
                label = "Taxa de acerto",
                value = "$accuracyPercent%"
            )

            HorizontalDivider()

            // XP ganho na sessão
            SessionStatRow(
                label = "XP ganho",
                value = "+${summary.totalXpEarned} XP",
                valueColor = MaterialTheme.colorScheme.primary,
                bold = true
            )

            // Tempo da sessão
            val durationText = formatDuration(summary.durationMillis)
            SessionStatRow(
                label = "Tempo",
                value = durationText
            )
        }
    }
}

/**
 * Linha de estatística com rótulo à esquerda e valor à direita.
 */
@Composable
private fun SessionStatRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Item de conquista desbloqueada durante a sessão.
 */
@Composable
private fun SessionAchievementItem(achievement: Achievement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = "Conquista desbloqueada",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = achievement.achievementKey.toDisplayName(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Nova conquista!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/**
 * Formata a duração em milissegundos para "Xm Ys" ou "Xs" se menos de 1 minuto.
 */
private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}

/**
 * Converte a chave de conquista para nome de exibição em português.
 *
 * Requisito 8.4: exibir nome legível das conquistas.
 */
internal fun String.toDisplayName(): String = when (this) {
    "first_correct"       -> "Primeira Acerto"
    "streak_7"            -> "7 Dias Seguidos"
    "streak_30"           -> "30 Dias Seguidos"
    "level_1_complete"    -> "Nível 1 Completo"
    "level_2_complete"    -> "Nível 2 Completo"
    "accuracy_90_session" -> "Precisão 90%"
    else                  -> this.replace('_', ' ')
        .split(' ')
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
}
