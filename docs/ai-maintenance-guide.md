# Guia de Manutenção para IA

> Este arquivo é otimizado para que uma IA possa dar manutenção ao projeto
> sem contexto prévio. Leia este arquivo antes de qualquer intervenção.

## Contexto rápido

- App Android de aprendizado de inglês
- Mecânica invertida: usuário vê resposta → formula pergunta (e vice-versa)
- Kotlin + Jetpack Compose + Room + Hilt
- Arquitetura Clean: `data/` → `domain/` → `ui/`
- Single-user, 100% offline (exceto imagens de vocabulário)
- Nenhum backend, nenhum login, nenhum dado pessoal

## Arquivos críticos para entender o app

| Prioridade | Arquivo | Por que ler |
|-----------|---------|-------------|
| 1 | `domain/model/ContentModels.kt` | Modelos principais (Exercise, ExerciseMode, Level) |
| 2 | `ui/navigation/AppNavGraph.kt` | Todas as rotas e como o app navega |
| 3 | `domain/validation/QuestionValidationEngineImpl.kt` | Como respostas são validadas |
| 4 | `data/database/EnglishLearningDatabase.kt` | Schema do banco e migrações |
| 5 | `ui/learning/LearningSessionScreen.kt` | Tela principal (mais complexa) |
| 6 | `ui/learning/LearningViewModel.kt` | Lógica da sessão de exercícios |
| 7 | `data/seeder/DatabaseSeeder.kt` | Como o conteúdo é carregado |

## Regras que NUNCA devem ser quebradas

1. **IDs de exercícios são únicos globalmente** — level_1: 1-99, level_2: 101-199, etc.
2. **Migrações Room nunca são removidas** — sempre acumulativas
3. **userId é sempre 1** — app single-user por design
4. **Nenhum dado pessoal é coletado** — não adicionar analytics, login ou tracking
5. **`domain/` não importa nada de `android.*`** — camada pura Kotlin
6. **ViewModels nunca chamam DAOs diretamente** — sempre via UseCases ou Repositories

## Padrões de código que devem ser seguidos

### ViewModel
```kotlin
@HiltViewModel
class MinhaTelaViewModel @Inject constructor(
    private val algumUseCase: AlgumUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MinhaTelaUiState>(MinhaTelaUiState.Loading)
    val uiState: StateFlow<MinhaTelaUiState> = _uiState.asStateFlow()

    private val _events = Channel<MinhaTelaEvent>(Channel.BUFFERED)
    val events: Flow<MinhaTelaEvent> = _events.receiveAsFlow()
}
```

### Screen
```kotlin
@Composable
fun MinhaTela(
    navController: NavController,
    viewModel: MinhaTelaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
}
```

### Nova migração Room
```kotlin
// Em EnglishLearningDatabase.kt, dentro de companion object:
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tabela` ADD COLUMN `nova_coluna` TEXT")
    }
}
// Incrementar version = 5
// Em DatabaseModule.kt:
.addMigrations(..., EnglishLearningDatabase.MIGRATION_4_5)
```

## Como depurar problemas comuns

### "O exercício não valida minha resposta correta"
1. Ler `TextNormalizer.kt` — verificar se a contração está no mapa
2. Ler `GrammarParser.detectQuestionType()` — verificar se o tipo é detectado
3. Verificar `match_threshold` no JSON — se muito alto (>0.90), reduzir
4. Verificar se o `exercise_mode` está correto no JSON

### "Imagem não carrega no vocabulário"
1. Verificar se `image_url` está no formato Pexels correto:
   `https://images.pexels.com/photos/{ID}/pexels-photo-{ID}.jpeg?auto=compress&cs=tinysrgb&w=400&h=300&fit=crop`
2. Verificar se o ID do Pexels é válido
3. Verificar se `EnglishLearningApp.setupCoil()` está sendo chamado

### "Banco de dados não atualiza após mudança no JSON"
O `DatabaseSeeder` verifica `db_seeded = true` e pula o seeding.
Para forçar re-seeding: Configurações Android → Apps → English Learning App → Limpar dados.
Em desenvolvimento: incrementar a versão do banco e adicionar migração.

### "Tela em branco / app não inicia"
1. Verificar `AppNavGraph.kt` — algum composable lançando exceção na composição?
2. Verificar se todas as rotas estão registradas no `NavHost`
3. Verificar se os ViewModels não estão fazendo operações de IO na main thread

### "Build falha com erro de SSL"
Ambiente corporativo com proxy Zscaler.
Solução: importar cert Zscaler no JDK:
```bash
keytool -importcert -alias "zscaler-ca" -file cert.pem \
  -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt
```

## Localização dos arquivos de conteúdo

```
assets/content/
├── levels.json          ← 6 níveis (A1→C1)
├── categories.json      ← 18 categorias (3 por nível)
├── exercises/
│   ├── level_1.json     ← 32 exercícios (Beginner)
│   ├── level_2.json     ← 18 exercícios (Elementary / IT básico)
│   ├── level_3.json     ← 13 exercícios (Pre-Intermediate / Dev)
│   ├── level_4.json     ← 10 exercícios (Intermediate / DevOps)
│   ├── level_5.json     ← 8 exercícios (Upper-Intermediate / SRE)
│   └── level_6.json     ← 7 exercícios (Advanced / Liderança)
└── vocabulary/
    ├── level_1_vocab.json  ← 282 palavras
    ├── level_2_vocab.json  ← 193 palavras
    ├── level_3_vocab.json  ← 186 palavras
    ├── level_4_vocab.json  ← 120 palavras
    ├── level_5_vocab.json  ← 108 palavras
    └── level_6_vocab.json  ← 112 palavras
```

## Dependências externas (runtime)

| Serviço | URL | Uso | Fallback |
|---------|-----|-----|---------|
| Pexels CDN | images.pexels.com | Imagens do vocabulário | Ícone broken image |
| Google Speech Services | (nativo Android) | Reconhecimento de voz | Só digitação |
| Google TTS | (nativo Android) | Pronúncia das palavras | Botão desabilitado |

## Comandos úteis

```bash
# Build debug
gradle assembleDebug --no-daemon -Dorg.gradle.jvmargs="-Xmx2g"

# Testes unitários
gradle test --no-daemon

# Instalar via ADB (dispositivo conectado)
adb install app/build/outputs/apk/debug/app-debug.apk

# Ver logs do app no dispositivo
adb logcat -s "EnglishLearningApp:*" "AndroidRuntime:E"

# Limpar banco de dados do app (força re-seeding)
adb shell pm clear com.englishlearning.app
```

## Histórico de releases

| Versão | Destaque |
|--------|---------|
| v1.0.0 | Setup inicial — 133 arquivos, arquitetura completa |
| v1.0.1 | Ícone personalizado |
| v1.0.2 | Teoria gramatical (bottom sheet por exercício) |
| v1.0.3 | TTS (áudio) + vocabulário expandido para 186 palavras |
| v1.0.4–v1.0.6 | Correções de imagens (Unsplash → Pexels) |
| v1.0.7 | Modo adivinhação vocabulário + áudio automático ao virar |
| v1.0.8 | 1001 palavras no vocabulário |
| v1.0.9 | Imagens coerentes por palavra (126 IDs únicos) |
| v1.1.0 | Política de privacidade (LGPD) no app e HTML |

## Package ID

`com.englishlearning.app`

## Repositório

`https://github.com/brancomrt/english-learning-app`
Branch principal: `feature/english-learning-app`

---

## Créditos

Desenvolvido por **Alex Martins** em colaboração com **Kiro** (Amazon).
