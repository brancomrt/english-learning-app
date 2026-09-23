package com.englishlearning.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.englishlearning.app.ui.navigation.AppNavGraph
import com.englishlearning.app.ui.theme.EnglishLearningTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Única Activity do aplicativo.
 *
 * Responsabilidades:
 * - Inicializar o Compose com o tema do aplicativo ([EnglishLearningTheme]).
 * - Delegar toda a navegação ao [AppNavGraph].
 * - Habilitada para injeção de dependência via Hilt (@AndroidEntryPoint).
 *
 * Registrada no AndroidManifest.xml como activity launcher.
 *
 * Requisito 12.1: ponto de entrada único para toda a navegação do app.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Habilita edge-to-edge para aproveitamento total da tela em dispositivos modernos.
        enableEdgeToEdge()
        setContent {
            EnglishLearningTheme {
                // O AppNavGraph gerencia todas as rotas, lógica de primeiro acesso
                // (Onboarding) e exibição condicional da LessonScreen.
                AppNavGraph()
            }
        }
    }
}
