package com.englishlearning.app.ui.shadowing

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.englishlearning.app.domain.model.ShadowingFeedback
import com.englishlearning.app.ui.components.rememberTtsManager
import com.englishlearning.app.ui.learning.rememberSpeechLauncher

const val SHADOWING_ROUTE = "shadowing/{levelId}/{categoryId}"

@Composable
fun ShadowingScreen(
    levelId: Int,
    categoryId: Int,
    onNavigateBack: () -> Unit,
    viewModel: ShadowingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.startSession(levelId, categoryId, exerciseLimit = 10)
    }
    
    val (ttsManager, _) = rememberTtsManager()
    
    val launchSpeech = rememberSpeechLauncher { spokenText ->
        viewModel.onSpeechResult(spokenText)
    }
    
    // Observa eventos
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ShadowingEvent.PlayModelAudio -> {
                    val state = uiState as? ShadowingUiState.Active
                    state?.let {
                        ttsManager.speak(it.exercise.displayText, slow = true)
                        viewModel.onModelAudioPlayed()
                    }
                }
                is ShadowingEvent.StartRecording -> {
                    launchSpeech()
                }
                is ShadowingEvent.ShowResult -> {}
            }
        }
    }
    
    Scaffold(
        topBar = {
            ShadowingTopBar(
                onClose = onNavigateBack,
                progress = when (val state = uiState) {
                    is ShadowingUiState.Active -> 
                        (state.currentIndex + 1).toFloat() / state.totalExercises
                    else -> 0f
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ShadowingUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is ShadowingUiState.Active -> {
                ShadowingContent(
                    state = state,
                    onPlayModel = {
                        ttsManager.speak(state.exercise.displayText, slow = true)
                        viewModel.onModelAudioPlayed()
                    },
                    onRecord = viewModel::startRecording,
                    onTryAgain = viewModel::tryAgain,
                    onNext = viewModel::moveToNext,
                    modifier = Modifier.padding(padding)
                )
            }
            
            is ShadowingUiState.Complete -> {
                ShadowingComplete(
                    state = state,
                    onFinish = onNavigateBack,
                    modifier = Modifier.padding(padding)
                )
            }
            
            is ShadowingUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) { Text("Voltar") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShadowingTopBar(
    onClose: () -> Unit,
    progress: Float
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "🎤 Shadowing",
                    style = MaterialTheme.typography.titleMedium
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    strokeCap = StrokeCap.Round
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Fechar")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    )
}

@Composable
private fun ShadowingContent(
    state: ShadowingUiState.Active,
    onPlayModel: () -> Unit,
    onRecord: () -> Unit,
    onTryAgain: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasResult = state.lastResult != null
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Instruções
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "1️⃣ Ouça  →  2️⃣ Repita",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tente imitar a pronúncia e o ritmo",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        // Frase a ser repetida
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = state.exercise.displayText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Botões de ação
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão de ouvir
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onPlayModel
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "Ouvir",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Ouvir", style = MaterialTheme.typography.labelSmall)
            }
            
            // Botão de gravar (maior)
            if (!hasResult) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val isRecording = state.shadowingState.isRecording
                    val bgColor by animateColorAsState(
                        targetValue = if (isRecording)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.tertiary,
                        animationSpec = tween(300),
                        label = "recordColor"
                    )
                    
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = bgColor,
                        onClick = if (!isRecording) onRecord else ({})
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Gravar",
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRecording) "Gravando..." else "Repetir",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Resultado
        state.lastResult?.let { result ->
            ShadowingResultCard(result = result)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onTryAgain,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Repetir")
                }
                
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (state.currentIndex < state.totalExercises - 1)
                            "Próximo"
                        else
                            "Concluir"
                    )
                }
            }
        }
        
        // Score da sessão
        if (state.totalAttempts > 0) {
            Text(
                text = "Score médio: ${(state.sessionScore * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShadowingResultCard(result: com.englishlearning.app.domain.model.ShadowingResult) {
    val (bgColor, emoji, label) = when (result.feedback) {
        ShadowingFeedback.EXCELLENT -> Triple(
            Color(0xFF4CAF50),
            "🌟",
            "Excelente!"
        )
        ShadowingFeedback.GOOD -> Triple(
            Color(0xFF8BC34A),
            "👍",
            "Muito bom!"
        )
        ShadowingFeedback.FAIR -> Triple(
            Color(0xFFFFC107),
            "💪",
            "Bom, continue praticando!"
        )
        ShadowingFeedback.NEEDS_WORK -> Triple(
            Color(0xFFFF9800),
            "🔄",
            "Precisa praticar mais"
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, style = MaterialTheme.typography.displaySmall)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = bgColor
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Barra de similaridade
            LinearProgressIndicator(
                progress = { result.similarityScore },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = bgColor,
                trackColor = bgColor.copy(alpha = 0.3f)
            )
            
            Text(
                text = "${(result.similarityScore * 100).toInt()}% de similaridade",
                style = MaterialTheme.typography.labelSmall
            )
            
            // O que você disse
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Você disse:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "\"${result.userSpokenText}\"",
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Dicas de pronúncia
            if (result.pronunciationTips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                result.pronunciationTips.forEach { tip ->
                    Text(
                        text = "💡 $tip",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ShadowingComplete(
    state: ShadowingUiState.Complete,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val emoji = when {
            state.averageScore >= 0.85f -> "🏆"
            state.averageScore >= 0.70f -> "🎉"
            else -> "💪"
        }
        
        Text(emoji, style = MaterialTheme.typography.displayLarge)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Sessão de Shadowing Completa!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Score médio: ${(state.averageScore * 100).toInt()}%",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatRow("Total de frases", state.totalExercises.toString())
                StatRow("🌟 Excelente", state.excellentCount.toString())
                StatRow("👍 Bom", state.goodCount.toString())
                StatRow("🔄 Precisa praticar", state.needsWorkCount.toString())
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Concluir")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
