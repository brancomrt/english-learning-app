package com.englishlearning.app.ui.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.TextButton
import androidx.navigation.NavController
import com.englishlearning.app.data.repository.AppLanguage
import com.englishlearning.app.data.repository.SessionSettingsRepository
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Diálogo de confirmação de desbloqueio forçado
    uiState.confirmUnlockLevelId?.let { levelId ->
        val levelName = uiState.levelSettings.find { it.levelId == levelId }?.levelName ?: ""
        AlertDialog(
            onDismissRequest = { viewModel.cancelUnlock() },
            title = { Text("Desbloquear $levelName?") },
            text = {
                Text(
                    "Você vai pular direto para $levelName sem completar os níveis anteriores. " +
                    "Tem certeza? Essa ação não pode ser desfeita."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmUnlockLevel() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Desbloquear mesmo assim")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelUnlock() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vocabulário — cards por sessão",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Quantos cards de vocabulário aparecem em cada sessão de revisão.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    VocabLimitCard(
                        currentLimit = uiState.vocabLimit,
                        onLimitChange = { viewModel.updateVocabLimit(it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Exercícios — questões por sessão",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Defina quantas questões aparecerão em cada sessão por nível.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(uiState.levelSettings, key = { it.levelId }) { setting ->
                    LevelSettingCard(
                        setting = setting,
                        onLimitChange = { viewModel.updateLimit(setting.levelId, it) },
                        onForceUnlock = { viewModel.requestUnlockLevel(setting.levelId) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "⚡ Callan Drill",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Defina o intervalo de stages e quantas questões por sessão.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CallanDrillSettingsCard(
                        minStage   = uiState.callanMinStage,
                        maxStage   = uiState.callanMaxStage,
                        drillSize  = uiState.callanDrillSize,
                        onMinStageChange  = { viewModel.updateCallanMinStage(it) },
                        onMaxStageChange  = { viewModel.updateCallanMaxStage(it) },
                        onDrillSizeChange = { viewModel.updateCallanDrillSize(it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Idioma da interface / Interface language",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LanguageSelectorCard(
                        currentLang = uiState.appLanguage,
                        onSelect = { viewModel.setLanguage(it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.resetAllToDefault() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Restaurar padrões (${SessionSettingsRepository.DEFAULT_QUESTIONS} questões)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { navController.navigate("privacy_policy") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🔒 Política de Privacidade",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun VocabLimitCard(
    currentLimit: Int,
    onLimitChange: (Int) -> Unit
) {
    val isUnlimited = currentLimit <= 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📚 Vocabulário",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Todos os níveis e categorias",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (isUnlimited) "Sem limite" else "$currentLimit cards",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Slider(
                value = currentLimit.toFloat(),
                onValueChange = { onLimitChange(it.roundToInt()) },
                valueRange = com.englishlearning.app.data.repository.SessionSettingsRepository.MIN_VOCAB.toFloat()..
                             com.englishlearning.app.data.repository.SessionSettingsRepository.MAX_VOCAB.toFloat(),
                steps = ((com.englishlearning.app.data.repository.SessionSettingsRepository.MAX_VOCAB -
                         com.englishlearning.app.data.repository.SessionSettingsRepository.MIN_VOCAB) / 5) - 1,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${com.englishlearning.app.data.repository.SessionSettingsRepository.MIN_VOCAB}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${com.englishlearning.app.data.repository.SessionSettingsRepository.MAX_VOCAB}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LanguageSelectorCard(
    currentLang: AppLanguage,
    onSelect: (AppLanguage) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onSelect(AppLanguage.PT_BR) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentLang == AppLanguage.PT_BR)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (currentLang == AppLanguage.PT_BR)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { Text("🇧🇷 Português") }
            Button(
                onClick = { onSelect(AppLanguage.EN) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentLang == AppLanguage.EN)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (currentLang == AppLanguage.EN)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { Text("🇬🇧 English") }
        }
    }
}

@Composable
private fun CallanDrillSettingsCard(
    minStage: Int,
    maxStage: Int,
    drillSize: Int,
    onMinStageChange: (Int) -> Unit,
    onMaxStageChange: (Int) -> Unit,
    onDrillSizeChange: (Int) -> Unit
) {
    fun stageCefr(stage: Int) = when (stage) {
        1    -> "A1"
        2    -> "A1-A2"
        3    -> "A2"
        4    -> "A2-B1"
        5    -> "B1-B2"
        6    -> "B2"
        7    -> "B2"
        8    -> "B2"
        9    -> "B2+/C1"
        10   -> "C1"
        11   -> "C1"
        else -> "C1+"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

            // ── Stage mínimo ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Stage inicial",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Stage $minStage · ${stageCefr(minStage)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "→  Stage $maxStage · ${stageCefr(maxStage)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Mínimo (Stage $minStage)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = minStage.toFloat(),
                onValueChange = { onMinStageChange(it.roundToInt()) },
                valueRange = SessionSettingsRepository.MIN_STAGE.toFloat()..
                             SessionSettingsRepository.MAX_STAGE.toFloat(),
                steps = SessionSettingsRepository.MAX_STAGE - SessionSettingsRepository.MIN_STAGE - 1,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Máximo (Stage $maxStage)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = maxStage.toFloat(),
                onValueChange = { onMaxStageChange(it.roundToInt()) },
                valueRange = SessionSettingsRepository.MIN_STAGE.toFloat()..
                             SessionSettingsRepository.MAX_STAGE.toFloat(),
                steps = SessionSettingsRepository.MAX_STAGE - SessionSettingsRepository.MIN_STAGE - 1,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "1 (A1)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "12 (C1+)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Questões por sessão ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Questões por sessão",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$drillSize questões",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = drillSize.toFloat(),
                onValueChange = { onDrillSizeChange(it.roundToInt()) },
                valueRange = SessionSettingsRepository.MIN_DRILL_SIZE.toFloat()..
                             SessionSettingsRepository.MAX_DRILL_SIZE.toFloat(),
                steps = ((SessionSettingsRepository.MAX_DRILL_SIZE -
                          SessionSettingsRepository.MIN_DRILL_SIZE) / 5) - 1,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${SessionSettingsRepository.MIN_DRILL_SIZE}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${SessionSettingsRepository.MAX_DRILL_SIZE}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LevelSettingCard(
    setting: LevelQuestionSetting,
    onLimitChange: (Int) -> Unit,
    onForceUnlock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            // Cabeçalho: nome do nível + badge de status + botão desbloquear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = setting.levelName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (setting.isUnlocked) "✓ Desbloqueado" else "🔒 Bloqueado",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (setting.isUnlocked)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${setting.currentLimit} questões",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Botão desbloquear — só aparece se o nível ainda está bloqueado
                if (!setting.isUnlocked) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onForceUnlock,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 10.dp, vertical = 6.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = "Desbloquear nível",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pular",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Slider de número de questões
            Slider(
                value = setting.currentLimit.toFloat(),
                onValueChange = { onLimitChange(it.roundToInt()) },
                valueRange = setting.minLimit.toFloat()..setting.maxLimit.toFloat(),
                steps = ((setting.maxLimit - setting.minLimit) / 5) - 1,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${setting.minLimit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${setting.maxLimit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
