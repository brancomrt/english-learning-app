# Arquitetura Detalhada — English Learning App

## Diagrama de Camadas

```
┌──────────────────────────────────────────────────────────────────┐
│  UI LAYER                                                        │
│  ┌─────────────┐  ┌────────────────┐  ┌────────────────────┐    │
│  │ Composables  │  │  ViewModels    │  │  Navigation        │    │
│  │ (Screens)    │◄─│  StateFlow     │  │  AppNavGraph       │    │
│  │              │  │  Channel<Event>│  │  Screen sealed     │    │
│  └─────────────┘  └───────┬────────┘  └────────────────────┘    │
│                           │ inject UseCases                      │
├───────────────────────────┼──────────────────────────────────────┤
│  DOMAIN LAYER             │                                      │
│                    ┌──────▼──────┐                               │
│                    │  Use Cases  │                               │
│                    │  (8 classes)│                               │
│                    └──────┬──────┘                               │
│                           │ uses                                 │
│  ┌────────────────────────▼───────────────────────┐             │
│  │  Domain Models  │  Repository Interfaces        │             │
│  │  Exercise        │  ContentRepository           │             │
│  │  Level           │  ProgressRepository          │             │
│  │  Category        │  SrsRepository               │             │
│  │  ValidationResult│                              │             │
│  └────────────────────────────────────────────────┘             │
│                                                                  │
│  ┌────────────────────────────────────────────────┐             │
│  │  Validation Engine   │  Gamification            │             │
│  │  TextNormalizer      │  LevelProgressionAlg     │             │
│  │  GrammarParser       │  StreakCalculator         │             │
│  │  SemanticMatcher     │  FsrsScheduler           │             │
│  │  ValidationEngineImpl│                          │             │
│  └────────────────────────────────────────────────┘             │
├──────────────────────────────────────────────────────────────────┤
│  DATA LAYER                                                      │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐  │
│  │  Repository Impls    │  │  Room Database v4                │  │
│  │  ContentRepositoryImpl│ │  12 entidades                    │  │
│  │  ProgressRepoImpl    │  │  11 DAOs                         │  │
│  │  SrsRepositoryImpl   │  │  4 migrações (v1→v4)             │  │
│  └──────────────────────┘  └──────────────────────────────────┘  │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐  │
│  │  DatabaseSeeder      │  │  SessionSettingsRepository       │  │
│  │  JSON → Room         │  │  SharedPreferences               │  │
│  └──────────────────────┘  └──────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
         ↑ assets/content/*.json      ↑ Pexels CDN (imagens)
```

## Fluxo de um Exercício

```
Usuário abre CategorySelect
         │
         ▼
LessonViewModel.shouldShowLesson(categoryId)
         │
    ┌────┴────┐
    │ Não viu │    ┌─────┐
    │ a lição │───►│Lição│
    └─────────┘    └──┬──┘
    │ Já viu   │      │ onStartLesson / onSkipLesson
    └──────────┘      │
         │            │
         ▼            ▼
    LearningSessionScreen
         │
         │ LearningViewModel.startSession(levelId, categoryId)
         │   → LoadExercisesUseCase
         │       → ContentRepository.getExercisesByLevelAndCategory()
         │       → shuffled().take(limit)  ← configurável pelo usuário
         ▼
    ExerciseActive state
    ┌──────────────────────────────┐
    │ displayText (resposta/pergunta)│
    │ QuestionTypeChip              │
    │ ContextCard (se houver)       │
    │ TextField + MicButton + 🔊    │
    │ Botão "Verificar"             │
    │ Botão "Dica" + dicas inline   │
    │ Botão "📖 Ver teoria"         │
    └──────────────────────────────┘
         │ submitAnswer(userInput)
         ▼
    ValidateAnswerUseCase
         │
         ├── ExerciseMode == ANSWER_FROM_QUESTION?
         │       └── SemanticMatcher.score() direto
         │
         └── ExerciseMode == QUESTION_FROM_ANSWER?
                 ├── TextNormalizer.normalize()
                 │   └── expandContractions() + lowercase + remove punct
                 ├── GrammarParser.detectQuestionType()
                 ├── GrammarParser.checkStructure()
                 └── SemanticMatcher.score()
                     (Jaccard 60% + bigrams 40%)
         │
         ▼
    ValidationResult → FeedbackScreen overlay
         │
         ├── CORRECT  → UpdateProgressUseCase → CheckAchievements → moveToNext()
         ├── TOO_CLOSE → mostrar dicas, tentar novamente
         └── INCORRECT → mostrar dicas/correções, tentar novamente
                              (após 3 tentativas → revela resposta)
         │
         ▼ (último exercício)
    SessionSummaryScreen → Home
```

