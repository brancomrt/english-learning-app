package com.englishlearning.app.ui.flashcard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.englishlearning.app.domain.model.Flashcard
import com.englishlearning.app.domain.model.FlashcardRating

/**
 * Tela de revisão de flashcards com animação de flip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    onNavigateBack: () -> Unit,
    viewModel: FlashcardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flashcards") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    // Contador de cards
                    Text(
                        text = "${uiState.reviewedCount}/${uiState.totalCards}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Barra de progresso
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Estatísticas da sessão
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip(
                    emoji = "✓",
                    count = uiState.correctCount,
                    label = "Corretos",
                    color = Color(0xFF4CAF50)
                )
                StatChip(
                    emoji = "📚",
                    count = uiState.dueToday,
                    label = "Pendentes",
                    color = MaterialTheme.colorScheme.secondary
                )
                StatChip(
                    emoji = "🆕",
                    count = uiState.newCards,
                    label = "Novos",
                    color = Color(0xFF2196F3)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }
                uiState.isComplete -> {
                    SessionCompleteCard(
                        reviewedCount = uiState.reviewedCount,
                        correctCount = uiState.correctCount,
                        onContinue = onNavigateBack
                    )
                }
                uiState.currentCard != null -> {
                    FlashcardContent(
                        card = uiState.currentCard!!,
                        isShowingAnswer = uiState.isShowingAnswer,
                        onFlip = { viewModel.flipCard() },
                        onRate = { rating -> viewModel.rateCard(rating) },
                        onPlayAudio = { viewModel.playAudio() }
                    )
                }
                else -> {
                    EmptyFlashcardsMessage(onNavigateBack)
                }
            }
        }
    }
}

@Composable
private fun FlashcardContent(
    card: Flashcard,
    isShowingAnswer: Boolean,
    onFlip: () -> Unit,
    onRate: (FlashcardRating) -> Unit,
    onPlayAudio: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isShowingAnswer) 180f else 0f,
        animationSpec = tween(400),
        label = "flip"
    )
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Card com flip animation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable { onFlip() }
        ) {
            if (rotation <= 90f) {
                // Frente do card (palavra)
                FlashcardFront(
                    word = card.word,
                    partOfSpeech = card.partOfSpeech?.label,
                    onPlayAudio = onPlayAudio
                )
            } else {
                // Verso do card (definição)
                FlashcardBack(
                    definition = card.definition,
                    example = card.exampleSentence,
                    pronunciation = card.pronunciation,
                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Botões de avaliação (só aparecem quando mostrando resposta)
        AnimatedVisibility(
            visible = isShowingAnswer,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            RatingButtons(onRate)
        }
        
        // Dica para virar o card
        if (!isShowingAnswer) {
            Text(
                text = "Toque para ver a resposta",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun FlashcardFront(
    word: String,
    partOfSpeech: String?,
    onPlayAudio: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            if (partOfSpeech != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "($partOfSpeech)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            IconButton(
                onClick = onPlayAudio,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(28.dp)
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Ouvir pronúncia",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun FlashcardBack(
    definition: String,
    example: String,
    pronunciation: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (pronunciation != null) {
                Text(
                    text = "/$pronunciation/",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Text(
                text = definition,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = "\"$example\"",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RatingButtons(onRate: (FlashcardRating) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FlashcardRating.entries.forEach { rating ->
            RatingButton(
                rating = rating,
                onClick = { onRate(rating) }
            )
        }
    }
}

@Composable
private fun RatingButton(
    rating: FlashcardRating,
    onClick: () -> Unit
) {
    val (containerColor, contentColor) = when (rating) {
        FlashcardRating.AGAIN -> Color(0xFFF44336) to Color.White
        FlashcardRating.HARD -> Color(0xFFFF9800) to Color.White
        FlashcardRating.GOOD -> Color(0xFF4CAF50) to Color.White
        FlashcardRating.EASY -> Color(0xFF2196F3) to Color.White
    }
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = Modifier.width(80.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(rating.emoji, fontSize = 20.sp)
            Text(
                rating.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatChip(
    emoji: String,
    count: Int,
    label: String,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun SessionCompleteCard(
    reviewedCount: Int,
    correctCount: Int,
    onContinue: () -> Unit
) {
    val accuracy = if (reviewedCount > 0) correctCount * 100 / reviewedCount else 0
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF4CAF50)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Sessão Completa! 🎉",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$reviewedCount", style = MaterialTheme.typography.headlineMedium)
                    Text("Revisados", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$accuracy%", style = MaterialTheme.typography.headlineMedium)
                    Text("Precisão", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = onContinue) {
                Text("Continuar")
            }
        }
    }
}

@Composable
private fun EmptyFlashcardsMessage(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📚", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Nenhum flashcard para revisar!",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Volte mais tarde ou adicione novos cards",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack) {
            Text("Voltar")
        }
    }
}
