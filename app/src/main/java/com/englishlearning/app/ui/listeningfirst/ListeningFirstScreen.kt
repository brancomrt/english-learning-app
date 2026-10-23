package com.englishlearning.app.ui.listeningfirst

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.englishlearning.app.domain.model.ValidationStatus
import com.englishlearning.app.ui.components.rememberTtsManager
import com.englishlearning.app.ui.learning.rememberSpeechLauncher

const val LISTENING_FIRST_ROUTE = "listening_first/{levelId}/{categoryId}"

@Composable
fun ListeningFirstScreen(
    levelId: Int,
    categoryId: Int,
    onNavigateBack: () -> Unit,
    viewModel: ListeningFirstViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.startSession(levelId, categoryId, exerciseLimit = 10)
    }
    
    val (ttsManager, _) = rememberTtsManager()
    
    // Observa eventos para auto-play
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ListeningFirstEvent.PlayAudio -> {
                    val state = uiState as? ListeningFirstUiState.Active
                    state?.let {
                        ttsManager.speak(it.exercise.displayText)
                        viewModel.onAudioPlayed()
                    }
                }
                is ListeningFirstEvent.ShowFeedback -> {}
            }
        }
    }
    
    Scaffold(
        topBar = {
            ListeningFirstTopBar(
                onClose = onNavigateBack,
                progress = when (val state = uiState) {
                    is ListeningFirstUiState.Active -> 
                        (state.currentIndex + 1).toFloat() / state.totalExercises
                    else -> 0f
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ListeningFirstUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is ListeningFirstUiState.Active -> {
                ListeningFirstContent(
                    state = state,
                    onPlayAudio = {
                        ttsManager.speak(state.exercise.displayText)
                        viewModel.onAudioPlayed()
                    },
                    onRevealText = viewModel::revealText,
                    onSubmit = viewModel::submitAnswer,
                    onNext = viewModel::moveToNext,
                    modifier = Modifier.padding(padding)
                )
            }
            
            is ListeningFirstUiState.Complete -> {
                ListeningFirstComplete(
                    state = state,
                    onFinish = onNavigateBack,
                    modifier = Modifier.padding(padding)
                )
            }
            
            is ListeningFirstUiState.Error -> {
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
private fun ListeningFirstTopBar(
    onClose: () -> Unit,
    progress: Float
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "👂 Listening First",
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
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@Composable
private fun ListeningFirstContent(
    state: ListeningFirstUiState.Active,
    onPlayAudio: () -> Unit,
    onRevealText: () -> Unit,
    onSubmit: (String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var userInput by rememberSaveable { mutableStateOf("") }
    val hasResult = state.lastResult != null
    
    val launchSpeech = rememberSpeechLauncher { transcribed ->
        userInput = transcribed
    }
    
    // Reset input quando muda de exercício
    LaunchedEffect(state.currentIndex) {
        userInput = ""
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Card de instrução
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Headphones,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (state.listeningState.textRevealed)
                        "Texto revelado - confira sua resposta"
                    else
                        "Ouça e tente responder sem ver o texto",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Botão de áudio grande
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            onClick = onPlayAudio
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = "Ouvir",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        Text(
            text = "Reproduções: ${state.listeningState.playCount}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Texto (visível ou oculto)
        AnimatedVisibility(
            visible = state.listeningState.textRevealed,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
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
                        .padding(20.dp)
                )
            }
        }
        
        // Botão de revelar texto
        if (!state.listeningState.textRevealed && state.listeningState.canRevealText) {
            OutlinedButton(onClick = onRevealText) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Revelar texto")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Campo de entrada
        if (!hasResult) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Digite sua resposta") },
                    placeholder = { Text("O que você ouviu?") },
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (userInput.isNotBlank()) onSubmit(userInput) }
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = launchSpeech,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Falar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Button(
                onClick = { onSubmit(userInput) },
                enabled = userInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verificar")
            }
        }
        
        // Resultado
        state.lastResult?.let { result ->
            ResultCard(
                isCorrect = result.status == ValidationStatus.CORRECT,
                correctAnswer = state.exercise.acceptedAnswers.firstOrNull { it.isPrimary }?.questionText
                    ?: state.exercise.acceptedAnswers.firstOrNull()?.questionText ?: ""
            )
            
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
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
}

@Composable
private fun ResultCard(
    isCorrect: Boolean,
    correctAnswer: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isCorrect) "✅ Correto!" else "❌ Incorreto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (!isCorrect) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Resposta correta: $correctAnswer",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ListeningFirstComplete(
    state: ListeningFirstUiState.Complete,
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
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Sessão Completa!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatRow("Exercícios", "${state.correctCount}/${state.totalExercises}")
                StatRow("XP Ganho", "+${state.totalXp}")
                StatRow("Média de plays", "%.1f".format(state.averagePlayCount))
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
