# English Learning App — Documentação Técnica Completa

> Documentação gerada para permitir manutenção por IA ou desenvolvedor sem contexto prévio.

## Índice

1. [Visão Geral](#1-visão-geral)
2. [Stack e Dependências](#2-stack-e-dependências)
3. [Arquitetura](#3-arquitetura)
4. [Estrutura de Diretórios](#4-estrutura-de-diretórios)
5. [Camada de Dados](#5-camada-de-dados)
6. [Camada de Domínio](#6-camada-de-domínio)
7. [Camada de UI](#7-camada-de-ui)
8. [Banco de Dados](#8-banco-de-dados)
9. [Conteúdo (JSON Assets)](#9-conteúdo-json-assets)
10. [Navegação](#10-navegação)
11. [Motor de Validação](#11-motor-de-validação)
12. [Sistema SRS / FSRS](#12-sistema-srs--fsrs)
13. [Gamificação](#13-gamificação)
14. [TTS e Reconhecimento de Voz](#14-tts-e-reconhecimento-de-voz)
15. [Injeção de Dependência](#15-injeção-de-dependência)
16. [Testes](#16-testes)
17. [Build e Release](#17-build-e-release)
18. [Guia de Manutenção](#18-guia-de-manutenção)

---

## 1. Visão Geral

**English Learning App** é um aplicativo Android de aprendizado de inglês com mecânica
invertida: o usuário vê uma **resposta** em inglês e deve formular a **pergunta** correspondente.
O app também inclui modo direto (pergunta → resposta), vocabulário visual com flip cards,
reconhecimento de voz, TTS e repetição espaçada (SRS/FSRS).

| Atributo | Valor |
|----------|-------|
| Plataforma | Android (API 26+ / Android 8.0+) |
| Linguagem | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material Design 3 |
| Banco de dados | Room 2.6.1 (SQLite) |
| Arquitetura | Clean Architecture (Data → Domain → UI) |
| DI | Hilt 2.54 |
| Usuário | Single-user (userId fixo = 1) |
| Internet | Apenas para imagens de vocabulário (Pexels CDN) |
| Privacidade | Nenhum dado pessoal coletado ou transmitido |

### Funcionalidades

- 6 níveis de dificuldade: Beginner A1 → Advanced C1
- 88 exercícios (formulação de perguntas e respostas)
- 1001 palavras no vocabulário com imagens
- Dois modos de exercício: `QUESTION_FROM_ANSWER` e `ANSWER_FROM_QUESTION`
- Sistema de dicas progressivas (3 níveis por exercício)
- Motor de validação semântica (Jaccard + bigrams)
- Teoria gramatical inline (bottom sheet por exercício)
- Reconhecimento de voz em inglês (Android SpeechRecognizer)
- TTS automático no vocabulário (Android TextToSpeech)
- SRS com algoritmo FSRS-4.5
- Gamificação: XP, streak diário, conquistas
- Configurações: limite de questões por nível, desbloqueio forçado de níveis
- Política de privacidade (LGPD)

---

## 2. Stack e Dependências

```toml
# gradle/libs.versions.toml

agp = "8.7.3"              # Android Gradle Plugin
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"     # Kotlin Symbol Processing (Room + Hilt)
composeBom = "2024.12.01"  # Jetpack Compose BOM
room = "2.6.1"
hilt = "2.54"
coil = "2.7.0"             # Carregamento de imagens
kotlinxSerialization = "1.7.3"
kotest = "5.9.1"           # Property-based testing
```

### Dependências principais

| Biblioteca | Uso |
|-----------|-----|
| Jetpack Compose + Material3 | UI declarativa |
| Navigation Compose 2.8.5 | Navegação entre telas |
| Room 2.6.1 | Persistência local (SQLite) |
| Hilt 2.54 | Injeção de dependência |
| Coil 2.7.0 | Carregamento de imagens (vocabulário) |
| kotlinx.serialization | Parse de JSON dos assets |
| Coroutines 1.9.0 | Assincronismo |
| Kotest 5.9.1 | Testes baseados em propriedades |

---

## 3. Arquitetura

O app segue **Clean Architecture** com três camadas e dependências apontando para dentro:

```
┌─────────────────────────────────────────────────────┐
│  UI Layer  (Compose Screens + ViewModels)            │
│  Depende de: Domain                                  │
├─────────────────────────────────────────────────────┤
│  Domain Layer  (UseCases + Models + Interfaces)      │
│  Depende de: NADA (puro Kotlin)                      │
├─────────────────────────────────────────────────────┤
│  Data Layer  (Room + Repositories + Seeder)          │
│  Depende de: Domain (implementa interfaces)          │
└─────────────────────────────────────────────────────┘
```

### Padrões usados

- **MVVM**: ViewModels expõem `StateFlow<UiState>` e `Channel<Event>`
- **Repository Pattern**: `ContentRepository`, `ProgressRepository`, `SrsRepository`
- **Use Cases**: uma classe por operação de negócio
- **Unidirectional Data Flow**: UI → ViewModel → UseCase → Repository → DB

---

## 4. Estrutura de Diretórios

```
english-learning-app/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── assets/content/
│   │   ├── levels.json
│   │   ├── categories.json
│   │   ├── exercises/
│   │   │   ├── level_1.json … level_6.json
│   │   └── vocabulary/
│   │       ├── level_1_vocab.json … level_6_vocab.json
│   ├── java/com/englishlearning/app/
│   │   ├── EnglishLearningApp.kt          ← Application class (Hilt + Coil setup)
│   │   ├── data/
│   │   │   ├── database/
│   │   │   │   ├── EnglishLearningDatabase.kt  ← Room DB v4
│   │   │   │   ├── entity/                     ← 12 entidades Room
│   │   │   │   ├── dao/                        ← 11 DAOs
│   │   │   │   └── converter/Converters.kt
│   │   │   ├── repository/                     ← Implementações dos repos
│   │   │   └── seeder/DatabaseSeeder.kt        ← Popula DB na 1ª execução
│   │   ├── di/
│   │   │   ├── DatabaseModule.kt
│   │   │   └── RepositoryModule.kt
│   │   ├── domain/
│   │   │   ├── model/                          ← Data classes puras
│   │   │   ├── repository/                     ← Interfaces dos repos
│   │   │   ├── usecase/                        ← 8 use cases
│   │   │   ├── validation/                     ← Motor de validação
│   │   │   ├── gamification/                   ← XP, streak, FSRS
│   │   │   └── lesson/                         ← Conteúdo de mini-lições
│   │   └── ui/
│   │       ├── navigation/AppNavGraph.kt       ← Grafo de navegação
│   │       ├── home/                           ← Dashboard
│   │       ├── learning/                       ← Tela principal de exercício
│   │       ├── vocabulary/                     ← Vocabulário com flip cards
│   │       ├── settings/                       ← Configurações + privacidade
│   │       ├── srs/                            ← Revisão por repetição espaçada
│   │       ├── progress/                       ← Progresso e conquistas
│   │       └── components/                     ← TtsManager, TtsButton, dialogs
│   └── res/
│       ├── drawable/                           ← Ícone vetorial (ic_launcher)
│       └── mipmap-*/                           ← Ícones PNG por densidade
├── docs/
│   ├── README.md                              ← Este arquivo
│   ├── privacy-policy.html                    ← Política para Play Store
│   └── architecture.md                        ← Diagramas detalhados
└── gradle/libs.versions.toml
```

---

## 5. Camada de Dados

### 5.1 Entidades Room

| Entidade | Tabela | Descrição |
|----------|--------|-----------|
| `LevelEntity` | `levels` | Níveis A1→C1 com flag `is_unlocked` |
| `CategoryEntity` | `categories` | Categorias temáticas por nível |
| `ExerciseEntity` | `exercises` | Exercícios com `exercise_mode` e `prompt_text` |
| `AcceptedAnswerEntity` | `accepted_answers` | Respostas aceitas com `match_threshold` |
| `HintEntity` | `hints` | Dicas progressivas por exercício |
| `UserProfileEntity` | `user_profile` | Perfil single-user (id=1 fixo) |
| `UserProgressEntity` | `user_progress` | Progresso por nível/categoria |
| `AttemptEntity` | `attempts` | Histórico de tentativas |
| `StreakRecordEntity` | `streak_records` | Streak diário |
| `AchievementEntity` | `achievements` | Conquistas desbloqueadas |
| `SrsCardEntity` | `srs_cards` | Cards FSRS com parâmetros de agendamento |
| `VocabularyCardEntity` | `vocabulary_cards` | Cards de vocabulário com `image_url` |

### 5.2 Migrações Room

```
v1 → v2: Adiciona tabelas srs_cards e vocabulary_cards
v2 → v3: Adiciona colunas prompt_text e exercise_mode em exercises
v3 → v4: Adiciona coluna image_url em vocabulary_cards
```

**IMPORTANTE:** ao adicionar novos campos em entidades existentes, sempre criar uma
nova migração em `EnglishLearningDatabase.kt` e registrá-la em `DatabaseModule.kt`.
Nunca use `fallbackToDestructiveMigration` em produção sem migração explícita.

### 5.3 DatabaseSeeder

Roda na inicialização do app via `EnglishLearningApp.seedDatabaseAsync()`.
Verifica a flag `db_seeded` no SharedPreferences antes de executar (idempotente).
Lê os arquivos JSON de `assets/content/` e popula todas as tabelas.

### 5.4 Repositórios

| Interface (domain) | Implementação (data) | Responsabilidade |
|-------------------|---------------------|-----------------|
| `ContentRepository` | `ContentRepositoryImpl` | Exercícios, níveis, categorias |
| `ProgressRepository` | `ProgressRepositoryImpl` | Progresso, tentativas, streak |
| `SrsRepository` | `SrsRepositoryImpl` | Cards SRS, agendamento FSRS |

### 5.5 SessionSettingsRepository

Não implementa uma interface de domínio. É um repositório de preferências do usuário
armazenado em `SharedPreferences` (chave `app_prefs`):
- `questions_limit_level_N` — limite de questões por sessão para o nível N
- `onboarding_completed` — flag de conclusão do onboarding
- `db_seeded` — flag de seeding do banco

---

## 6. Camada de Domínio

### 6.1 Modelos principais

```kotlin
// ContentModels.kt
enum class ExerciseMode {
    QUESTION_FROM_ANSWER,  // Mostra resposta → usuário formula pergunta
    ANSWER_FROM_QUESTION   // Mostra pergunta → usuário formula resposta
}

data class Exercise(
    val id: Long,
    val answerText: String,    // Exibido no modo QUESTION_FROM_ANSWER
    val promptText: String?,   // Exibido no modo ANSWER_FROM_QUESTION
    val exerciseMode: ExerciseMode,
    val questionType: QuestionType,
    val grammarFocus: String,
    val acceptedAnswers: List<AcceptedAnswer>,
    val hints: List<Hint>
) {
    val displayText: String  // Computed: answerText ou promptText dependendo do modo
}
```

### 6.2 Use Cases

| Use Case | Entrada | Saída | Descrição |
|----------|---------|-------|-----------|
| `LoadExercisesUseCase` | levelId, categoryId | `Result<List<Exercise>>` | Carrega e embaralha até o limite configurado |
| `ValidateAnswerUseCase` | userInput, exercise | `ValidationResult` | Valida via motor semântico |
| `UpdateProgressUseCase` | userId, levelId, categoryId, AttemptResult | Unit | Persiste tentativa e atualiza streak |
| `CheckAchievementsUseCase` | — | `List<Achievement>` | Verifica e desbloqueia conquistas |
| `GetAvailableLevelsUseCase` | — | `List<Level>` | Níveis desbloqueados |
| `GetUserStatsUseCase` | userId | `UserStats` | XP, streak, progresso |
| `CheckDailyStreakUseCase` | userId | Unit | Verifica/reseta streak diário |
| `LoadExercisesUseCase` | levelId, categoryId | `Result<List<Exercise>>` | Filtra e embaralha pelo limite do usuário |

### 6.3 QuestionType

```kotlin
enum class QuestionType {
    WH_WHAT, WH_WHERE, WH_WHEN, WH_WHO, WH_WHY,
    WH_HOW, WH_HOW_MUCH, WH_HOW_MANY, WH_WHICH,
    YES_NO, TAG, INDIRECT
}
```

---

## 7. Camada de UI

### 7.1 Telas e ViewModels

| Tela | ViewModel | Rota | Descrição |
|------|-----------|------|-----------|
| `HomeScreen` | `HomeViewModel` | `home` | Dashboard com stats e botões |
| `LevelSelectScreen` | `LevelSelectViewModel` | `level_select` | Seleção de nível |
| `CategorySelectScreen` | `CategorySelectViewModel` | `category_select/{levelId}` | Seleção de categoria |
| `LessonScreen` | `LessonViewModel` | `lesson/{levelId}/{categoryId}/{questionType}` | Mini-lição gramatical |
| `LearningSessionScreen` | `LearningViewModel` | `learning_session/{levelId}/{categoryId}` | Exercício principal |
| `SessionSummaryScreen` | `LearningViewModel` | `session_summary` | Resumo da sessão |
| `ProgressScreen` | `ProgressViewModel` | `progress` | Progresso e conquistas |
| `SrsReviewScreen` | `SrsReviewViewModel` | `srs_review` | Revisão SRS |
| `VocabularyScreen` | `VocabularyViewModel` | `vocabulary` | Vocabulário com flip cards |
| `SettingsScreen` | `SettingsViewModel` | `settings` | Configurações |
| `PrivacyPolicyScreen` | — | `privacy_policy` | Política de privacidade |
| `OnboardingTutorialScreen` | — | `onboarding` | Tutorial inicial |

### 7.2 Padrão UiState + Events

Todos os ViewModels seguem o padrão:

```kotlin
// Estado exposto como StateFlow
val uiState: StateFlow<XyzUiState>

// Eventos one-shot via Channel (nunca perdidos)
val events: Flow<XyzEvent>
```

### 7.3 LearningViewModel — fluxo principal

```
startSession(levelId, categoryId)
  → LoadExercisesUseCase → embaralha até o limite
  → uiState = ExerciseActive

submitAnswer(userInput)
  → ValidateAnswerUseCase (semântico)
  → UpdateProgressUseCase (persiste tentativa)
  → CheckAchievementsUseCase
  → events.send(ShowFeedback)
  → uiState.copy(lastValidationResult = result)

moveToNext()
  → currentIndex++ ou SessionComplete
  → events.send(SessionFinished) → navega para session_summary
```

### 7.4 Componentes reutilizáveis

| Componente | Arquivo | Descrição |
|-----------|---------|-----------|
| `TtsManager` | `components/TtsManager.kt` | Wrapper Android TextToSpeech |
| `TtsButton` | `components/TtsButton.kt` | Botão 🔊 com animação de pulso |
| `rememberTtsManager()` | `TtsManager.kt` | Composable que gerencia o lifecycle do TTS |
| `rememberSpeechLauncher()` | `SpeechRecognizerHelper.kt` | Lança RecognizerIntent em inglês |
| `GrammarTheorySheet` | `learning/GrammarTheorySheet.kt` | Bottom sheet com teoria gramatical |
| `AchievementUnlockDialog` | `components/` | Dialog de conquista desbloqueada |
| `LevelUnlockedDialog` | `components/` | Dialog de nível desbloqueado |

---

## 8. Banco de Dados

### 8.1 Schema resumido

```sql
levels          (id, name_key, is_unlocked, sort_order, ...)
categories      (id, level_id, name_key, icon_key, color_key, sort_order)
exercises       (id, level_id, category_id, answer_text, prompt_text,
                 context_text, question_type, exercise_mode,
                 grammar_focus, difficulty_score, xp_reward, sort_order)
accepted_answers(id, exercise_id, question_text, is_primary, match_threshold)
hints           (id, exercise_id, hint_order, hint_text, hint_type)
user_profile    (id=1, display_name, total_xp, current_level_id, ...)
user_progress   (id, user_id, level_id, category_id, completed_exercises, ...)
attempts        (id, user_id, exercise_id, user_answer, validation_status,
                 score, xp_earned, attempted_at)
streak_records  (id, user_id, current_streak, longest_streak, last_activity_date)
achievements    (id, user_id, achievement_key, unlocked_at, is_seen)
srs_cards       (id, user_id, card_type, ref_id, stability, difficulty,
                 elapsed_days, scheduled_days, reps, lapses, state,
                 due_date, last_review_date)
vocabulary_cards(id, word, definition, example_sentence, level_id,
                 category_id, audio_path, image_url)
```

### 8.2 Como adicionar uma nova versão do banco

1. Incrementar `version` em `EnglishLearningDatabase.kt`
2. Adicionar `MIGRATION_N_M` como object dentro de `companion object`
3. Registrar em `DatabaseModule.kt` → `.addMigrations(..., MIGRATION_N_M)`
4. Nunca remover migrações antigas

---

## 9. Conteúdo (JSON Assets)

### 9.1 Estrutura de exercício

```json
{
  "id": 1,
  "level_id": 1,
  "category_id": 1,
  "answer_text": "My name is Maria.",
  "prompt_text": null,
  "context_text": null,
  "question_type": "WH_WHAT",
  "exercise_mode": "QUESTION_FROM_ANSWER",
  "grammar_focus": "present simple - to be",
  "difficulty_score": 1,
  "xp_reward": 10,
  "sort_order": 1,
  "accepted_answers": [
    { "id": 1, "question_text": "What is your name?", "is_primary": true, "match_threshold": 0.85 },
    { "id": 2, "question_text": "What's your name?", "is_primary": false, "match_threshold": 0.80 }
  ],
  "hints": [
    { "id": 1, "hint_order": 1, "hint_text": "Use a Wh-question.", "hint_type": "structural" },
    { "id": 2, "hint_order": 2, "hint_text": "Start with 'What'.", "hint_type": "vocabulary" },
    { "id": 3, "hint_order": 3, "hint_text": "What is / What's your...?", "hint_type": "answer" }
  ]
}
```

### 9.2 Modos de exercício

| `exercise_mode` | `prompt_text` | `answer_text` | O que o usuário vê | O que deve formular |
|----------------|--------------|--------------|---------------------|---------------------|
| `QUESTION_FROM_ANSWER` | null | frase declarativa | A resposta | A pergunta |
| `ANSWER_FROM_QUESTION` | pergunta | possíveis respostas | A pergunta | A resposta |

### 9.3 Tipos de dica (`hint_type`)

| Tipo | Descrição |
|------|-----------|
| `structural` | Explica a estrutura gramatical |
| `vocabulary` | Sugere palavras-chave |
| `answer` | Revela parte da resposta (só após as outras) |

### 9.4 Estrutura de vocabulário

```json
{
  "cards": [
    {
      "word": "coffee",
      "definition": "café (bebida)",
      "example_sentence": "I drink two cups of coffee every morning.",
      "level_id": 1,
      "category_id": 1,
      "image_url": "https://images.pexels.com/photos/302899/..."
    }
  ]
}
```

### 9.5 IDs para novos conteúdos

Ao adicionar exercícios, garantir IDs únicos globalmente:
- Level 1: ex 1–99, ans 1–999, hints 1–999
- Level 2: ex 101–199, ans 101–999, hints 101–999
- Level 3: ex 201–299, ans 201–999, hints 201–999
- Level 4: ex 301–399
- Level 5: ex 401–499
- Level 6: ex 501–599

---

## 10. Navegação

### 10.1 Grafo de navegação

```
Splash
  ↓ (primeiro acesso)
Onboarding → Home
  ↓ (acessos subsequentes)
Home
  ├── LevelSelect
  │     └── CategorySelect
  │           ├── Lesson (se lição não vista)
  │           │     └── LearningSession
  │           └── LearningSession
  │                 └── SessionSummary
  ├── Progress
  ├── Settings
  │     └── PrivacyPolicy
  ├── SrsReview
  └── Vocabulary
```

### 10.2 Regras de navegação

- `Splash` → verifica `onboarding_completed` no SharedPreferences
- `CategorySelect` → verifica `LessonViewModel.shouldShowLesson()` antes de navegar
- `SessionSummary` → sempre navega de volta para `Home` (limpa back stack)

---

## 11. Motor de Validação

### 11.1 Pipeline (modo QUESTION_FROM_ANSWER)

```
userInput
  → TextNormalizer.normalize()
      - lowercase
      - expand contractions (what's → what is, don't → do not, etc.)
      - remove pontuação
  → GrammarParser.detectQuestionType()
      - compara com QuestionType esperado
      - se diferente → ValidationStatus.INCORRECT (WRONG_QUESTION_TYPE)
  → GrammarParser.checkStructure()
      - verifica presença de verbo auxiliar, ordem de palavras
  → SemanticMatcher.score(normalized, normalizedAccepted)
      - Jaccard similarity (60%) + bigram similarity (40%)
      - score ≥ threshold → CORRECT
      - score ≥ threshold * 0.8 → TOO_CLOSE
      - abaixo → INCORRECT
```

### 11.2 Pipeline (modo ANSWER_FROM_QUESTION)

```
userInput
  → TextNormalizer.normalize()
  → SemanticMatcher.score() diretamente
      (sem verificação de tipo/estrutura gramatical)
```

### 11.3 Contrações suportadas

O `TextNormalizer` expande automaticamente:
- Wh-word: `what's`, `where's`, `who's`, `how's`, `when's`, `why's`
- Pronomes: `i'm`, `you're`, `he's`, `she's`, `it's`, `we're`, `they're`
- Negativas: `don't`, `doesn't`, `didn't`, `isn't`, `aren't`, `won't`, `haven't`, `hasn't`, etc.
- Outras: `there's`, `that's`, `let's`, etc.

### 11.4 Score e threshold

- `match_threshold` é definido por resposta aceita no JSON (tipicamente 0.75–0.90)
- `CLOSE_THRESHOLD_FACTOR = 0.8` (threshold * 0.8 → TOO_CLOSE)
- Score final = `(semanticScore * 100 + structureBonus) * difficultyMultiplier`

---

## 12. Sistema SRS / FSRS

### 12.1 Algoritmo FSRS-4.5

Implementado em `FsrsScheduler.kt`. O algoritmo calcula o próximo intervalo de revisão
baseado em:
- `stability`: estabilidade da memória
- `difficulty`: dificuldade do card
- `retrievability`: probabilidade de recuperação

### 12.2 Estados de um SrsCard

```kotlin
enum class SrsCardState { NEW, LEARNING, REVIEW, RELEARNING }
```

### 12.3 Ratings do usuário

```kotlin
enum class Rating { AGAIN, HARD, GOOD, EASY }
```

---

## 13. Gamificação

### 13.1 XP

Calculado por `LevelProgressionAlgorithm.calculatePoints()`:
- Base: `exercise.xpReward` (definido no JSON, tipicamente 10–50)
- Multiplicadores: dificuldade, número da tentativa, streak ativo

### 13.2 Streak

Gerenciado por `StreakCalculator` e `CheckDailyStreakUseCase`:
- Incrementado ao completar qualquer exercício no dia
- Resetado se o usuário pular um dia
- Armazenado em `streak_records`

### 13.3 Conquistas (`achievement_key`)

Exemplos de chaves: `first_exercise`, `level_1_complete`, `streak_7`, `streak_30`,
`exercises_50`, `exercises_100`, `perfect_session`.

Para adicionar conquistas: implementar em `CheckAchievementsUseCase.kt`.

---

## 14. TTS e Reconhecimento de Voz

### 14.1 TtsManager

```kotlin
// Uso em Composable:
val (ttsManager, ttsState) = rememberTtsManager()

// Falar texto:
ttsManager.speak("Hello, how are you?")

// Falar devagar (0.7x):
ttsManager.speak("The flight departs at six.", slow = true)

// Parar:
ttsManager.stop()
```

**Lifecycle:** `rememberTtsManager()` faz `shutdown()` automaticamente via `DisposableEffect`.

### 14.2 SpeechRecognizer

```kotlin
// Uso em Composable:
val launchSpeech = rememberSpeechLauncher { transcribedText ->
    // usa o texto transcrito
}

// Lançar:
launchSpeech()
```

Sempre em inglês (`en-US`), independente do idioma do dispositivo.

---

## 15. Injeção de Dependência

### 15.1 Módulos Hilt

| Módulo | Escopo | Provê |
|--------|--------|-------|
| `DatabaseModule` | `@Singleton` | `EnglishLearningDatabase`, todos os DAOs |
| `RepositoryModule` | `@Singleton` | `ContentRepositoryImpl`, `ProgressRepositoryImpl`, `SrsRepositoryImpl`, `SessionSettingsRepository` |
| `AppModule` | `@Singleton` | (placeholder para bindings futuros) |

### 15.2 Ponto de entrada

`@HiltAndroidApp` em `EnglishLearningApp`.
`@AndroidEntryPoint` em `MainActivity`.
`@HiltViewModel` em todos os ViewModels.

---

## 16. Testes

### 16.1 Estrutura

```
app/src/test/java/com/englishlearning/app/
├── domain/validation/
│   ├── TextNormalizerPropertyTest.kt
│   ├── SemanticMatcherPropertyTest.kt
│   ├── GrammarParserPropertyTest.kt
│   ├── QuestionValidationEngineImplTest.kt
│   └── QuestionValidationEnginePropertyTest.kt
├── domain/gamification/
│   ├── StreakCalculatorPropertyTest.kt
│   ├── FsrsSchedulerTest.kt
│   ├── FsrsSchedulerPropertyTest.kt
│   └── LevelProgressionAlgorithmPropertyTest.kt
├── data/seeder/
│   └── ExerciseSerializationPropertyTest.kt
└── ui/lesson/
    └── LessonViewModelTest.kt
```

### 16.2 Framework

**Kotest** com JUnit 5 Platform. Os testes usam property-based testing (PBT):
geração automática de entradas aleatórias para verificar propriedades invariantes.

### 16.3 Como rodar

```bash
export JAVA_HOME=/home/houston/tools/jdk-17.0.13+11
export GRADLE_USER_HOME=/home/houston/.gradle
cd english-learning-app
gradle test --no-daemon
```

---

## 17. Build e Release

### 17.1 Pré-requisitos (ambiente WSL/Linux)

```bash
# JDK 17 (Temurin) - localização atual:
/home/houston/tools/jdk-17.0.13+11/

# Gradle 8.11.1 - localização atual:
/home/houston/tools/gradle-dist/gradle-8.11.1/

# Android SDK - localização atual:
/home/houston/android-sdk/
  ├── build-tools/35.0.0/
  ├── platforms/android-35/
  └── platform-tools/adb
```

### 17.2 Comando de build

```bash
export JAVA_HOME=/home/houston/tools/jdk-17.0.13+11
export PATH=$JAVA_HOME/bin:/home/houston/tools/gradle-dist/gradle-8.11.1/bin:$PATH
export GRADLE_USER_HOME=/home/houston/.gradle
cd /home/houston/english-learning-app

# Build debug
gradle assembleDebug --no-daemon --warning-mode=none -Dorg.gradle.jvmargs="-Xmx2g"

# APK gerado em:
# app/build/outputs/apk/debug/app-debug.apk
```

### 17.3 Observação sobre SSL (ambiente corporativo)

O ambiente de desenvolvimento usa proxy Zscaler que intercepta SSL.
Os certificados do Zscaler foram importados no JDK em:
```
/home/houston/tools/jdk-17.0.13+11/lib/security/cacerts
aliases: zscaler-ca-2, zscaler-ca-3
```
Sem isso, o Gradle não consegue baixar dependências do `dl.google.com`.

### 17.4 Publicar release no GitHub

```bash
export PATH=/home/houston/tools/gh/bin:$PATH
export GH_TOKEN=<token>

cp app/build/outputs/apk/debug/app-debug.apk /tmp/english-learning-app.apk

gh release create v1.X.Y \
  /tmp/english-learning-app.apk \
  --repo brancomrt/english-learning-app \
  --target feature/english-learning-app \
  --title "vX.Y.Z - Descrição" \
  --notes "Notas da release"
```

### 17.5 Versionamento

Branch: `feature/english-learning-app`
Versão atual do app: `versionCode = 1`, `versionName = "1.0"` em `app/build.gradle.kts`
Releases GitHub: v1.0.0 → v1.1.0 (atual)

---

## 18. Guia de Manutenção

### 18.1 Adicionar novos exercícios

1. Editar `app/src/main/assets/content/exercises/level_N.json`
2. Garantir IDs únicos globalmente (ver seção 9.5)
3. O `DatabaseSeeder` verifica `db_seeded` — para reprocessar, limpar dados do app
4. Aumentar `versionCode` em `app/build.gradle.kts`

### 18.2 Adicionar novas palavras ao vocabulário

1. Editar `app/src/main/assets/content/vocabulary/level_N_vocab.json`
2. Usar `image_url` do Pexels: `https://images.pexels.com/photos/{ID}/pexels-photo-{ID}.jpeg?auto=compress&cs=tinysrgb&w=400&h=300&fit=crop`
3. Mesmas observações do seeder acima

### 18.3 Adicionar novo nível

1. Adicionar entrada em `levels.json`
2. Adicionar categorias em `categories.json`
3. Criar `exercises/level_N.json` e `vocabulary/level_N_vocab.json`
4. Adicionar nomes de display em `SettingsViewModel.levelDisplayName()`
5. Adicionar abreviação em `VocabularyScreen.LevelFilterRow`

### 18.4 Adicionar nova tela

1. Criar `ui/minhafuncionalidade/MinhaTela.kt` + `MinhaTelaViewModel.kt`
2. Adicionar rota em `Screen` sealed class no `AppNavGraph.kt`
3. Adicionar `composable(Screen.MinhaRota.route)` no `NavHost`
4. Adicionar navegação de onde for necessário

### 18.5 Adicionar nova entidade ao banco

1. Criar `data/database/entity/NovaEntidade.kt` com `@Entity`
2. Criar DAO em `data/database/dao/NovaEntidadeDao.kt`
3. Adicionar no `@Database(entities = [...])` em `EnglishLearningDatabase.kt`
4. Incrementar `version`
5. Criar `MIGRATION_N_M` e registrar em `DatabaseModule`
6. Adicionar `@Provides fun provideNovaDao(...)` em `DatabaseModule`

### 18.6 Modificar o motor de validação

Arquivo principal: `domain/validation/QuestionValidationEngineImpl.kt`

- Para suportar novo tipo de pergunta: adicionar em `QuestionType` enum e implementar
  `detectQuestionType()` e `checkStructure()` em `GrammarParser`
- Para adicionar novas contrações: editar o mapa `contractions` em `TextNormalizer`
- Para ajustar thresholds: constantes `CLOSE_THRESHOLD_FACTOR` e `STRUCTURE_BONUS`

### 18.7 Checklist antes de cada release

- [ ] Verificar se todos os novos IDs de exercícios são únicos
- [ ] Rodar `gradle assembleDebug` sem erros
- [ ] Verificar se alguma migração de banco é necessária
- [ ] Incrementar `versionCode` em `app/build.gradle.kts`
- [ ] Commitar no branch `feature/english-learning-app`
- [ ] Criar release no GitHub com o APK

---

*Documentação gerada em setembro de 2026. Para dúvidas, consultar o repositório:
https://github.com/brancomrt/english-learning-app*
