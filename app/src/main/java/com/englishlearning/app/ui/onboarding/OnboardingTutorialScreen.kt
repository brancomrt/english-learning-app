package com.englishlearning.app.ui.onboarding

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Constantes do exercício de exemplo
// ---------------------------------------------------------------------------

/** Resposta em inglês exibida durante o tutorial — conteúdo em inglês (Req. 14.8). */
private const val EXAMPLE_ANSWER = "My name is Maria."

/**
 * Validação simplificada para o onboarding: a pergunta deve começar com "what"
 * (case-insensitive). Não utiliza o ValidationEngine completo para não sobrecarregar
 * o tutorial com lógica de produção.
 */
private fun isAnswerAccepted(userQuestion: String): Boolean =
    userQuestion.trim().lowercase().startsWith("what")

// ---------------------------------------------------------------------------
// Estados internos do tutorial
// ---------------------------------------------------------------------------

/** Etapas do tutorial interativo (Req. 14.3). */
private enum class OnboardingStep { WELCOME, EXERCISE, COMPLETION }

// ---------------------------------------------------------------------------
// Tela principal
// ---------------------------------------------------------------------------

/**
 * Tela de tutorial interativo exibida apenas na primeira inicialização do app.
 *
 * Fluxo de três etapas (Req. 14.3):
 * 1. **Boas-vindas** — explica a mecânica em português.
 * 2. **Exercício** — exibe a resposta em inglês e captura a pergunta do usuário.
 * 3. **Conclusão** — parabeniza e oferece botão para a HomeScreen.
 *
 * Registro de conclusão (Req. 14.5 / 14.6 / 14.7):
 * - Ocorre **antes** de invocar [onTutorialCompleted] ou [onSkip],
 *   gravando `onboarding_completed = true` no SharedPreferences.
 *
 * Textos de interface em português; conteúdo do exercício em inglês (Req. 14.8).
 *
 * @param onTutorialCompleted Callback chamada após o usuário completar o exercício.
 * @param onSkip              Callback chamada quando o usuário pressiona "Pular Tutorial".
 * @param context             Contexto Android usado para acessar o SharedPreferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingTutorialScreen(
    onTutorialCompleted: () -> Unit,
    onSkip: () -> Unit,
    context: Context = LocalContext.current
) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.WELCOME) }
    var userInput by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }

    /**
     * Persiste o tutorial como concluído no SharedPreferences.
     * Chamado antes de qualquer navegação (Req. 14.5 / 14.6 / 14.7).
     */
    fun markOnboardingCompleted() {
        context
            .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_completed", true)
            .apply()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tutorial",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                // "Pular Tutorial" visível nas etapas WELCOME e EXERCISE (Req. 14.7)
                actions = {
                    if (step != OnboardingStep.COMPLETION) {
                        OutlinedButton(
                            onClick = {
                                markOnboardingCompleted() // Req. 14.7
                                onSkip()
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Pular Tutorial")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding_step_transition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { currentStep ->
            when (currentStep) {
                OnboardingStep.WELCOME -> WelcomeStep(
                    onNext = { step = OnboardingStep.EXERCISE }
                )

                OnboardingStep.EXERCISE -> ExerciseStep(
                    answerText = EXAMPLE_ANSWER,
                    userInput = userInput,
                    onUserInputChange = { text ->
                        userInput = text
                        showError = false
                    },
                    showError = showError,
                    onSubmit = {
                        if (isAnswerAccepted(userInput)) {
                            step = OnboardingStep.COMPLETION
                        } else {
                            showError = true
                        }
                    }
                )

                OnboardingStep.COMPLETION -> CompletionStep(
                    onGoHome = {
                        markOnboardingCompleted() // Req. 14.5 / 14.6
                        onTutorialCompleted()
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Etapa 1 — Boas-vindas
// ---------------------------------------------------------------------------

/**
 * Primeira etapa do tutorial: explica a mecânica em português (Req. 14.3 / 14.8).
 */
@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bem-vindo! 👋",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Como funciona?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Você verá uma resposta em inglês e deverá digitar " +
                        "a pergunta correspondente.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Por exemplo: se a resposta for \u201cMy name is Maria.\u201d, " +
                        "a pergunta esperada \u00e9 \u201cWhat is your name?\u201d",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Veja a resposta e formule a pergunta correspondente em inglês. " +
                "Vamos praticar com um exemplo!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Começar Exercício",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Etapa 2 — Exercício interativo
// ---------------------------------------------------------------------------

/**
 * Segunda etapa: exibe [answerText] em inglês, campo de entrada e botão de submissão.
 * Validação simplificada verifica se a resposta começa com "what" (Req. 14.4).
 */
@Composable
private fun ExerciseStep(
    answerText: String,
    userInput: String,
    onUserInputChange: (String) -> Unit,
    showError: Boolean,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Exercício de Exemplo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Veja a resposta abaixo e escreva a pergunta correspondente em inglês.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Resposta em inglês — conteúdo do exercício (Req. 14.8)
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
                    text = "Resposta:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = answerText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Qual é a pergunta?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo de entrada para a pergunta do usuário (Req. 14.4)
        OutlinedTextField(
            value = userInput,
            onValueChange = onUserInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Digite a pergunta em inglês") },
            placeholder = { Text("What is your name?") },
            isError = showError,
            supportingText = if (showError) {
                {
                    Text(
                        text = "Dica: a pergunta deve começar com \"What\".",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            singleLine = false,
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = userInput.isNotBlank()
        ) {
            Text(
                text = "Verificar Resposta",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Etapa 3 — Conclusão
// ---------------------------------------------------------------------------

/**
 * Terceira etapa: parabeniza o usuário e oferece acesso à HomeScreen (Req. 14.3).
 * A persistência de `onboarding_completed` é feita antes de chamar [onGoHome].
 */
@Composable
private fun CompletionStep(onGoHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Parabéns!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Você entendeu a mecânica. Vamos começar!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Agora você está pronto para praticar inglês de verdade. " +
                "Escolha um nível e comece a aprender!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onGoHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Ir para o Início",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "Onboarding — Boas-vindas")
@Composable
private fun OnboardingTutorialPreview() {
    MaterialTheme {
        OnboardingTutorialScreen(
            onTutorialCompleted = {},
            onSkip = {}
        )
    }
}
