package com.englishlearning.app.ui.vocabulary

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
                    Text(when (uiState) {
                        is VocabularyUiState.Reviewing -> "Vocabulário"
                        is VocabularyUiState.Done -> "Sessão concluída"
                        else -> "Vocabulário"
                    })
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
                    "level_beginner"           -> "A1"
                    "level_elementary"         -> "A2"
                    "level_pre_intermediate"   -> "A2+"
                    "level_intermediate"       -> "B1"
                    "level_upper_intermediate" -> "B2"
                    "level_advanced"           -> "C1"
                    else -> level.nameKey.replace("level_","")
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
            items(state.filteredCategories) { cat ->
                val name = cat.nameKey.removePrefix("category_")
                    .replace("_", " ")
                    .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    StatBox(label = "Total", value = (state.dueCount + state.newCount.coerceAtMost(20)).toString())
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
        animationSpec = tween(400),
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

        Spacer(Modifier.height(16.dp))

        // Card com flip
        Spacer(Modifier.weight(1f))

        // Card estilo baralho — tamanho fixo centralizado
        Box(
            modifier = Modifier
                .width(300.dp)
                .height(420.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
        ) {
            if (isFront) {
                VocabCardFront(card = state.card, onReveal = onReveal)
            } else {
                Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                    VocabCardBack(
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
private fun VocabCardFront(card: VocabularyCardEntity, onReveal: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onReveal
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Imagem local ocupa a parte superior
                VocabImage(
                    word = card.word,
                    imageUrl = card.imageUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                )
                
                // Área inferior com pergunta
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Que palavra é essa?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Toque para revelar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun VocabCardBack(
    card: VocabularyCardEntity,
    ttsManager: TtsManager?,
    ttsState: TtsState,
    onRate: (SrsRating) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Imagem menor no topo
            VocabImage(
                word = card.word,
                imageUrl = card.imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            )
            
            // Conteúdo principal
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Palavra + TTS
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = card.word,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (ttsManager != null) {
                            Spacer(Modifier.width(8.dp))
                            com.englishlearning.app.ui.components.TtsButton(
                                text = card.word,
                                manager = ttsManager,
                                state = ttsState,
                                size = 28.dp
                            )
                        }
                    }

                    // Definição
                    Text(
                        text = card.definition,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    // Exemplo
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "\"${card.exampleSentence}\"",
                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (ttsManager != null) {
                                Spacer(Modifier.height(4.dp))
                                com.englishlearning.app.ui.components.TtsButton(
                                    text = card.exampleSentence,
                                    manager = ttsManager,
                                    state = ttsState,
                                    slow = true,
                                    size = 20.dp
                                )
                            }
                        }
                    }
                }

                // Botões de avaliação FSRS
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Você lembrou?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RatingButton(
                            label = "Não",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onRate(SrsRating.AGAIN) }
                        )
                        RatingButton(
                            label = "Difícil",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onRate(SrsRating.HARD) }
                        )
                        RatingButton(
                            label = "Bom",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onRate(SrsRating.GOOD) }
                        )
                        RatingButton(
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
    }
}

@Composable
private fun RatingButton(
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
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
            Text(
                "✓",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
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

// ─── Componente de imagem local ───────────────────────────────────────────────

/**
 * Carrega uma imagem local dos assets baseada na palavra.
 * As imagens estão em assets/vocab_images/{word}.jpg
 */
@Composable
fun VocabImage(
    word: String,
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Determinar o nome do arquivo
    val filename = if (imageUrl != null && imageUrl.startsWith("local:")) {
        imageUrl.removePrefix("local:")
    } else {
        // Gerar nome do arquivo a partir da palavra
        word.lowercase()
            .replace(" ", "_")
            .replace("/", "_")
            .replace("\\", "_")
            .filter { it.isLetterOrDigit() || it == '_' }
            .take(50) + ".jpg"
    }
    
    // Carregar bitmap dos assets
    val bitmap = remember(filename) {
        runCatching {
            context.assets.open("vocab_images/$filename").use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }
    
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = word,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        // Fallback: placeholder colorido
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
