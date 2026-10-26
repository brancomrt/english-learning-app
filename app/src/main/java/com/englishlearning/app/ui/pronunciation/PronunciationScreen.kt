package com.englishlearning.app.ui.pronunciation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.englishlearning.app.domain.model.PronunciationBreakdown
import com.englishlearning.app.domain.model.PronunciationResult
import com.englishlearning.app.domain.model.WordPronunciationAnalysis
import com.englishlearning.app.domain.model.WordStatus

/**
 * Tela de avaliação de pronúncia com Speech Recognition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PronunciationScreen(
    onNavigateBack: () -> Unit,
    viewModel: PronunciationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var hasPermission by remember { mutableStateOf(false) }
    
    // Solicitar permissão de microfone
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }
    
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pronúncia 🎤") },
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
        if (!hasPermission) {
            PermissionRequest(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                modifier = Modifier.padding(padding)
            )
        } else {
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
                    uiState.currentSentence != null -> {
                        PronunciationContent(
                            sentence = uiState.currentSentence!!,
                            isRecording = uiState.isRecording,
                            isAnalyzing = uiState.isAnalyzing,
                            result = uiState.currentResult,
                            onPlayReference = { viewModel.playReference() },
                            onStartRecording = { viewModel.startRecording() },
                            onStopRecording = { viewModel.stopRecording() },
                            onRetry = { viewModel.retry() },
                            onNext = { viewModel.nextExercise() }
                        )
                    }
                    else -> {
                        EmptyMessage(onNavigateBack)
                    }
                }
            }
        }
    }
}

@Composable
private fun PronunciationContent(
    sentence: String,
    isRecording: Boolean,
    isAnalyzing: Boolean,
    result: PronunciationResult?,
    onPlayReference: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Frase para pronunciar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Diga esta frase:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = sentence,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botão para ouvir referência
                Button(
                    onClick = onPlayReference,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ouvir pronúncia correta")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Área de gravação ou resultado
        if (result == null) {
            // Área de gravação
            RecordingArea(
                isRecording = isRecording,
                isAnalyzing = isAnalyzing,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording
            )
        } else {
            // Resultado
            ResultArea(
                result = result,
                expectedText = sentence,
                onRetry = onRetry,
                onNext = onNext
            )
        }
    }
}

@Composable
private fun RecordingArea(
    isRecording: Boolean,
    isAnalyzing: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Waveform animation quando gravando
        if (isRecording) {
            WaveformAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Botão de gravação
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            val scale by animateFloatAsState(
                targetValue = if (isRecording) 1.1f else 1f,
                animationSpec = tween(300),
                label = "scale"
            )
            
            FloatingActionButton(
                onClick = { 
                    if (isRecording) onStopRecording() else onStartRecording()
                },
                modifier = Modifier
                    .size((100 * scale).dp),
                containerColor = if (isRecording) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Parar" else "Gravar",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = when {
                isAnalyzing -> "Analisando..."
                isRecording -> "Gravando... Toque para parar"
                else -> "Toque para gravar"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WaveformAnimation(modifier: Modifier = Modifier) {
    var time by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            time += 0.1f
            kotlinx.coroutines.delay(50)
        }
    }
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        val path = Path()
        var x = 0f
        path.moveTo(x, centerY)
        
        while (x < width) {
            val y = centerY + kotlin.math.sin((x / 50 + time).toDouble()).toFloat() * 30 * 
                    kotlin.math.sin((x / 100 + time * 0.5).toDouble()).toFloat()
            path.lineTo(x, y)
            x += 5f
        }
        
        drawPath(
            path = path,
            color = Color(0xFF2196F3),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun ResultArea(
    result: PronunciationResult,
    expectedText: String,
    onRetry: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Score circular
        ScoreCircle(score = result.overallScore)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Breakdown de pontuações
        ScoreBreakdown(breakdown = result.breakdown)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Análise palavra por palavra
        if (result.wordAnalysis.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Análise detalhada:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    WordAnalysisRow(analysis = result.wordAnalysis)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Botões
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.Replay, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tentar novamente")
            }
            
            Button(onClick = onNext) {
                Text("Próximo")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.SkipNext, null)
            }
        }
    }
}

@Composable
private fun ScoreCircle(score: Int) {
    val color = when {
        score >= 90 -> Color(0xFF4CAF50)
        score >= 70 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    
    val progress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(1000),
        label = "progress"
    )
    
    Box(
        modifier = Modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Background circle
            drawCircle(
                color = color.copy(alpha = 0.2f),
                style = Stroke(width = 12.dp.toPx())
            )
            
            // Progress arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360 * progress,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = when {
                    score >= 90 -> "Excelente! 🎉"
                    score >= 70 -> "Bom! 👍"
                    else -> "Pratique mais 💪"
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ScoreBreakdown(breakdown: PronunciationBreakdown) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BreakdownItem("Precisão", breakdown.accuracyScore, "🎯")
        BreakdownItem("Fluência", breakdown.fluencyScore, "🌊")
        BreakdownItem("Completude", breakdown.completenessScore, "✓")
    }
}

@Composable
private fun BreakdownItem(label: String, score: Int, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(
            "$score%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WordAnalysisRow(analysis: List<WordPronunciationAnalysis>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(analysis) { wordAnalysis ->
            val backgroundColor = when (wordAnalysis.status) {
                WordStatus.CORRECT -> Color(0xFF4CAF50)
                WordStatus.MISPRONOUNCED -> Color(0xFFF44336)
                WordStatus.OMITTED -> Color(0xFFFF9800)
                WordStatus.INSERTED -> Color(0xFF2196F3)
            }
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = wordAnalysis.word,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = if (wordAnalysis.status == WordStatus.CORRECT) {
                        FontWeight.Normal
                    } else {
                        FontWeight.Bold
                    },
                    color = backgroundColor
                )
            }
        }
    }
}

@Composable
private fun PermissionRequest(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.MicOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Permissão de Microfone",
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Para avaliar sua pronúncia, precisamos acessar o microfone.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRequestPermission) {
            Text("Permitir Microfone")
        }
    }
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
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎤", fontSize = 64.sp)
            
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
                    Text("Frases", style = MaterialTheme.typography.bodySmall)
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
        Text("🎤", fontSize = 64.sp)
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
