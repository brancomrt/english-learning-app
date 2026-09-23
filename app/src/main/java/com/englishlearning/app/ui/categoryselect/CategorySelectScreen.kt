package com.englishlearning.app.ui.categoryselect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.englishlearning.app.domain.model.Category
import com.englishlearning.app.ui.lesson.LessonViewModel

// ---------------------------------------------------------------------------
// CategorySelectScreen — Ponto de entrada
// ---------------------------------------------------------------------------

/**
 * Tela de seleção de categoria temática dentro de um nível.
 *
 * Carrega as categorias do nível via [CategorySelectViewModel] e decide a rota de
 * navegação ao tocar em uma categoria:
 * - Se a MiniLesson da categoria ainda não foi vista → navega para LessonScreen.
 * - Se a MiniLesson já foi vista → navega diretamente para LearningSession.
 *
 * Requisito 7.1: exercícios organizados em categorias temáticas.
 * Requisito 7.2: exibir categorias disponíveis para o nível selecionado.
 * Requisito 15.1: verificar se a MiniLesson da categoria já foi vista.
 * Requisito 15.2: exibir LessonScreen quando a lição não foi vista.
 * Requisito 15.3: ir direto para a sessão quando a lição já foi vista.
 *
 * @param levelId               ID do nível selecionado na tela anterior.
 * @param onNavigateToSession   Callback para navegar à LearningSession (categoryId, questionType).
 * @param onNavigateToLesson    Callback para navegar à LessonScreen (categoryId, questionType).
 * @param onNavigateBack        Callback ao pressionar o botão Voltar.
 * @param viewModel             ViewModel injetado via Hilt.
 * @param lessonViewModel       LessonViewModel para verificar se a lição já foi vista.
 */
@Composable
fun CategorySelectScreen(
    levelId: Int,
    onNavigateToSession: (categoryId: Int, questionType: String) -> Unit = { _, _ -> },
    onNavigateToLesson: (categoryId: Int, questionType: String) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: CategorySelectViewModel = hiltViewModel(),
    lessonViewModel: LessonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Carrega as categorias assim que o levelId estiver disponível
    LaunchedEffect(levelId) {
        viewModel.loadCategoriesForLevel(levelId)
    }

    CategorySelectScaffold(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onCategorySelected = { category ->
            // Req. 15.1 / 15.2 / 15.3: decide a rota baseado no SharedPreferences da lição
            // questionType é vazio: Req. 15.12 garante que, sem mapeamento, a lição é pulada
            val questionType = ""
            if (lessonViewModel.shouldShowLesson(category.id)) {
                onNavigateToLesson(category.id, questionType)
            } else {
                onNavigateToSession(category.id, questionType)
            }
        },
        onRetry = viewModel::retry
    )
}

// ---------------------------------------------------------------------------
// CategorySelectScaffold — Layout com Scaffold
// ---------------------------------------------------------------------------

/**
 * Scaffold da tela de seleção de categoria com TopAppBar e conteúdo condicional.
 *
 * @param uiState               Estado atual da tela.
 * @param onNavigateBack        Callback ao pressionar o botão Voltar.
 * @param onCategorySelected    Callback ao selecionar uma categoria.
 * @param onRetry               Callback ao pressionar Tentar Novamente em caso de erro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectScaffold(
    uiState: CategorySelectUiState,
    onNavigateBack: () -> Unit,
    onCategorySelected: (Category) -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Selecionar Categoria",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> CategorySelectLoadingIndicator(
                modifier = Modifier.padding(innerPadding)
            )
            uiState.error != null -> CategorySelectErrorScreen(
                message = uiState.error,
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding)
            )
            uiState.categories.isEmpty() -> CategorySelectEmptyScreen(
                modifier = Modifier.padding(innerPadding)
            )
            else -> CategorySelectContent(
                categories = uiState.categories,
                onCategorySelected = onCategorySelected,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// CategorySelectContent — Grade de categorias
// ---------------------------------------------------------------------------

/**
 * Exibe as categorias do nível em uma grade de 2 colunas.
 *
 * Requisito 7.1: exercícios organizados em categorias temáticas.
 * Requisito 7.2: exibir as categorias disponíveis para o nível.
 *
 * @param categories        Lista de categorias a exibir.
 * @param onCategorySelected Callback ao tocar em uma categoria.
 * @param modifier          Modifier de layout.
 */
