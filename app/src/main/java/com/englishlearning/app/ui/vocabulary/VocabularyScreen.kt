package com.englishlearning.app.ui.vocabulary

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.englishlearning.app.data.database.entity.VocabularyCardEntity
import com.englishlearning.app.domain.model.Category
import com.englishlearning.app.domain.model.Level
import com.englishlearning.app.ui.components.TtsButton
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
                title = { Text("Adivinhe a palavra") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                LevelFilterRow(
                    levels = uiState.levels,
                    selectedLevelId = uiState.selectedLevelId,
                    onLevelSelected = viewModel::selectLevel
                )

                if (uiState.filteredCategories.isNotEmpty()) {
                    CategoryFilterRow(
                        categories = uiState.filteredCategories,
                        selectedCategoryId = uiState.selectedCategoryId,
                        onCategorySelected = viewModel::selectCategory
                    )
                }

                if (uiState.cards.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📚", style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Nenhum card nesta categoria ainda.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.cards, key = { it.id }) { card ->
                            VocabularyCard(
                                card = card,
                                isFlipped = uiState.flippedCardId == card.id,
                                onClick = { viewModel.flipCard(card.id) },
                                ttsManager = ttsManager,
                                ttsState = ttsState
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Filtros
// ---------------------------------------------------------------------------

@Composable
private fun LevelFilterRow(
    levels: List<Level>,
    selectedLevelId: Int,
    onLevelSelected: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(levels) { level ->
            val name = when (level.nameKey) {
                "level_beginner"           -> "A1"
                "level_elementary"         -> "A2"
                "level_pre_intermediate"   -> "A2+"
                "level_intermediate"       -> "B1"
                "level_upper_intermediate" -> "B2"
                "level_advanced"           -> "C1"
                else -> level.nameKey.replace("level_", "")
            }
            FilterChip(
                selected = level.id == selectedLevelId,
                onClick = { onLevelSelected(level.id) },
                label = { Text(name, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<Category>,
    selectedCategoryId: Int,
    onCategorySelected: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { cat ->
            val name = cat.nameKey
                .removePrefix("category_")
                .replace("_", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            FilterChip(
                selected = cat.id == selectedCategoryId,
                onClick = { onCategorySelected(cat.id) },
                label = { Text(name, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Card com flip — frente: só imagem (adivinhação) / verso: nome + áudio + definição
// ---------------------------------------------------------------------------

@Composable
private fun VocabularyCard(
    card: VocabularyCardEntity,
    isFlipped: Boolean,
    onClick: () -> Unit,
    ttsManager: TtsManager? = null,
    ttsState: TtsState = TtsState.IDLE
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "card_flip"
    )

    // Toca o áudio automaticamente ao revelar (quando rotation ultrapassa 90°)
    LaunchedEffect(isFlipped) {
        if (isFlipped) {
            kotlinx.coroutines.delay(450) // aguarda a animação terminar
            ttsManager?.speak(card.word)
        } else {
            ttsManager?.stop()
        }
    }

    val isFrontVisible = rotation <= 90f

    Box(
        modifier = Modifier
            .height(220.dp)
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(onClick = onClick)
    ) {
        if (isFrontVisible) {
            CardFront(card = card)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
            ) {
                CardBack(card = card, ttsManager = ttsManager, ttsState = ttsState)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Frente: só a imagem — sem nome (modo adivinhação)
// ---------------------------------------------------------------------------

@Composable
private fun CardFront(card: VocabularyCardEntity) {
    // image_url agora contém um emoji (ex: "☕", "🐛", "☁️")
    val emoji = card.imageUrl?.takeIf { it.isNotBlank() } ?: "❓"

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Emoji centralizado — grande e legível
            Text(
                text = emoji,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = androidx.compose.ui.unit.TextUnit(72f, androidx.compose.ui.unit.TextUnitType.Sp)
                ),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )

            // Dica discreta no rodapé
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Que palavra é essa? Toque para revelar",
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Verso: nome + áudio automático + definição PT-BR + exemplo
// ---------------------------------------------------------------------------

@Composable
private fun CardBack(
    card: VocabularyCardEntity,
    ttsManager: TtsManager? = null,
    ttsState: TtsState = TtsState.IDLE
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Palavra em inglês + botão TTS (replay manual)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = card.word,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                if (ttsManager != null) {
                    Spacer(Modifier.size(4.dp))
                    TtsButton(
                        text = card.word,
                        manager = ttsManager,
                        state = ttsState,
                        size = 22.dp
                    )
                }
            }

            // Definição PT-BR
            Text(
                text = card.definition,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Exemplo em inglês com botão TTS lento
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "\"${card.exampleSentence}\"",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (ttsManager != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TtsButton(
                            text = card.exampleSentence,
                            manager = ttsManager,
                            state = ttsState,
                            slow = true,
                            size = 16.dp
                        )
                        Text(
                            text = "ouvir devagar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Text(
                text = "toque para próxima",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.45f)
            )
        }
    }
}
