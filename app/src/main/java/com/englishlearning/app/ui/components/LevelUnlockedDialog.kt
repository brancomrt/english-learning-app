package com.englishlearning.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Diálogo animado exibido quando o usuário completa um nível e desbloqueia o próximo.
 *
 * Mostra animação de cadeado abrindo, o nome do nível desbloqueado e estrelas
 * decorativas para celebrar o avanço.
 *
 * Requisito 6.4: exibir animação/notificação ao desbloquear novo nível.
 * Requisito 12.4: todos os textos em português brasileiro.
 *
 * @param levelNumber  O número do nível recém-concluído (extraído do achievementKey).
 * @param onDismiss    Callback invocado ao fechar o diálogo.
 */
@Composable
fun LevelUnlockedDialog(
    levelNumber: Int,
    onDismiss: () -> Unit
) {
    // Stage the entry animation across two phases for a more dynamic feel
    var showIcon by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var iconScale by remember { mutableStateOf(false) }

    LaunchedEffect(levelNumber) {
        showIcon = true
        delay(150)
        iconScale = true
        delay(200)
        showText = true
    }

    val scale by animateFloatAsState(
        targetValue = if (iconScale) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "icon_scale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            AnimatedVisibility(
                visible = showIcon,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LockOpen,
                        contentDescription = "Nível desbloqueado",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        },
        title = {
            AnimatedVisibility(
                visible = showText,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            ) {
                Text(
                    text = "Novo Nível Desbloqueado!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        text = {
            AnimatedVisibility(
                visible = showText,
                enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Decorative stars
                    StarRow()

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Parabéns! Você concluiu o Nível $levelNumber.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "O Nível ${levelNumber + 1} está disponível para você.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Continuar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

/**
 * Extrai o número do nível a partir de uma chave de conquista no formato "level_X_complete".
 * Retorna null se a chave não corresponder ao padrão esperado.
 */
fun parseLevelNumberFromKey(achievementKey: String): Int? {
    val regex = Regex("^level_(\\d+)_complete$")
    return regex.find(achievementKey)?.groupValues?.get(1)?.toIntOrNull()
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

@Composable
private fun StarRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun LevelUnlockedDialogPreview() {
    MaterialTheme {
        LevelUnlockedDialog(
            levelNumber = 1,
            onDismiss = {}
        )
    }
}
