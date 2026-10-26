package com.englishlearning.app.ui.grammar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.englishlearning.app.domain.model.GrammarExample
import com.englishlearning.app.domain.model.GrammarLesson

/**
 * Tela de lições de gramática.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarLessonScreen(
    onNavigateBack: () -> Unit,
    viewModel: GrammarLessonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gramática 📚") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("❌", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ocorreu um erro",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "Erro desconhecido",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            uiState.currentLesson != null -> {
                LessonContent(
                    lesson = uiState.currentLesson!!,
                    exampleIndex = uiState.currentExampleIndex,
                    onExampleViewed = { viewModel.viewExample(it) },
                    onComplete = { viewModel.completeLesson() },
                    onPlayAudio = { viewModel.playAudio(it) },
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                LessonList(
                    lessons = uiState.lessons,
                    onSelectLesson = { viewModel.selectLesson(it) },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun LessonList(
    lessons: List<GrammarLesson>,
    onSelectLesson: (GrammarLesson) -> Unit,
    modifier: Modifier = Modifier
) {
    if (lessons.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📚", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Nenhuma lição disponível ainda",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "As lições de gramática serão carregadas em breve.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(lessons, key = { it.id }) { lesson ->
            LessonCard(
                lesson = lesson,
                onClick = { onSelectLesson(lesson) }
            )
        }
    }
}

@Composable
private fun LessonCard(
    lesson: GrammarLesson,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (lesson.isCompleted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone/emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(lesson.iconEmoji, fontSize = 24.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = lesson.grammarTopic,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Status
            if (lesson.isCompleted) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Completo",
                    tint = Color(0xFF4CAF50)
                )
            } else if (lesson.isViewed) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Em progresso",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun LessonContent(
    lesson: GrammarLesson,
    exampleIndex: Int,
    onExampleViewed: (Int) -> Unit,
    onComplete: () -> Unit,
    onPlayAudio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFormulaExpanded by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabeçalho
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(lesson.iconEmoji, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = lesson.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Nível ${lesson.levelId} • ${lesson.grammarTopic}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Explicação principal
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = lesson.explanation,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        
        // Fórmula (se houver)
        if (lesson.formula != null) {
            item {
                Card(
                    modifier = Modifier.clickable { isFormulaExpanded = !isFormulaExpanded },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Estrutura",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                if (isFormulaExpanded) Icons.Default.ExpandLess 
                                else Icons.Default.ExpandMore,
                                null
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = isFormulaExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = lesson.formula!!,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Exemplos
        item {
            Text(
                "Exemplos Interativos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        items(lesson.examples.withIndex().toList()) { (index, example) ->
            ExampleCard(
                example = example,
                index = index,
                isActive = index == exampleIndex,
                isViewed = index < exampleIndex,
                onView = { onExampleViewed(index) },
                onPlayAudio = { onPlayAudio(example.sentence) }
            )
        }
        
        // Botão de conclusão
        item {
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                enabled = exampleIndex >= lesson.examples.size
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Concluir Lição")
            }
            
            if (exampleIndex < lesson.examples.size) {
                Text(
                    "Visualize todos os exemplos para concluir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ExampleCard(
    example: GrammarExample,
    index: Int,
    isActive: Boolean,
    isViewed: Boolean,
    onView: () -> Unit,
    onPlayAudio: () -> Unit
) {
    val backgroundColor = when {
        !example.isCorrect -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        isViewed -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        isActive -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isViewed) { onView() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de correto/incorreto
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!example.isCorrect) {
                        Icon(
                            Icons.Default.Close,
                            "Incorreto",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Incorreto",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (isViewed) {
                        Icon(
                            Icons.Default.Check,
                            "Visto",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Botão de áudio
                IconButton(onClick = onPlayAudio) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, "Ouvir")
                }
            }
            
            // Frase com destaque
            if (example.highlight != null) {
                val annotatedString = buildAnnotatedString {
                    val highlightStart = example.sentence.indexOf(example.highlight, ignoreCase = true)
                    if (highlightStart >= 0) {
                        append(example.sentence.substring(0, highlightStart))
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Bold,
                            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )) {
                            append(example.sentence.substring(
                                highlightStart, 
                                highlightStart + example.highlight.length
                            ))
                        }
                        append(example.sentence.substring(highlightStart + example.highlight.length))
                    } else {
                        append(example.sentence)
                    }
                }
                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(
                    text = example.sentence,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tradução
            Text(
                text = example.translation,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Nota adicional
            if (example.note != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = example.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
