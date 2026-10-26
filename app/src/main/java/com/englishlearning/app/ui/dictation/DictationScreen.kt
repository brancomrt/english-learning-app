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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
 * Tela de Ditado no estilo Callan Method.
 * - Organizado por Stages (1-12)
 * - Textos completos (parágrafos)
 * - Reprodução de áudio (3x recomendado)
 * - Correção detalhada
 * - Possibilidade de refazer
 * - Marca como completo (100%)
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
                title = { 
                    Text(
                        if (uiState.isSelectingStage) "Ditado ✍️" 
                        else "Stage ${uiState.selectedStage} ✍️"
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isSelectingStage) {
                            onNavigateBack()
                        } else {
                            viewModel.backToStageSelection()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    if (!uiState.isSelectingStage) {
                        Text(
                            text = "${uiState.currentIndex + 1}/${uiState.totalExercises}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
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
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.isSelectingStage -> {
                    StageSelectionCard(
                        availableStages = uiState.availableStages,
                        onSelectStage = { viewModel.selectStage(it) }
                    )
                }
                uiState.isComplete -> {
                    SessionCompleteCard(
                        averageScore = uiState.averageScore,
                        totalExercises = uiState.totalExercises,
                        completedCount = uiState.completedInStage,
                        onContinue = onNavigateBack,
                        onSelectAnotherStage = { viewModel.backToStageSelection() }
                    )
                }
                uiState.currentExercise != null -> {
                    // Barra de progresso
                    LinearProgressIndicator(
                        progress = { uiState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Info do Stage
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "✅ ${uiState.completedInStage}/${uiState.totalExercises} completos",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF4CAF50)
                        )
                        if (uiState.currentExercise!!.completed) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Completo",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
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
                        onRetry = { viewModel.retryExercise() },
                        onHint = { viewModel.useHint() }
                    )
                }
                else -> {
                    EmptyMessage(
                        onBack = onNavigateBack,
                        onSelectStage = { viewModel.backToStageSelection() }
                    )
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
    onRetry: () -> Unit,
    onHint: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título e info do exercício
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    exercise.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Stage ${exercise.stage} • ${exercise.wordCount} palavras",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (exercise.grammarFocus != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "📚 ${exercise.grammarFocus}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
                
                Text(
                    "Recomendado: ouça 3 vezes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    "Reproduzido $playCount ${if (playCount == 1) "vez" else "vezes"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Campo de entrada ou resultado
        if (!isChecked) {
            // Campo de texto para digitar
            OutlinedTextField(
                value = userInput,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Digite o que você ouviu") },
                placeholder = { Text("Escreva o texto aqui...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                singleLine = false,
                minLines = 5
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
                correctText = exercise.text,
                onNext = onNext,
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun ResultCard(
    result: DictationResult,
    correctText: String,
    onNext: () -> Unit,
    onRetry: () -> Unit
) {
    val scoreColor = when {
        result.score == 100 -> Color(0xFF4CAF50)
        result.score >= 80 -> Color(0xFF8BC34A)
        result.score >= 60 -> Color(0xFFFF9800)
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${result.score}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
                if (result.score == 100) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Text(
                text = when {
                    result.score == 100 -> "Perfeito! 🎉 Marcado como completo!"
                    result.score >= 80 -> "Muito bom! 👍"
                    result.score >= 60 -> "Bom trabalho! 👏"
                    else -> "Continue praticando! 💪"
                },
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Texto correto
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        "✅ Texto correto:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = correctText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Sua resposta com erros destacados
            if (result.mistakes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            "❌ Sua resposta (${result.mistakes.size} erros):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        HighlightedText(
                            userText = result.userText,
                            mistakes = result.mistakes
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Lista de erros
                        result.mistakes.take(5).forEach { mistake ->
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
                        
                        if (result.mistakes.size > 5) {
                            Text(
                                "... e mais ${result.mistakes.size - 5} erros",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botões
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Refazer
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Replay, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refazer")
                }
                
                // Próximo
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Próximo")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.SkipNext, null)
                }
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
                        textDecoration = TextDecoration.LineThrough,
                        fontWeight = FontWeight.Bold
                    )) {
                        append(word)
                    }
                } else {
                    append(word)
                }
            }
        },
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun SessionCompleteCard(
    averageScore: Float,
    totalExercises: Int,
    completedCount: Int,
    onContinue: () -> Unit,
    onSelectAnotherStage: () -> Unit
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$completedCount",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text("100%", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onSelectAnotherStage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Escolher outro Stage")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Voltar ao Menu")
            }
        }
    }
}

@Composable
private fun EmptyMessage(
    onBack: () -> Unit,
    onSelectStage: () -> Unit
) {
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
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Tente selecionar outro Stage",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSelectStage) {
            Text("Escolher Stage")
        }
    }
}

@Composable
private fun StageSelectionCard(
    availableStages: List<Int>,
    onSelectStage: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✍️", fontSize = 64.sp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Callan Method Dictation",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Escolha o Stage para praticar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Text(
            "Ouça o texto 3 vezes e escreva o que ouviu",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Grid de Stages
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            availableStages.chunked(3).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    row.forEach { stage ->
                        Button(
                            onClick = { onSelectStage(stage) },
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    stage <= 3 -> Color(0xFF4CAF50) // Verde - Beginner
                                    stage <= 6 -> Color(0xFF8BC34A) // Verde claro - Elementary
                                    stage <= 9 -> Color(0xFFFF9800) // Laranja - Intermediate
                                    else -> Color(0xFFF44336) // Vermelho - Advanced
                                }
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$stage",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Legenda
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(Color(0xFF4CAF50), "1-3: Beginner")
            LegendItem(Color(0xFF8BC34A), "4-6: Elementary")
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(Color(0xFFFF9800), "7-9: Intermediate")
            LegendItem(Color(0xFFF44336), "10-12: Advanced")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
