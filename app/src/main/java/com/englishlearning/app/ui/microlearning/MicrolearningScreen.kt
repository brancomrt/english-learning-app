package com.englishlearning.app.ui.microlearning

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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.englishlearning.app.domain.model.MicrolearningProgress
import com.englishlearning.app.domain.model.ValidationStatus
import com.englishlearning.app.ui.components.rememberTtsManager
import com.englishlearning.app.ui.learning.rememberSpeechLauncher
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

const val MICROLEARNING_ROUTE = "microlearning"

@Composable
fun MicrolearningScreen(
    onNavigateBack: () -> Unit,
    viewModel: MicrolearningViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.startSession()
    }
    
    // Timer update
    LaunchedEffect(uiState) {
        while (uiState is MicrolearningUiState.Active) {
            delay(1000)
            viewModel.updateTimer()
        }
    }
    
    val (ttsManager, _) = rememberTtsManager()
    val launchSpeech = rememberSpeechLauncher { text ->
        viewModel.submitAnswer(text)
    }
    
    Scaffold(
        topBar = {
            MicrolearningTopBar(
                onClose = onNavigateBack,
                progress = when (val state = uiState) {
                    is MicrolearningUiState.Active -> state.progress
                    else -> null
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is MicrolearningUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is MicrolearningUiState.Active -> {
                MicrolearningContent(
                    state = state,
                    onSpeak = { ttsManager.speak(state.exercise.displayText) },
                    onSubmit = viewModel::submitAnswer,
                    onVoiceInput = launchSpeech,
                    onNext = viewModel::moveToNext,
                    modifier = Modifier.padding(padding)
                )
            }
            
            is MicrolearningUiState.Complete -> {
                MicrolearningComplete(
                    state = state,
                    onFinish = onNavigateBack,
                    modifier = Modifier.padding(padding)
                )
            }
            
            is MicrolearningUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MicrolearningTopBar(
    onClose: () -> Unit,
    progress: MicrolearningProgress?
) {
    TopAppBar(
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Quick Session",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    progress?.let {
                        Spacer(modifier = Modifier.width(12.dp))
                        TimerChip(progress = it)
                    }
                }
                
                progress?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    val progressValue by animateFloatAsState(
                        targetValue = it.progressPercent,
                        label = "progress"
                    )
                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Fechar")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    )
}

@Composable
private fun TimerChip(progress: MicrolearningProgress) {
    val remainingSeconds = progress.timeRemainingMs / 1000
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    
    val isLowTime = remainingSeconds < 30
    val timerColor = if (isLowTime)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.onSecondaryContainer
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isLowTime)
            MaterialTheme.colorScheme.errorContainer
        else
            MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = timerColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "%d:%02d".format(minutes, seconds),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = timerColor
            )
        }
    }
}

@Composable
private fun MicrolearningContent(
    state: MicrolearningUiState.Active,
    onSpeak: () -> Unit,
    onSubmit: (String) -> Unit,
    onVoiceInput: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var userInput by rememberSaveable { mutableStateOf("") }
    val hasResult = state.lastResult != null
    
    LaunchedEffect(state.exerciseIndex) {
        userInput = ""
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatChip(
                emoji = "✅",
                value = "${state.progress.correctCount}",
                label = "Acertos"
            )
            StatChip(
                emoji = "🔥",
                value = "${state.progress.currentStreak}",
                label = "Sequência"
            )
            StatChip(
                emoji = "📝",
                value = "${state.progress.exercisesCompleted}/${state.progress.targetExercises}",
                label = "Exercícios"
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Exercício
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            onClick = onSpeak
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Formule a pergunta:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.exercise.displayText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🔊 Toque para ouvir",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        // Input
        if (!hasResult) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Sua resposta") },
                    singleLine = false,
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (userInput.isNotBlank()) onSubmit(userInput) }
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = onVoiceInput,
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
        AnimatedVisibility(
            visible = hasResult,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            state.lastResult?.let { result ->
                val isCorrect = result.status == ValidationStatus.CORRECT
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isCorrect) "✅ Correto!" else "❌ Incorreto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (!isCorrect) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val correctAnswer = state.exercise.acceptedAnswers
                                .firstOrNull { it.isPrimary }?.questionText
                                ?: state.exercise.acceptedAnswers.firstOrNull()?.questionText
                            
                            correctAnswer?.let {
                                Text(
                                    text = "Resposta: $it",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Próximo")
                }
            }
        }
    }
}

@Composable
private fun StatChip(emoji: String, value: String, label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun MicrolearningComplete(
    state: MicrolearningUiState.Complete,
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
            state.accuracy >= 0.9f -> "🏆"
            state.accuracy >= 0.7f -> "🎉"
            state.accuracy >= 0.5f -> "👍"
            else -> "💪"
        }
        
        Text(emoji, style = MaterialTheme.typography.displayLarge)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (state.completedInTime) "Tempo!" else "Sessão Completa!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "${(state.accuracy * 100).toInt()}% de acertos",
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
                StatRow("Exercícios", "${state.correctCount}/${state.totalExercises}")
                StatRow("Melhor sequência", "${state.bestStreak} 🔥")
                StatRow("Tempo", formatTime(state.durationMs))
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

private fun formatTime(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%d:%02d".format(minutes, seconds)
}
