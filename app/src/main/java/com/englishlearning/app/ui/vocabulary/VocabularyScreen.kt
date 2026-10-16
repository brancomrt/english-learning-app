package com.englishlearning.app.ui.vocabulary

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.englishlearning.app.data.database.entity.VocabularyCardEntity
import com.englishlearning.app.domain.gamification.SrsRating
import com.englishlearning.app.ui.components.TtsManager
import com.englishlearning.app.ui.components.TtsState
import com.englishlearning.app.ui.components.rememberTtsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    navController: NavController,
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val (ttsManager, ttsState) = rememberTtsManager()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState) {
                            is VocabularyUiState.Reviewing -> "Vocabulário"
                            is VocabularyUiState.Done -> "Sessão concluída"
                            else -> "Vocabulário"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState is VocabularyUiState.Reviewing)
                            viewModel.backToSelection()
                        else
                            navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is VocabularyUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is VocabularyUiState.Selecting -> {
                    SelectionScreen(
                        state = state,
                        onLevelSelected = viewModel::selectLevel,
                        onCategorySelected = viewModel::selectCategory,
                        onStart = viewModel::startSession
                    )
                }
                is VocabularyUiState.Reviewing -> {
                    ReviewScreen(
                        state = state,
                        ttsManager = ttsManager,
                        ttsState = ttsState,
                        onReveal = viewModel::revealCard,
                        onRate = viewModel::rateCard
                    )
                }
                is VocabularyUiState.Done -> {
                    DoneScreen(
                        state = state,
                        onRestart = viewModel::startSession,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

// ─── Tela de seleção ─────────────────────────────────────────────────────────

@Composable
private fun SelectionScreen(
    state: VocabularyUiState.Selecting,
    onLevelSelected: (Int) -> Unit,
    onCategorySelected: (Int) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Seletor de nível
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.levels) { level ->
                val name = when (level.nameKey) {
                    "level_beginner" -> "A1"
                    "level_elementary" -> "A2"
                    "level_pre_intermediate" -> "A2+"
                    "level_intermediate" -> "B1"
                    "level_upper_intermediate" -> "B2"
                    "level_advanced" -> "C1"
                    else -> level.nameKey.replace("level_", "")
                }
                FilterChip(
                    selected = level.id == state.selectedLevelId,
                    onClick = { onLevelSelected(level.id) },
                    label = { Text(name, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // Seletor de categoria
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Chip "Todas" como primeira opção
            item {
                FilterChip(
                    selected = state.selectedCategoryId == VocabularyViewModel.ALL_CATEGORIES_ID,
                    onClick = { onCategorySelected(VocabularyViewModel.ALL_CATEGORIES_ID) },
                    label = { Text("Todas", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                    )
                )
            }
            items(state.filteredCategories) { cat ->
                val name = categoryDisplayName(cat.nameKey)
                FilterChip(
                    selected = cat.id == state.selectedCategoryId,
                    onClick = { onCategorySelected(cat.id) },
                    label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Info da sessão
        Card(
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Sessão de hoje",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBox(label = "Revisar", value = state.dueCount.toString())
                    StatBox(label = "Novos", value = state.newCount.toString())
                    StatBox(
                        label = "Total",
                        value = (state.dueCount + state.newCount.coerceAtMost(20)).toString()
                    )
                }

                val total = state.dueCount + state.newCount.coerceAtMost(20)
                if (total == 0) {
                    Text(
                        "Nenhum card para revisar hoje nesta categoria!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        val total = state.dueCount + state.newCount.coerceAtMost(20)
        Button(
            onClick = onStart,
            enabled = total > 0,
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()
        ) {
            Text(
                text = if (total > 0) "Iniciar ($total cards)" else "Nada para revisar hoje",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "O algoritmo FSRS agendará a próxima revisão\nautomaticamente com base no seu desempenho.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Tela de revisão ─────────────────────────────────────────────────────────

@Composable
private fun ReviewScreen(
    state: VocabularyUiState.Reviewing,
    ttsManager: TtsManager?,
    ttsState: TtsState,
    onReveal: () -> Unit,
    onRate: (SrsRating) -> Unit
) {
    // TTS automático ao revelar
    LaunchedEffect(state.card.id, state.isRevealed) {
        if (state.isRevealed) {
            kotlinx.coroutines.delay(300)
            ttsManager?.speak(state.card.word)
        }
    }

    val rotation by animateFloatAsState(
        targetValue = if (state.isRevealed) 180f else 0f,
        // Revelar: animação suave de 400ms.
        // Fechar/próximo card: instantâneo — evita mostrar a resposta do card anterior.
        animationSpec = if (state.isRevealed) tween(400) else snap(),
        label = "flip"
    )
    val isFront = rotation <= 90f

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Barra de progresso
        val progress = if (state.total > 0) state.reviewed.toFloat() / state.total else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${state.reviewed} / ${state.total}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(Modifier.weight(1f))

        // Card com flip
        Box(
            modifier = Modifier
                .width(320.dp)
                .height(440.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
        ) {
            if (isFront) {
                CardFront(card = state.card, onReveal = onReveal)
            } else {
                Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                    CardBack(
                        card = state.card,
                        ttsManager = ttsManager,
                        ttsState = ttsState,
                        onRate = onRate
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun CardFront(card: VocabularyCardEntity, onReveal: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        onClick = onReveal
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Ícone de interrogação
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QuestionMark,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Definição como dica
                Text(
                    text = card.definition,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(Modifier.height(48.dp))

                Text(
                    "Qual é a palavra em inglês?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Toque para revelar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun CardBack(
    card: VocabularyCardEntity,
    ttsManager: TtsManager?,
    ttsState: TtsState,
    onRate: (SrsRating) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Palavra principal com áudio
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = card.word,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (ttsManager != null) {
                    Spacer(Modifier.width(12.dp))
                    IconButton(
                        onClick = { ttsManager.speak(card.word) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Ouvir pronúncia",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Definição
            Text(
                text = card.definition,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Frase de exemplo
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "\"${card.exampleSentence}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    if (ttsManager != null) {
                        Spacer(Modifier.height(8.dp))
                        IconButton(
                            onClick = { ttsManager.speak(card.exampleSentence, slow = true) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Ouvir frase",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Pergunta de avaliação
            Text(
                "Você lembrou?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(12.dp))

            // Botões de avaliação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RatingButton(
                    icon = Icons.Default.Close,
                    label = "Não",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { onRate(SrsRating.AGAIN) }
                )
                RatingButton(
                    icon = Icons.Default.Refresh,
                    label = "Difícil",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { onRate(SrsRating.HARD) }
                )
                RatingButton(
                    icon = Icons.Default.Check,
                    label = "Bom",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { onRate(SrsRating.GOOD) }
                )
                RatingButton(
                    icon = Icons.Default.Check,
                    label = "Fácil",
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { onRate(SrsRating.EASY) }
                )
            }
        }
    }
}

@Composable
private fun RatingButton(
    icon: ImageVector,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp
            )
        }
    }
}

// ─── Tela de conclusão ────────────────────────────────────────────────────────

@Composable
private fun DoneScreen(
    state: VocabularyUiState.Done,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ícone de sucesso
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Sessão concluída!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "${state.reviewed} cards revisados",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatChip(label = "Novos", value = state.newCards.toString())
            StatChip(label = "Revisões", value = state.reviewedCards.toString())
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "O algoritmo FSRS agendou a próxima revisão\nde cada palavra automaticamente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar ao início")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Revisar mais")
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

/** Converte nameKey da categoria para label legível. */
private fun categoryDisplayName(nameKey: String): String = when (nameKey) {
    // Level 1
    "category_daily_life"        -> "Daily Life"
    "category_work"              -> "Work"
    "category_travel"            -> "Travel"
    // Level 2 - TI
    "category_it_support"        -> "IT Support"
    "category_hardware_software" -> "Hardware & SW"
    "category_networking_basics" -> "Networking"
    // Level 2 - Cotidiano
    "category_food_cooking"      -> "Food & Cooking"
    "category_health_body"       -> "Health & Body"
    // Level 3 - TI
    "category_software_dev"      -> "Software Dev"
    "category_databases"         -> "Databases"
    "category_cloud_basics"      -> "Cloud"
    // Level 3 - Cotidiano
    "category_shopping_money"    -> "Shopping & Money"
    "category_sports_leisure"    -> "Sports & Leisure"
    // Level 4 - TI
    "category_devops"            -> "DevOps"
    "category_security"          -> "Security"
    "category_architecture"      -> "Architecture"
    // Level 4 - Cotidiano
    "category_home_housing"      -> "Home & Housing"
    "category_relationships"     -> "Relationships"
    // Level 5 - TI
    "category_agile_pm"          -> "Agile & PM"
    "category_sre_reliability"   -> "SRE"
    "category_microservices"     -> "Microservices"
    // Level 5 - Cotidiano
    "category_media_culture"     -> "Media & Culture"
    "category_environment"       -> "Environment"
    // Level 6 - TI
    "category_tech_leadership"   -> "Leadership"
    "category_system_design"     -> "System Design"
    "category_tech_writing"      -> "Tech Writing"
    // Level 6 - Cotidiano
    "category_society_politics"  -> "Society & Politics"
    "category_idioms_expressions"-> "Idioms"
    else -> nameKey.removePrefix("category_")
        .replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
