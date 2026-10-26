package com.englishlearning.app.ui.dictation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.englishlearning.app.domain.model.DictationExercise
import com.englishlearning.app.domain.model.DictationResult
import com.englishlearning.app.domain.model.MistakeType

/**
 * Tela de Ditado (Spaced Writing).
 * O usuário ouve o áudio e escreve a frase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictationScreen(
    onNavigateBack: () -> Unit,
    viewModel: DictationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ditado ✍️") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    Text(
                        text = "${uiState.currentIndex + 1}/${uiState.totalExercises}",
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
                    .clip(RoundedCornerShape(4.dp))
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.isComplete -> {
                    SessionCompleteCard(
                        averageScore = uiState.averageScore,
                        totalExercises = uiState.totalExercises,
                        onContinue = onNavigateBack
                    )
                }
                uiState.currentExercise != null -> {
                    DictationContent(
                        exercise = uiState.currentExercise!!,
                        userInput = uiState.userInput,
                        isChecked = uiState.isChecked,
                        result = uiState.currentResult,
                        hintsUsed = uiState.hintsUsed,
                        playCount = uiState.playCount,
                        onInputChange = { viewModel.updateInput(it) },
                        onPlayAudio = { viewModel.playAudio() },
                        onPlaySlow = { viewModel.playAudioSlow() },
                        onSubmit = { 
                            keyboardController?.hide()
                            viewModel.checkAnswer() 
                        },
                        onNext = { viewModel.nextExercise() },
                        onHint = { viewModel.useHint() }
                    )
                }
                else -> {
                    EmptyMessage(onNavigateBack)
                }
            }
        }
    }
}

@Composable
private fun DictationContent(
    exercise: DictationExercise,
    userInput: String,
    isChecked: Boolean,
    result: DictationResult?,
    hintsUsed: Int,
    playCount: Int,
    onInputChange: (String) -> Unit,
    onPlayAudio: () -> Unit,
    onPlaySlow: () -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    onHint: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nível e dificuldade
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Nível ${exercise.levelId}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${exercise.difficulty.emoji} ${exercise.difficulty.label}",
                style = MaterialTheme.typography.labelMedium
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Área do áudio
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🎧 Ouça e escreva",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Botão de play normal
                    FloatingActionButton(
                        onClick = onPlayAudio,
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            "Ouvir",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    // Botão de play lento
                    FloatingActionButton(
                        onClick = onPlaySlow,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.SlowMotionVideo,
                            "Ouvir lento",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Reproduzido $playCount vezes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Campo de entrada ou resultado
        if (!isChecked) {
            // Campo de texto para digitar
            OutlinedTextField(
                value = userInput,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Digite o que você ouviu") },
                placeholder = { Text("Escreva a frase aqui...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                singleLine = false,
                minLines = 3
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botões de ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Dica
                Button(
                    onClick = onHint,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Lightbulb, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dica ($hintsUsed)")
                }
                
                // Verificar
                Button(
                    onClick = onSubmit,
                    enabled = userInput.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verificar")
                }
            }
        } else if (result != null) {
            // Mostrar resultado
            ResultCard(
                result = result,
                correctSentence = exercise.sentence,
                onNext = onNext
            )
        }
    }
}

@Composable
private fun ResultCard(
    result: DictationResult,
    correctSentence: String,
    onNext: () -> Unit
) {
    val scoreColor = when {
        result.score >= 90 -> Color(0xFF4CAF50)
        result.score >= 70 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = scoreColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pontuação
            Text(
                text = "${result.score}%",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
            
            Text(
                text = when {
                    result.score >= 90 -> "Excelente! 🎉"
                    result.score >= 70 -> "Bom trabalho! 👍"
                    else -> "Continue praticando! 💪"
                },
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Frase correta
            Text(
                "Resposta correta:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = correctSentence,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Sua resposta com erros destacados
            if (result.mistakes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Sua resposta:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HighlightedText(
                    userText = result.userText,
                    mistakes = result.mistakes
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Lista de erros
                result.mistakes.take(3).forEach { mistake ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "${mistake.type.emoji} ",
                            fontSize = 12.sp
                        )
                        Text(
                            text = when (mistake.type) {
                                MistakeType.SPELLING -> "\"${mistake.actual}\" → \"${mistake.expected}\""
                                MistakeType.MISSING_WORD -> "Faltou: \"${mistake.expected}\""
                                MistakeType.EXTRA_WORD -> "Extra: \"${mistake.actual}\""
                                else -> "${mistake.type.label}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.SkipNext, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Próximo")
            }
        }
    }
}

@Composable
private fun HighlightedText(
    userText: String,
    mistakes: List<com.englishlearning.app.domain.model.DictationMistake>
) {
    val words = userText.split(" ")
    val errorPositions = mistakes.map { it.position }.toSet()
    
    Text(
        text = buildAnnotatedString {
            words.forEachIndexed { index, word ->
                if (index > 0) append(" ")
                if (index in errorPositions) {
                    withStyle(SpanStyle(
                        color = Color(0xFFF44336),
                        textDecoration = TextDecoration.LineThrough
                    )) {
                        append(word)
                    }
                } else {
                    append(word)
                }
            }
        },
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(8.dp)
    )
}

@Composable
private fun SessionCompleteCard(
    averageScore: Float,
    totalExercises: Int,
    onContinue: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("✍️", fontSize = 64.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Sessão Completa!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$totalExercises",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Exercícios", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${averageScore.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Média", style = MaterialTheme.typography.bodySmall)
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
private fun EmptyMessage(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✍️", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Nenhum exercício disponível",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack) {
            Text("Voltar")
        }
    }
}
