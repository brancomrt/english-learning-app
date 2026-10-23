package com.englishlearning.app.ui.learning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightbulbCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishlearning.app.domain.model.AcceptedAnswer
import com.englishlearning.app.domain.model.Correction
import com.englishlearning.app.domain.model.ErrorType
import com.englishlearning.app.domain.model.Hint
import com.englishlearning.app.domain.model.HintType
import com.englishlearning.app.domain.model.ValidationResult
import com.englishlearning.app.domain.model.ValidationStatus

// ---------------------------------------------------------------------------
// FeedbackScreen
//
// Implementa os Requisitos 3.1–3.6:
//   3.1 — Exibir primeira dica ao errar na primeira tentativa.
//   3.2 — Revelar dicas progressivamente em ordem de hintOrder.
//   3.3 — Dicas do tipo ANSWER exibidas apenas após as anteriores.
//   3.4 — Feedback visual positivo com score e XP ao acertar.
//   3.5 — Feedback de encorajamento quando TOO_CLOSE.
//   3.6 — Exibir resposta aceita principal após esgotamento de tentativas.
// ---------------------------------------------------------------------------

/**
 * Número máximo de tentativas antes de revelar a resposta aceita principal.
 * Após este limite, [FeedbackScreen] exibe a resposta canônica.
 *
 * Requisito 3.6: exibir a pergunta aceita principal ao esgotar tentativas.
 */
const val MAX_ATTEMPTS_BEFORE_REVEAL = 3

/**
 * Tela/overlay de feedback exibida após o usuário submeter uma resposta.
 *
 * É exibida como um painel que ocupa a tela inteira sobre o exercício,
 * com animações de entrada distintas para cada status de validação.
 *
 * Responsabilidades:
 * - Mostrar status da validação (CORRECT / TOO_CLOSE / PARTIAL / INCORRECT)
 *   com cores, ícone e título adequados — Req. 3.4, 3.5.
 * - Mostrar pontuação e XP ganhos — Req. 3.4.
 * - Mostrar correções textuais quando incorreto/parcial — Req. 2.10.
 * - Mostrar dicas reveladas progressivamente — Req. 3.1, 3.2, 3.3.
 * - Mostrar a resposta aceita principal quando as tentativas foram esgotadas
 *   (attemptCount >= [MAX_ATTEMPTS_BEFORE_REVEAL]) — Req. 3.6.
 * - Botão "Próximo exercício" quando CORRECT, "Tentar novamente" para os
 *   demais casos (ou "Próximo" ao esgotar tentativas).
 *
 * @param result            Resultado da validação retornado pelo [QuestionValidationEngine].
 * @param xpEarned          XP calculado para esta tentativa.
 * @param attemptCount      Total de tentativas realizadas para o exercício atual.
 * @param revealedHints     Dicas já reveladas pelo ViewModel (em ordem de hintOrder).
 * @param primaryAnswer     Resposta aceita canônica do exercício (isPrimary == true),
 *                          exibida ao esgotar as tentativas — Req. 3.6.
 * @param onNext            Callback para avançar ao próximo exercício.
 * @param onTryAgain        Callback para tentar novamente (limpa o campo de entrada).
 * @param modifier          Modifier externo.
 */
