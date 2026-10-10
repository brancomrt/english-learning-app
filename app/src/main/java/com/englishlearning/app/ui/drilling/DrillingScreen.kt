package com.englishlearning.app.ui.drilling

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.englishlearning.app.ui.learning.rememberSpeechLauncher
import com.englishlearning.app.ui.components.TtsManager
import com.englishlearning.app.ui.components.TtsState
import com.englishlearning.app.ui.components.rememberTtsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrillingScreen(
    navController: NavController,
    viewModel: DrillingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val (ttsManager, ttsState) = rememberTtsManager()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚡ Drilling") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
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
                is DrillUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is DrillUiState.Question -> QuestionScreen(
                    state = state,
                    ttsManager = ttsManager,
                    ttsState = ttsState,
                    onSubmit = viewModel::submitAnswer,
                    onNext = viewModel::next
                )
                is DrillUiState.Done -> DoneScreen(
                    state = state,
                    onRestart = viewModel::restart,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun QuestionScreen(
    state: DrillUiState.Question,
    ttsManager: TtsManager?,
    ttsState: TtsState,
    onSubmit: (String) -> Unit,
    onNext: () -> Unit
) {
    var input by rememberSaveable(state.index) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val timerProgress by animateFloatAsState(
        targetValue = state.secondsLeft.toFloat() / DrillingViewModel.TIMER_SECONDS,
        animationSpec = tween(900),
        label = "timer"
    )

    // TTS automático ao aparecer a pergunta
    val promptText = state.drill.prompt
    LaunchedEffect(state.index) {
        kotlinx.coroutines.delay(200)
        ttsManager?.speak(promptText)
        if (!state.showResult) focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progresso e score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${state.index}/${state.total}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Timer circular
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { timerProgress },
                    modifier = Modifier.size(48.dp),
                    color = when {
                        state.secondsLeft > 8 -> MaterialTheme.colorScheme.primary
                        state.secondsLeft > 4 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 4.dp
                )
                Text(
                    "${state.secondsLeft}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            // Acertos
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    "✅ ${state.score}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Card da pergunta
        AnimatedContent(
            targetState = state.index,
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(100))
            },
            label = "question"
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "🗣️",
                        fontSize = 32.sp
                    )
                    Text(
                        text = promptText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                    // Mostra o foco gramatical como contexto
                    Text(
                        text = state.drill.grammar,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Resultado (visível após responder)
        if (state.showResult) {
            ResultCard(
                wasCorrect = state.wasCorrect,
                correctAnswer = state.correctAnswer,
                onNext = onNext
            )
        } else {
            // Campo de resposta + voz
            val launchSpeech = rememberSpeechLauncher { transcribed ->
                input = transcribed
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    label = { Text("Answer in English") },
                    placeholder = { Text("Full sentence!") },
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (input.isNotBlank()) onSubmit(input)
                        }
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { launchSpeech() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Voice",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Button(
                onClick = { if (input.isNotBlank()) onSubmit(input) },
                enabled = input.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check ✓", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ResultCard(
    wasCorrect: Boolean,
    correctAnswer: String,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (wasCorrect)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (wasCorrect) "✅ Correct!" else "❌ Answer:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (wasCorrect)
                    MaterialTheme.colorScheme.onTertiaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )
            if (!wasCorrect) {
                Text(
                    text = correctAnswer,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Repeat it out loud!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (wasCorrect)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Next →", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun DoneScreen(
    state: DrillUiState.Done,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            when {
                state.percentage >= 80 -> "🏆"
                state.percentage >= 50 -> "⭐"
                else -> "💪"
            },
            fontSize = 72.sp
        )
        Spacer(Modifier.height(16.dp))
        Text("Drill Complete!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "${state.score}/${state.total} correct (${state.percentage}%)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                state.percentage >= 80 -> "Excellent! 🏆 Excellent! 🎉"
                state.percentage >= 50 -> "Good progress! Keep practising."
                else -> "Keep going! Repetition is the key."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text("Drill Again", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back", style = MaterialTheme.typography.labelLarge)
        }
    }
}
