package com.englishlearning.app.ui.learning

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Encapsula o RecognizerIntent do Android para reconhecimento de voz em inglês.
 *
 * Usa o reconhecedor de voz do sistema (Google Speech Services) com locale
 * fixado em "en-US", independente do idioma do dispositivo.
 *
 * O texto transcrito é entregue via [onResult]. Em caso de erro ou cancelamento
 * [onResult] não é chamado.
 *
 * @param onResult Callback chamado com o texto transcrito quando reconhecimento sucede.
 * @return Lambda que lança o diálogo de reconhecimento de voz do sistema.
 */
@Composable
fun rememberSpeechLauncher(onResult: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            matches?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { onResult(it) }
        }
    }

    return remember(launcher) {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                // Reconhece em inglês independente do idioma do dispositivo
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                // Mensagem exibida no diálogo de reconhecimento
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale a pergunta em inglês")
                // Retorna múltiplas hipóteses (usamos só a primeira/melhor)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            launcher.launch(intent)
        }
    }
}