@Composable
fun FeedbackScreen(
    result: ValidationResult,
    xpEarned: Int,
    attemptCount: Int,
    revealedHints: List<Hint>,
    primaryAnswer: AcceptedAnswer?,
    onNext: () -> Unit,
    onTryAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ------------------------------------------------------------------
    // Derived state
    // ------------------------------------------------------------------
    val isCorrect = result.status == ValidationStatus.CORRECT
    val attemptsExhausted = !isCorrect && attemptCount >= MAX_ATTEMPTS_BEFORE_REVEAL

    // ------------------------------------------------------------------
    // Animation trigger: fires once when the composable first enters
    // ------------------------------------------------------------------
    var visible by remember(result) { mutableStateOf(false) }
    LaunchedEffect(result) { visible = true }

    // ------------------------------------------------------------------
    // Visual theme per status
    // ------------------------------------------------------------------
    val theme = feedbackTheme(result.status)

    // Animated background color transition
    val bgColor by animateColorAsState(
        targetValue = if (visible) theme.containerColor else theme.containerColor.copy(alpha = 0f),
        animationSpec = tween(durationMillis = 350),
        label = "feedback_bg"
    )

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ----------------------------------------------------------
                // Header: animated status icon + title + score/XP
                // Requisito 3.4, 3.5
                // ----------------------------------------------------------
                FeedbackHeader(
                    theme = theme,
                    result = result,
                    xpEarned = xpEarned,
                    visible = visible
                )

                // ----------------------------------------------------------
                // Corrections (when answer was wrong or partial)
                // ----------------------------------------------------------
                if (result.corrections.isNotEmpty()) {
                    CorrectionsSection(corrections = result.corrections)
                }

                // ----------------------------------------------------------
                // Progressive hints (Req. 3.1, 3.2, 3.3)
                // ----------------------------------------------------------
                if (revealedHints.isNotEmpty()) {
                    HintsSection(hints = revealedHints)
                }

                // ----------------------------------------------------------
                // Primary accepted answer reveal (Req. 3.6)
                // Shown when attempts are exhausted and answer is not correct
                // ----------------------------------------------------------
                if (attemptsExhausted && primaryAnswer != null) {
                    AcceptedAnswerReveal(primaryAnswer = primaryAnswer)
                }

                // Spacer to push action buttons to the bottom when content is short
                Spacer(modifier = Modifier.height(24.dp))

                // ----------------------------------------------------------
                // Action buttons
                // ----------------------------------------------------------
                FeedbackActions(
                    isCorrect = isCorrect,
                    attemptsExhausted = attemptsExhausted,
                    onNext = onNext,
                    onTryAgain = onTryAgain
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Header — animated icon + title + score/XP badge
// ---------------------------------------------------------------------------

/**
 * Cabeçalho do feedback com ícone animado, título e badge de pontuação/XP.
 *
 * Requisito 3.4: feedback visual positivo com score e XP.
 * Requisito 3.5: encorajamento quando TOO_CLOSE.
 */
@Composable
private fun FeedbackHeader(
    theme: FeedbackTheme,
    result: ValidationResult,
    xpEarned: Int,
    visible: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Animated status icon
        AnimatedStatusIcon(theme = theme, visible = visible)

        // Status title
        Text(
            text = theme.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = theme.contentColor,
            textAlign = TextAlign.Center
        )

        // Subtitle / encouragement message
        Text(
            text = theme.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Score + XP badges (shown only when there is a score to display)
        if (result.score > 0 || xpEarned > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (result.score > 0) {
                    ScoreBadge(
                        label = "Pontuação",
                        value = "${result.score} pts",
                        containerColor = theme.badgeContainerColor,
                        contentColor = theme.badgeContentColor
                    )
                }
                if (xpEarned > 0) {
                    ScoreBadge(
                        label = "XP ganho",
                        value = "+$xpEarned XP",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

// ---------------------------------------------------------------------------
// Animated icon
// ---------------------------------------------------------------------------

/**
 * Ícone de status com animação de escala/bounce na entrada.
 */
@Composable
private fun AnimatedStatusIcon(theme: FeedbackTheme, visible: Boolean) {
    // Pulse animation for CORRECT to reinforce the success feeling
    val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (theme.pulsing) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.3f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(theme.iconBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = theme.icon,
                contentDescription = theme.title,
                tint = theme.iconTint,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Score badge
// ---------------------------------------------------------------------------

/**
 * Pill/badge exibindo rótulo e valor (pontuação ou XP).
 */
@Composable
private fun ScoreBadge(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Corrections section
// ---------------------------------------------------------------------------

/**
 * Seção de correções textuais sugeridas ao usuário.
 * Exibida quando a validação resulta em INCORRECT ou PARTIAL.
 * Agora inclui explicações detalhadas do tipo de erro.
 */
@Composable
private fun CorrectionsSection(corrections: List<Correction>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(
            icon = Icons.Filled.Info,
            title = "Por que está errado?"
        )
        corrections.forEach { correction ->
            CorrectionItem(correction = correction)
        }
    }
}

@Composable
private fun CorrectionItem(correction: Correction) {
    var isExpanded by remember { mutableStateOf(false) }
    val (wrongExample, correctExample) = correction.type.exampleWithCorrection()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header com tipo de erro e sugestão
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = correction.type.displayName(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = correction.suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Botão para expandir/colapsar explicação detalhada
            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = if (isExpanded) "Ocultar explicação" else "Ver explicação detalhada",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Conteúdo expandido com explicação detalhada
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f)
                    )
                    
                    // Explicação detalhada
                    Text(
                        text = correction.type.detailedExplanation(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        lineHeight = 18.sp
                    )
                    
                    // Exemplo: errado vs correto
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Exemplo:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "❌",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = wrongExample,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✅",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = correctExample,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Hints section
// ---------------------------------------------------------------------------

/**
 * Seção de dicas reveladas progressivamente.
 *
 * Requisito 3.1: exibir a primeira dica ao errar na primeira tentativa.
 * Requisito 3.2: revelar dicas progressivamente em ordem de hintOrder.
 * Requisito 3.3: dicas do tipo ANSWER exibidas apenas após as anteriores.
 */
@Composable
private fun HintsSection(hints: List<Hint>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(
            icon = Icons.Filled.LightbulbCircle,
            title = "Dicas"
        )
        hints.forEach { hint ->
            HintItem(hint = hint)
        }
    }
}

@Composable
private fun HintItem(hint: Hint) {
    // Dicas do tipo ANSWER usam cor de destaque para indicar que revelam a resposta
    val (containerColor, contentColor) = if (hint.hintType == HintType.ANSWER) {
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(300)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = containerColor
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.LightbulbCircle,
                    contentDescription = "Dica ${hint.hintOrder}",
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Dica ${hint.hintOrder}",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = hint.hintText,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Accepted answer reveal
// ---------------------------------------------------------------------------

/**
 * Exibe a pergunta aceita principal quando as tentativas foram esgotadas.
 *
 * Requisito 3.6: exibir a pergunta aceita principal (isPrimary = true) após o
 * usuário atingir o número máximo de tentativas sem acertar.
 */
@Composable
private fun AcceptedAnswerReveal(primaryAnswer: AcceptedAnswer) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Resposta esperada",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                text = primaryAnswer.questionText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Estude essa estrutura e tente novamente na próxima sessão.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Action buttons
// ---------------------------------------------------------------------------

/**
 * Botões de ação do feedback:
 * - CORRECT: "Próximo exercício" → [onNext].
 * - Tentativas esgotadas: "Próximo exercício" → [onNext].
 * - Demais: "Tentar novamente" → [onTryAgain].
 */
@Composable
private fun FeedbackActions(
    isCorrect: Boolean,
    attemptsExhausted: Boolean,
    onNext: () -> Unit,
    onTryAgain: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isCorrect || attemptsExhausted) {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                colors = if (isCorrect) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                }
            ) {
                Text(
                    text = "Próximo exercício",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } else {
            Button(
                onClick = onTryAgain,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tentar novamente",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section title
// ---------------------------------------------------------------------------

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

// ---------------------------------------------------------------------------
// FeedbackTheme — visual parameters per ValidationStatus
// ---------------------------------------------------------------------------

/**
 * Parâmetros visuais para cada [ValidationStatus].
 *
 * Centraliza cores, ícones, títulos e subtítulos para que [FeedbackScreen]
 * simplesmente consuma um [FeedbackTheme] sem duplicar lógica de condicionais.
 */
private data class FeedbackTheme(
    val containerColor: Color,
    val contentColor: Color,
    val iconBackgroundColor: Color,
    val iconTint: Color,
    val badgeContainerColor: Color,
    val badgeContentColor: Color,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val pulsing: Boolean = false
)

@Composable
private fun feedbackTheme(status: ValidationStatus): FeedbackTheme = when (status) {
    ValidationStatus.CORRECT -> FeedbackTheme(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        iconBackgroundColor = MaterialTheme.colorScheme.tertiary,
        iconTint = MaterialTheme.colorScheme.onTertiary,
        badgeContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
        badgeContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        icon = Icons.Filled.CheckCircle,
        title = "Correto! 🎉",
        subtitle = "Muito bem! Você formulou a pergunta certa.",
        pulsing = true
    )
    ValidationStatus.TOO_CLOSE -> FeedbackTheme(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        iconBackgroundColor = MaterialTheme.colorScheme.secondary,
        iconTint = MaterialTheme.colorScheme.onSecondary,
        badgeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        badgeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        icon = Icons.Filled.Star,
        title = "Quase lá! ⭐",
        subtitle = "Sua resposta está muito próxima. Tente reformular a pergunta com mais precisão."
    )
    ValidationStatus.PARTIAL -> FeedbackTheme(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        iconBackgroundColor = MaterialTheme.colorScheme.secondary,
        iconTint = MaterialTheme.colorScheme.onSecondary,
        badgeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        badgeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        icon = Icons.Filled.StarOutline,
        title = "Parcialmente correta",
        subtitle = "Você chegou perto! Veja as sugestões abaixo para melhorar."
    )
    ValidationStatus.INCORRECT -> FeedbackTheme(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        iconBackgroundColor = MaterialTheme.colorScheme.error,
        iconTint = MaterialTheme.colorScheme.onError,
        badgeContainerColor = MaterialTheme.colorScheme.errorContainer,
        badgeContentColor = MaterialTheme.colorScheme.onErrorContainer,
        icon = Icons.Filled.Close,
        title = "Incorreto",
        subtitle = "Não desanime! Revise as dicas e tente novamente."
    )
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun FeedbackScreenCorrectPreview() {
    MaterialTheme {
        FeedbackScreen(
            result = ValidationResult(
                status = ValidationStatus.CORRECT,
                score = 90,
                matchedAnswer = AcceptedAnswer(
                    id = 1L,
                    exerciseId = 1L,
                    questionText = "What is your name?",
                    isPrimary = true,
                    matchThreshold = 0.85f
                ),
                errorType = null,
                corrections = emptyList(),
                hints = emptyList()
            ),
            xpEarned = 100,
            attemptCount = 1,
            revealedHints = emptyList(),
            primaryAnswer = AcceptedAnswer(
                id = 1L,
                exerciseId = 1L,
                questionText = "What is your name?",
                isPrimary = true,
                matchThreshold = 0.85f
            ),
            onNext = {},
            onTryAgain = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedbackScreenTooClosePreview() {
    MaterialTheme {
        FeedbackScreen(
            result = ValidationResult(
                status = ValidationStatus.TOO_CLOSE,
                score = 45,
                matchedAnswer = null,
                errorType = ErrorType.SEMANTIC_MISMATCH,
                corrections = listOf(
                    Correction(
                        type = ErrorType.SEMANTIC_MISMATCH,
                        messageKey = "error_semantic",
                        suggestion = "Tente usar 'What' em vez de 'Which'."
                    )
                ),
                hints = emptyList()
            ),
            xpEarned = 25,
            attemptCount = 1,
            revealedHints = listOf(
                Hint(
                    id = 1L, exerciseId = 1L, hintOrder = 1,
                    hintText = "Use uma Wh-question.",
                    hintType = HintType.STRUCTURAL
                )
            ),
            primaryAnswer = null,
            onNext = {},
            onTryAgain = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedbackScreenIncorrectExhaustedPreview() {
    MaterialTheme {
        FeedbackScreen(
            result = ValidationResult(
                status = ValidationStatus.INCORRECT,
                score = 0,
                matchedAnswer = null,
                errorType = ErrorType.WRONG_QUESTION_TYPE,
                corrections = listOf(
                    Correction(
                        type = ErrorType.WRONG_QUESTION_TYPE,
                        messageKey = "error_wrong_type",
                        suggestion = "Este exercício espera uma Wh-question (What)."
                    )
                ),
                hints = emptyList()
            ),
            xpEarned = 0,
            attemptCount = 3,
            revealedHints = listOf(
                Hint(
                    id = 1L, exerciseId = 1L, hintOrder = 1,
                    hintText = "Use uma Wh-question.",
                    hintType = HintType.STRUCTURAL
                ),
                Hint(
                    id = 2L, exerciseId = 1L, hintOrder = 2,
                    hintText = "Qual palavra interrogativa pergunta sobre nomes?",
                    hintType = HintType.VOCABULARY
                ),
                Hint(
                    id = 3L, exerciseId = 1L, hintOrder = 3,
                    hintText = "What _____ your name?",
                    hintType = HintType.ANSWER
                )
            ),
            primaryAnswer = AcceptedAnswer(
                id = 1L,
                exerciseId = 1L,
                questionText = "What is your name?",
                isPrimary = true,
                matchThreshold = 0.85f
            ),
            onNext = {},
            onTryAgain = {}
        )
    }
}