## Fluxo do Vocabulário

```
VocabularyScreen
    │
    ├── LevelFilterRow (chips A1→C1)
    ├── CategoryFilterRow (chips por categoria)
    └── LazyVerticalGrid 2 colunas
            │
            ▼ para cada VocabularyCardEntity
       VocabularyCard
            │
       ┌────┴─────┐
       │ FRENTE   │  → só imagem (modo adivinhação)
       │ (isFlipped=false)│  → hint: "Que palavra é essa?"
       └──────────┘
            │ toque → viewModel.flipCard(id)
            │ LaunchedEffect(isFlipped) → delay(450ms) → ttsManager.speak(word)
            ▼
       ┌────┴─────┐
       │  VERSO   │  → palavra em inglês + 🔊 (replay)
       │ (isFlipped=true)│  → definição PT-BR
       │          │  → "exemplo" + 🔊 (devagar, 0.7x)
       └──────────┘
```

## Injeção de Dependência (Hilt)

```
@HiltAndroidApp
EnglishLearningApp
        │
        ├── DatabaseModule (@Singleton)
        │       ├── EnglishLearningDatabase (Room)
        │       └── 11 DAOs (LevelDao, ExerciseDao, ...)
        │
        ├── RepositoryModule (@Singleton)
        │       ├── ContentRepository → ContentRepositoryImpl
        │       ├── ProgressRepository → ProgressRepositoryImpl
        │       ├── SrsRepository → SrsRepositoryImpl
        │       └── SessionSettingsRepository (direct binding)
        │
        └── @AndroidEntryPoint: MainActivity
                └── ViewModels via @HiltViewModel
                    (injetados com hiltViewModel() em Composables)
```

## Banco de Dados — Relacionamentos

```
levels ──────┬──── categories ────┬──── exercises ──┬── accepted_answers
             │                    │                  └── hints
             └──── user_progress  │
                                  └──── vocabulary_cards

user_profile ──── streak_records
             ──── achievements
             ──── attempts ──── exercises (ref)
             ──── srs_cards ──── (ref_id → exercise ou vocabulary_card)
```

## Configurações (SharedPreferences: "app_prefs")

| Chave | Tipo | Default | Descrição |
|-------|------|---------|-----------|
| `db_seeded` | Boolean | false | Flag de seeding inicial |
| `onboarding_completed` | Boolean | false | Flag de tutorial |
| `questions_limit_level_1` | Int | 10 | Limite questões nível 1 |
| `questions_limit_level_2` | Int | 10 | Limite questões nível 2 |
| `questions_limit_level_3` | Int | 10 | Limite questões nível 3 |
| `questions_limit_level_4` | Int | 10 | Limite questões nível 4 |
| `questions_limit_level_5` | Int | 10 | Limite questões nível 5 |
| `questions_limit_level_6` | Int | 10 | Limite questões nível 6 |

## Permissões Android

| Permissão | Uso | Sensível? |
|-----------|-----|-----------|
| `INTERNET` | Imagens do vocabulário (Pexels CDN) | Baixo |
| `RECORD_AUDIO` | RecognizerIntent (voz para texto) | Médio — solicitado em runtime no Android 6+ |
