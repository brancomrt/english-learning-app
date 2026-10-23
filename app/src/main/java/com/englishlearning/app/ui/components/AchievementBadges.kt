package com.englishlearning.app.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tipos de conquistas com seus visuais correspondentes.
 */
enum class AchievementBadgeType(
    val icon: ImageVector,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color
) {
    /** Primeira resposta correta */
    FIRST_CORRECT(
        icon = Icons.Filled.Star,
        primaryColor = Color(0xFFFCD34D), // Amber-300
        secondaryColor = Color(0xFFF59E0B), // Amber-500
        glowColor = Color(0xFFFEF3C7) // Amber-50
    ),
    
    /** Streak de 7 dias */
    STREAK_7(
        icon = Icons.Filled.LocalFireDepartment,
        primaryColor = Color(0xFFFB923C), // Orange-400
        secondaryColor = Color(0xFFEA580C), // Orange-600
        glowColor = Color(0xFFFFEDD5) // Orange-50
    ),
    
    /** Streak de 30 dias */
    STREAK_30(
        icon = Icons.Filled.LocalFireDepartment,
        primaryColor = Color(0xFFF87171), // Red-400
        secondaryColor = Color(0xFFDC2626), // Red-600
        glowColor = Color(0xFFFEE2E2) // Red-50
    ),
    
    /** Completou um nível */
    LEVEL_COMPLETE(
        icon = Icons.Filled.School,
        primaryColor = Color(0xFF60A5FA), // Blue-400
        secondaryColor = Color(0xFF2563EB), // Blue-600
        glowColor = Color(0xFFDBEAFE) // Blue-50
    ),
    
    /** 90% de precisão */
    ACCURACY_90(
        icon = Icons.Filled.Verified,
        primaryColor = Color(0xFF34D399), // Emerald-400
        secondaryColor = Color(0xFF059669), // Emerald-600
        glowColor = Color(0xFFD1FAE5) // Emerald-50
    ),
    
    /** Conquista especial / troféu */
    TROPHY(
        icon = Icons.Filled.EmojiEvents,
        primaryColor = Color(0xFFFCD34D), // Gold
        secondaryColor = Color(0xFFB45309), // Dark gold
        glowColor = Color(0xFFFEF3C7)
    )
}

/**
 * Badge de conquista visual com gradiente e ícone.
 *
 * @param type Tipo da conquista (define cores e ícone).
 * @param size Tamanho do badge.
 * @param isUnlocked Se a conquista está desbloqueada.
 * @param modifier Modifier para customização.
 */
@Composable
fun AchievementBadge(
    type: AchievementBadgeType,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    isUnlocked: Boolean = true
) {
    val colors = if (isUnlocked) {
        listOf(type.primaryColor, type.secondaryColor)
    } else {
        listOf(Color.Gray.copy(alpha = 0.5f), Color.Gray.copy(alpha = 0.3f))
    }
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect (só quando desbloqueado)
        if (isUnlocked) {
            Box(
                modifier = Modifier
                    .size(size * 1.1f)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                type.glowColor.copy(alpha = 0.6f),
                                type.glowColor.copy(alpha = 0f)
                            )
                        )
                    )
            )
        }
        
        // Badge circle with gradient
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(colors = colors)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null,
                    modifier = Modifier.size(size * 0.5f),
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Card de conquista completo com título e descrição.
 *
 * @param title Título da conquista.
 * @param description Descrição de como foi obtida.
 * @param badgeType Tipo visual do badge.
 * @param isUnlocked Se está desbloqueada.
 * @param isNew Se é uma conquista recém-desbloqueada.
 * @param modifier Modifier para customização.
 */
@Composable
fun AchievementCard(
    title: String,
    description: String,
    badgeType: AchievementBadgeType,
    modifier: Modifier = Modifier,
    isUnlocked: Boolean = true,
    isNew: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isNew) {
                badgeType.glowColor.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AchievementBadge(
                type = badgeType,
                size = 56.dp,
                isUnlocked = isUnlocked
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isUnlocked) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }
                    )
                    
                    if (isNew) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeType.primaryColor
                        ) {
                            Text(
                                text = "NOVO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUnlocked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
            }
        }
    }
}

/**
 * Grid de badges de conquistas (para tela de progresso).
 */
@Composable
fun AchievementBadgeGrid(
    achievements: List<Pair<AchievementBadgeType, Boolean>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        achievements.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowItems.forEach { (type, isUnlocked) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AchievementBadge(
                            type = type,
                            size = 48.dp,
                            isUnlocked = isUnlocked
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isUnlocked) "✓" else "?",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUnlocked) {
                                type.secondaryColor
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun AchievementBadgePreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AchievementBadge(type = AchievementBadgeType.FIRST_CORRECT)
            AchievementBadge(type = AchievementBadgeType.STREAK_7)
            AchievementBadge(type = AchievementBadgeType.LEVEL_COMPLETE)
            AchievementBadge(type = AchievementBadgeType.ACCURACY_90, isUnlocked = false)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AchievementCardPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AchievementCard(
                title = "Primeira Resposta",
                description = "Acertou sua primeira pergunta!",
                badgeType = AchievementBadgeType.FIRST_CORRECT,
                isNew = true
            )
            AchievementCard(
                title = "Streak de 7 dias",
                description = "Estude por 7 dias consecutivos",
                badgeType = AchievementBadgeType.STREAK_7,
                isUnlocked = false
            )
        }
    }
}
