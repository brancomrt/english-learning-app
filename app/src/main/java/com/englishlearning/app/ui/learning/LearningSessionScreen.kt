package com.englishlearning.app.ui.learning

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightbulbCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.englishlearning.app.domain.model.Achievement
import com.englishlearning.app.domain.model.ExerciseMode
import com.englishlearning.app.ui.components.TtsButton
import com.englishlearning.app.ui.components.rememberTtsManager
import com.englishlearning.app.domain.model.Hint
import com.englishlearning.app.domain.model.HintType
import com.englishlearning.app.domain.model.QuestionType
import com.englishlearning.app.ui.components.AchievementUnlockDialog
import com.englishlearning.app.ui.components.LevelUnlockedDialog
import com.englishlearning.app.ui.components.parseLevelNumberFromKey

const val LEARNING_SESSION_ROUTE = "learning_session/{levelId}/{categoryId}"

@Composable
fun LearningSessionScreen(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    viewModel: LearningViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingAchievement by remember { mutableStateOf<Achievement?>(null) }

    LaunchedEffect(Unit) {
        val levelId = backStackEntry.arguments?.getInt("levelId") ?: 0
        val categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 0
        viewModel.startSession(levelId, categoryId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LearningEvent.ShowFeedback -> {}
                is LearningEvent.AchievementUnlocked -> pendingAchievement = event.achievement
                LearningEvent.SessionFinished -> {
                    navController.navigate("session_summary") {
                        popUpTo(LEARNING_SESSION_ROUTE) { inclusive = true }
                    }
                }
            }
        }
    }

    pendingAchievement?.let { achievement ->
        val levelNumber = parseLevelNumberFromKey(achievement.achievementKey)
        if (levelNumber != null) {
            LevelUnlockedDialog(levelNumber = levelNumber, onDismiss = { pendingAchievement = null })
        } else {
            AchievementUnlockDialog(achievement = achievement, onDismiss = { pendingAchievement = null })
        }
    }

    Scaffold { innerPadding ->
        when (val state = uiState) {
            is LearningUiState.Idle,
            is LearningUiState.Loading -> LoadingContent(modifier = Modifier.padding(innerPadding))

            is LearningUiState.ExerciseActive -> ExerciseContent(
                state = state,
                onSubmit = { answer -> viewModel.submitAnswer(answer) },
                onHint = { viewModel.requestHint() },
                onNext = { viewModel.moveToNext() },
                onClearResult = { viewModel.clearLastResult() },
                modifier = Modifier.padding(innerPadding)
            )

            is LearningUiState.SessionComplete -> LoadingContent(modifier = Modifier.padding(innerPadding))

            is LearningUiState.Error -> ErrorContent(
                message = state.message,
                onRetry = { navController.popBackStack() },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseContent(
    state: LearningUiState.ExerciseActive,
    onSubmit: (String) -> Unit,
    onHint: () -> Unit,
    onNext: () -> Unit,
    onClearResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exercise = state.exercise
    var questionInput by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val hasResult = state.lastValidationResult != null
    val canRequestMoreHints = state.revealedHints.size < exercise.hints.size

    // TTS para reprodução de áudio
    val (ttsManager, ttsState) = rememberTtsManager()

    // Controla exibição do bottom sheet de teoria gramatical
    var showTheorySheet by rememberSaveable { mutableStateOf(false) }

    // Lança o reconhecedor de voz do sistema em inglês
    val launchSpeech = rememberSpeechLauncher { transcribed ->
        questionInput = transcribed
    }

    LaunchedEffect(state.currentIndex) {
        questionInput = ""
        focusRequester.requestFocus()
    }

    val primaryAnswer = remember(exercise.id) {
        exercise.acceptedAnswers.firstOrNull { it.isPrimary }
            ?: exercise.acceptedAnswers.firstOrNull()
    }

    // Bottom sheet de teoria gramatical
    if (showTheorySheet) {
        GrammarTheorySheet(
            grammarFocus = exercise.grammarFocus,
            onDismiss = { showTheorySheet = false }
        )
    }

    Scaffold(
        topBar = {
            ExerciseTopBar(
                currentIndex = state.currentIndex + 1,
                totalExercises = state.totalExercises
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isAnswerMode = exercise.exerciseMode == ExerciseMode.ANSWER_FROM_QUESTION
                QuestionTypeChip(
                    questionType = exercise.questionType,
                    isAnswerMode = isAnswerMode
                )

                exercise.contextText?.takeIf { it.isNotBlank() }?.let { ctx ->
                    ContextCard(contextText = ctx)
                }

                AnswerCard(
                    displayText = exercise.displayText,
                    label = if (isAnswerMode) "Pergunta" else "Resposta",
                    onSpeak = { ttsManager.speak(exercise.displayText) },
                    ttsManager = ttsManager,
                    ttsState = ttsState
                )

                // Botão de teoria gramatical
                if (!hasResult) {
                    androidx.compose.material3.TextButton(
                        onClick = { showTheorySheet = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "📖 Ver teoria",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Campo de texto + botão de microfone lado a lado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val inputLabel = if (isAnswerMode) "Digite ou fale a resposta em inglês" else "Digite ou fale a pergunta em inglês"
                    val inputPlaceholder = if (isAnswerMode) "Ex.: Yes, I do. / No, I don't." else "Ex.: What is your name?"
                    OutlinedTextField(
                        value = questionInput,
                        onValueChange = { questionInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        label = { Text(inputLabel) },
                        placeholder = { Text(inputPlaceholder) },
                        enabled = !hasResult,
                        singleLine = false,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (questionInput.isNotBlank() && !hasResult) {
                                    onSubmit(questionInput)
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Botão de microfone — desativado durante feedback
                    IconButton(
                        onClick = launchSpeech,
                        enabled = !hasResult,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Responder por voz",
                            tint = if (!hasResult)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (!hasResult) {
                    SubmitHintButtons(
                        canSubmit = questionInput.isNotBlank(),
                        canShowHint = canRequestMoreHints,
                        onSubmit = { onSubmit(questionInput) },
                        onHint = onHint
                    )
                }

                // Dicas reveladas inline (antes de submeter a resposta)
                if (!hasResult && state.revealedHints.isNotEmpty()) {
                    InlineHintsSection(hints = state.revealedHints)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            state.lastValidationResult?.let { result ->
                FeedbackScreen(
                    result = result,
                    xpEarned = state.sessionXp,
                    attemptCount = state.attemptCount,
                    revealedHints = state.revealedHints,
                    primaryAnswer = primaryAnswer,
                    onNext = onNext,
                    onTryAgain = {
                        questionInput = ""
                        onClearResult()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseTopBar(currentIndex: Int, totalExercises: Int) {
    TopAppBar(
        title = {
            Text(
                text = "Exercício $currentIndex de $totalExercises",
                style = MaterialTheme.typography.titleMedium
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun QuestionTypeChip(questionType: QuestionType, isAnswerMode: Boolean = false) {
    FilterChip(
        selected = true,
        onClick = {},
        label = {
            val label = if (isAnswerMode) "Formule a resposta" else questionType.displayName()
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    )
}

@Composable
private fun ContextCard(contextText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Contexto",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp).padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = "Contexto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(text = contextText, style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic), color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun AnswerCard(
    displayText: String,
    label: String = "Resposta",
    onSpeak: (() -> Unit)? = null,
    ttsManager: com.englishlearning.app.ui.components.TtsManager? = null,
    ttsState: com.englishlearning.app.ui.components.TtsState = com.englishlearning.app.ui.components.TtsState.IDLE
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                if (ttsManager != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    TtsButton(
                        text = displayText,
                        manager = ttsManager,
                        state = ttsState,
                        size = 18.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayText,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SubmitHintButtons(
    canSubmit: Boolean,
    canShowHint: Boolean,
    onSubmit: () -> Unit,
    onHint: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onSubmit, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Verificar", style = MaterialTheme.typography.labelLarge)
        }
        if (canShowHint) {
            OutlinedButton(onClick = onHint, modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Filled.LightbulbCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Dica", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Algo deu errado", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) { Text(text = "Voltar") }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnswerCardPreview() {
    MaterialTheme { AnswerCard(displayText = "I work from home three days a week.") }
}

@Preview(showBackground = true)
@Composable
private fun ContextCardPreview() {
    MaterialTheme { ContextCard(contextText = "— I've been really busy lately.") }
}

@Preview(showBackground = true)
@Composable
private fun QuestionTypeChipPreview() {
    MaterialTheme { QuestionTypeChip(questionType = QuestionType.WH_HOW) }
}

// ---------------------------------------------------------------------------
// Inline hints — exibidas antes de submeter a resposta
// ---------------------------------------------------------------------------

/**
 * Exibe as dicas reveladas diretamente na tela do exercício,
 * antes de o usuário submeter a resposta.
 * Isso garante que o botão "Dica" tenha um efeito visível imediato.
 */
@Composable
private fun InlineHintsSection(hints: List<Hint>) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LightbulbCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Dicas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            hints.forEach { hint ->
                val isAnswer = hint.hintType == HintType.ANSWER
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = if (isAnswer)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${hint.hintOrder}.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = hint.hintText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAnswer)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
