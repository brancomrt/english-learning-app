package com.englishlearning.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Item de navegação na barra inferior.
 *
 * @property route Rota de navegação.
 * @property label Label exibida abaixo do ícone.
 * @property selectedIcon Ícone quando selecionado.
 * @property unselectedIcon Ícone quando não selecionado.
 * @property badgeCount Número para exibir no badge (0 = sem badge).
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

/**
 * Itens padrão da navegação inferior.
 */
object BottomNavItems {
    val HOME = BottomNavItem(
        route = "home",
        label = "Início",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    val STUDY = BottomNavItem(
        route = "level_select",
        label = "Estudar",
        selectedIcon = Icons.Filled.School,
        unselectedIcon = Icons.Outlined.School
    )
    
    val VOCABULARY = BottomNavItem(
        route = "vocabulary",
        label = "Vocabulário",
        selectedIcon = Icons.Filled.Book,
        unselectedIcon = Icons.Outlined.Book
    )
    
    val PROGRESS = BottomNavItem(
        route = "progress",
        label = "Progresso",
        selectedIcon = Icons.Filled.TrendingUp,
        unselectedIcon = Icons.Outlined.TrendingUp
    )
    
    val items = listOf(HOME, STUDY, VOCABULARY, PROGRESS)
}

/**
 * Barra de navegação inferior do app.
 *
 * Fornece acesso rápido às principais seções:
 * - Início (Home)
 * - Estudar (Level Select)
 * - Vocabulário
 * - Progresso
 *
 * @param currentRoute Rota atual para destacar o item selecionado.
 * @param onNavigate Callback quando um item é clicado.
 * @param reviewBadgeCount Número de revisões pendentes (exibido no item Home).
 * @param modifier Modifier para customização.
 */
@Composable
fun AppBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    reviewBadgeCount: Int = 0
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        BottomNavItems.items.forEach { item ->
            val isSelected = currentRoute == item.route || 
                            (item.route == "home" && currentRoute == "home")
            
            // Mostra badge apenas no item Home se houver revisões
            val badgeCount = if (item.route == "home") reviewBadgeCount else 0
            
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    if (badgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ) {
                                    Text(
                                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}
