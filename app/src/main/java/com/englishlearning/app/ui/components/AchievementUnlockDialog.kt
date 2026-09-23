package com.englishlearning.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.englishlearning.app.domain.model.Achievement

/**
 * Diálogo animado exibido quando o usuário desbloqueia uma conquista durante a sessão.
 *
 * Apresenta ícone de troféu, nome de exibição da conquista e botão de dispensar.
 *
 * Requisito 8.3: notificação visual imediata ao desbloquear conquista.
 *
 * @param achievement  A conquista recém-desbloqueada.
 * @param onDismiss    Callback invocado ao fechar o diálogo.
 */
@Composable
fun AchievementUnlockDialog(
    achievement: Achievement,
    onDismiss: () -> Unit
) {
    // Trigger entry animation after the first composition frame
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(achievement.achievementKey) { visible = true }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn()
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = "Troféu",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
            }
        },
        title = {
            Text(
                text = "Conquista desbloqueada!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = achievement.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = achievement.description(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Incrível!",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

// ---------------------------------------------------------------------------
// Display name and description helpers
// ---------------------------------------------------------------------------

/**
 * Returns a user-facing display name for the achievement based on its key.
 * Requisito 8.1: marcos específicos mapeados para nomes legíveis.
 */
fun Achievement.displayName(): String = when (achievementKey) {
    "first_correct"        -> "Primeira Resposta Correta"
    "streak_7"             -> "Sequência de 7 Dias"
    "streak_30"            -> "Sequência de 30 Dias"
    "level_1_complete"     -> "Nível 1 Concluído"
    "level_2_complete"     -> "Nível 2 Concluído"
    "accuracy_90_session"  -> "Precisão de 90%"
    else                   -> achievementKey.replace('_', ' ').capitalize()
}

/**
 * Returns a short motivational description for the achievement.
 */
fun Achievement.description(): String = when (achievementKey) {
    "first_correct"        -> "Você acertou sua primeira pergunta em inglês!"
    "streak_7"             -> "Sete dias consecutivos de prática. Continue assim!"
    "streak_30"            -> "Trinta dias seguidos! Você é dedicado de verdade."
    "level_1_complete"     -> "Parabéns! Você completou o Nível 1."
    "level_2_complete"     -> "Incrível! O Nível 2 está no seu histórico."
    "accuracy_90_session"  -> "90% de precisão nesta sessão. Impressionante!"
    else                   -> "Continue assim e conquiste ainda mais!"
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun AchievementUnlockDialogPreview() {
    MaterialTheme {
        AchievementUnlockDialog(
            achievement = Achievement(
                userId = 1L,
                achievementKey = "streak_7",
                isNew = true
            ),
            onDismiss = {}
        )
    }
}