@Composable
fun CategorySelectContent(
    categories: List<Category>,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = categories, key = { it.id }) { category ->
            CategoryCard(
                category = category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// CategoryCard — Card de categoria individual
// ---------------------------------------------------------------------------

/**
 * Converte uma string hexadecimal de cor (ex: "#4CAF50") para [Color].
 * Retorna a cor primária do tema como fallback em caso de formato inválido.
 */
@Composable
private fun String.toComposeColor(): Color {
    return try {
        val hex = this.trimStart('#')
        val colorInt = hex.toLong(16)
        // Garante canal alpha = FF se a string não incluir alpha
        val colorLong = if (hex.length == 6) 0xFF000000L or colorInt else colorInt
        Color(colorLong.toInt())
    } catch (_: NumberFormatException) {
        MaterialTheme.colorScheme.primary
    }
}

/**
 * Card de categoria exibindo ícone (emoji) e nome em português dentro de um
 * fundo com a cor temática da categoria.
 *
 * Requisito 7.1 / 7.2: card com ícone e cor temática.
 * Requisito 12.4: nome da categoria em português brasileiro.
 *
 * @param category  Categoria a ser exibida.
 * @param onClick   Callback ao tocar no card.
 */
@Composable
fun CategoryCard(
    category: Category,
    onClick: () -> Unit
) {
    val themeColor = category.themeColorHex.toComposeColor()

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ícone da categoria: emoji em círculo com cor temática
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.icon.ifBlank { "📚" },
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // Nome da categoria em português (nameKey como chave de localização —
            // exibido diretamente pois o app usa strings pt-BR nos assets — Req. 12.4)
            Text(
                text = formatCategoryName(category.nameKey),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Linha de cor temática na base do card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(themeColor)
            )
        }
    }
}

/**
 * Converte o nameKey de uma categoria em rótulo legível em português.
 *
 * Exemplos de nameKey → exibição:
 * - "category_daily_life" → "Cotidiano"
 * - "category_work"       → "Trabalho"
 * - "category_travel"     → "Viagem"
 *
 * Requisito 12.4: interface em português brasileiro.
 */
internal fun formatCategoryName(nameKey: String): String {
    return when (nameKey) {
        "category_daily_life" -> "Cotidiano"
        "category_work"       -> "Trabalho"
        "category_travel"     -> "Viagem"
        "category_business"   -> "Negócios"
        "category_technology" -> "Tecnologia"
        "category_health"     -> "Saúde"
        "category_food"       -> "Alimentação"
        "category_education"  -> "Educação"
        "category_sports"     -> "Esportes"
        "category_culture"    -> "Cultura"
        "category_nature"     -> "Natureza"
        "category_science"    -> "Ciência"
        else -> nameKey
            .removePrefix("category_")
            .replace('_', ' ')
            .replaceFirstChar { it.uppercase() }
    }
}

// ---------------------------------------------------------------------------
// LoadingIndicator — Estado de carregamento
// ---------------------------------------------------------------------------

/**
 * Indicador de carregamento centralizado na tela de seleção de categoria.
 */
@Composable
fun CategorySelectLoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// ---------------------------------------------------------------------------
// EmptyScreen — Sem categorias disponíveis
// ---------------------------------------------------------------------------

/**
 * Tela exibida quando não há categorias disponíveis para o nível selecionado.
 */
@Composable
fun CategorySelectEmptyScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📂",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "Nenhuma categoria disponível",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Este nível ainda não possui categorias de exercícios.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
// ErrorScreen — Estado de erro (Req. 12.7)
// ---------------------------------------------------------------------------

/**
 * Tela de erro exibida quando o carregamento das categorias falha.
 *
 * Requisito 12.7: mensagem de erro amigável com opção de retry.
 *
 * @param message  Mensagem de erro a ser exibida.
 * @param onRetry  Callback ao pressionar Tentar Novamente.
 * @param modifier Modifier de layout.
 */
@Composable
fun CategorySelectErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "😕",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "Não foi possível carregar as categorias",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Tentar Novamente")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun CategorySelectContentPreview() {
    MaterialTheme {
        CategorySelectContent(
            categories = listOf(
                Category(
                    id = 1, levelId = 1, nameKey = "category_daily_life",
                    descriptionKey = "category_daily_life_desc",
                    icon = "🏠", themeColorHex = "#4CAF50", sortOrder = 1
                ),
                Category(
                    id = 2, levelId = 1, nameKey = "category_work",
                    descriptionKey = "category_work_desc",
                    icon = "💼", themeColorHex = "#2196F3", sortOrder = 2
                ),
                Category(
                    id = 3, levelId = 1, nameKey = "category_travel",
                    descriptionKey = "category_travel_desc",
                    icon = "✈️", themeColorHex = "#FF9800", sortOrder = 3
                ),
                Category(
                    id = 4, levelId = 1, nameKey = "category_health",
                    descriptionKey = "category_health_desc",
                    icon = "❤️", themeColorHex = "#F44336", sortOrder = 4
                )
            ),
            onCategorySelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryCardPreview() {
    MaterialTheme {
        CategoryCard(
            category = Category(
                id = 1, levelId = 1, nameKey = "category_daily_life",
                descriptionKey = "category_daily_life_desc",
                icon = "🏠", themeColorHex = "#4CAF50", sortOrder = 1
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategorySelectLoadingPreview() {
    MaterialTheme {
        CategorySelectLoadingIndicator()
    }
}

@Preview(showBackground = true)
@Composable
private fun CategorySelectEmptyPreview() {
    MaterialTheme {
        CategorySelectEmptyScreen()
    }
}
