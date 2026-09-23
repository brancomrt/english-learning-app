package com.englishlearning.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Política de Privacidade") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "English Learning App",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Última atualização: setembro de 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            PolicySection(
                title = "1. Visão Geral",
                body = "O English Learning App é um aplicativo de aprendizado de inglês que funciona " +
                    "inteiramente no seu dispositivo. Esta política explica de forma transparente " +
                    "quais dados são utilizados e como são tratados."
            )

            PolicySection(
                title = "2. Dados que NÃO coletamos",
                body = "Este aplicativo NÃO coleta, armazena, transmite ou compartilha:\n\n" +
                    "• Nome, e-mail, CPF ou qualquer dado pessoal identificável\n" +
                    "• Localização geográfica\n" +
                    "• Contatos do dispositivo\n" +
                    "• Histórico de navegação\n" +
                    "• Dados biométricos\n" +
                    "• Informações de pagamento\n" +
                    "• Dados de uso para fins analíticos ou publicitários"
            )

            PolicySection(
                title = "3. Dados armazenados localmente",
                body = "O aplicativo armazena no próprio dispositivo, exclusivamente para " +
                    "funcionamento do app:\n\n" +
                    "• Progresso nos exercícios (pontuação, XP, streak)\n" +
                    "• Conquistas desbloqueadas\n" +
                    "• Preferências de configuração (número de questões por nível)\n" +
                    "• Indicador de conclusão do tutorial inicial\n\n" +
                    "Esses dados são anônimos, não identificam você e nunca saem do dispositivo. " +
                    "São apagados ao desinstalar o aplicativo."
            )

            PolicySection(
                title = "4. Acesso à internet",
                body = "O aplicativo utiliza internet somente para carregar imagens ilustrativas " +
                    "do vocabulário, servidas pelo serviço Pexels (pexels.com). Nenhuma informação " +
                    "pessoal é enviada nessas requisições — apenas o endereço IP do seu dispositivo " +
                    "é registrado nos logs do CDN, o que é padrão de qualquer acesso à internet. " +
                    "Consulte a política de privacidade do Pexels em pexels.com/privacy-policy."
            )

            PolicySection(
                title = "5. Reconhecimento de voz",
                body = "O recurso de resposta por voz utiliza o serviço de reconhecimento de fala " +
                    "nativo do Android (Google Speech Services), já instalado no seu dispositivo. " +
                    "O aplicativo não grava, armazena nem processa o áudio — ele é enviado " +
                    "diretamente pelo sistema Android ao Google e convertido em texto. " +
                    "Consulte a política de privacidade do Google em policies.google.com/privacy."
            )

            PolicySection(
                title = "6. Permissões solicitadas",
                body = "• INTERNET — necessária para carregar imagens do vocabulário\n" +
                    "• RECORD_AUDIO — necessária para o recurso de resposta por voz\n\n" +
                    "Nenhuma outra permissão é solicitada."
            )

            PolicySection(
                title = "7. Crianças",
                body = "Este aplicativo não é direcionado a crianças menores de 13 anos e não " +
                    "coleta conscientemente dados de crianças. Por não haver coleta de dados " +
                    "pessoais, o aplicativo não apresenta riscos específicos para este público."
            )

            PolicySection(
                title = "8. Seus direitos (LGPD)",
                body = "Nos termos da Lei Geral de Proteção de Dados (Lei nº 13.709/2018), você " +
                    "tem direito ao acesso, correção, exclusão e portabilidade dos seus dados. " +
                    "Como todos os dados são armazenados localmente no seu dispositivo, você " +
                    "pode exercer esses direitos a qualquer momento:\n\n" +
                    "• Para apagar todos os dados: desinstale o aplicativo\n" +
                    "• Para apagar dados de progresso: Configurações do Android → Aplicativos → " +
                    "English Learning App → Limpar dados"
            )

            PolicySection(
                title = "9. Alterações nesta política",
                body = "Eventuais atualizações desta política serão refletidas na data de " +
                    "\"Última atualização\" no topo deste documento. Como não coletamos dados " +
                    "de contato, recomendamos consultar esta tela periodicamente."
            )

            PolicySection(
                title = "10. Contato",
                body = "Em caso de dúvidas sobre esta política de privacidade, entre em contato " +
                    "pelo repositório do projeto:\n\ngithub.com/brancomrt/english-learning-app"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Este aplicativo foi desenvolvido com respeito à sua privacidade. " +
                    "Nenhum dado seu sai do seu dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}
