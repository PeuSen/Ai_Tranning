# Architecture

This document explains how Ai_Tranning is structured and how data flows through it. For the
authentication module specifically, see [`AUTHENTICATION.md`](AUTHENTICATION.md).

## Goals & constraints

- **Fully offline.** No network, no API, no remote auth. A local Room (SQLite) database is the system of
  record; repositories play the role a backend service normally would.
- **MVVM** with unidirectional data flow and immutable UI state.
- **Single source of truth** per concern: one repository per aggregate, one DataStore for the session.

## Layered design

```
┌──────────────────────────────────────────────────────────────────────┐
│ UI (Jetpack Compose)                                                   │
│   - Stateless screens render a UiState and emit events                 │
│   - Screens: login, register, dashboard, project, task                 │
└───────────────▲───────────────────────────────────┬───────────────────┘
                │ StateFlow<UiState>                 │ user events
┌───────────────┴───────────────────────────────────▼───────────────────┐
│ ViewModel (@HiltViewModel)                                             │
│   - Holds UiState in MutableStateFlow                                  │
│   - Validates input, launches work in viewModelScope                   │
│   - Maps Result/Flow from repositories into UiState                    │
└───────────────▲───────────────────────────────────┬───────────────────┘
                │ domain data                        │ suspend calls / Flow
┌───────────────┴───────────────────────────────────▼───────────────────┐
│ Repository (@Singleton)                                               │
│   - Business rules (uniqueness, hashing, validation-of-record)         │
│   - The only layer that talks to DAOs                                  │
└───────────────▲───────────────────────────────────┬───────────────────┘
                │ entities / relations               │ DAO calls
┌───────────────┴───────────────────────────────────▼───────────────────┐
│ Room (SQLite)                                                         │
│   - Entities, DAOs, relations, type converters                        │
│   - Foreign keys with CASCADE delete                                   │
└────────────────────────────────────────────────────────────────────────┘
```

### UI layer (`ui/`)
- Composables are **stateless**: they receive `uiState` (collected from a `StateFlow`) plus lambdas for
  events and navigation. They contain no business logic.
- `ui/navigation/NavGraph.kt` defines all routes (`Routes`) and the `NavHost`. Typed arguments
  (`projectId`, `taskId`) are declared as `NavType.LongType`.
- `ui/components/` holds reusable composables (e.g. `PriorityBadge`); `ui/theme/` holds Material 3 theming.

### ViewModel layer (`viewmodel/`)
- One ViewModel per screen, each annotated `@HiltViewModel`.
- State is a single immutable `*UiState` data class held in a `MutableStateFlow`, exposed read-only as a
  `StateFlow`.
- Work runs in `viewModelScope`; results from repositories (`Result<T>` or `Flow<T>`) are folded into the
  next `UiState`.

### Repository layer (`data/repository/`)
- `@Singleton` classes injected via Hilt.
- Own the domain rules: uniqueness checks, password hashing, mapping DAO results into `Result`/`Flow`.
- Never expose Room types upward beyond entities/relations; ViewModels never touch DAOs directly.

### Data layer (`data/local/`)
- **Entities** (`entity/`): `UserEntity`, `ProjectEntity`, `TaskEntity`.
- **DAOs** (`dao/`): `UserDao`, `ProjectDao`, `TaskDao` — `suspend` for single-shot operations, `Flow`
  for observable queries.
- **Relations** (`relation/`): `UserWithProjects`, `ProjectWithTasks` for `@Relation` graph reads.
- **Converters** (`converter/Converters.kt`): Room `@TypeConverters` for non-primitive columns.
- **Database** (`AppDatabase.kt`): version 1, file `ai_tranning_db`, `exportSchema = false`.

## Persistence model

```
User (users)
  └─1:N─ Project (projects)   FK userId  ─ CASCADE
            └─1:N─ Task (tasks)  FK projectId ─ CASCADE
```

- Deleting a **project** removes its **tasks**.
- Deleting a **user** removes their **projects** and (transitively) **tasks**.
- Foreign-key columns (`userId`, `projectId`) are indexed for efficient per-parent queries.

## Dependency injection (Hilt)

- `AiTrainingApp` (`@HiltAndroidApp`) is the DI root, registered as `android:name` in the manifest.
- `MainActivity` is `@AndroidEntryPoint`; all ViewModels are `@HiltViewModel`.
- `di/DatabaseModule` provides the singleton `AppDatabase` and the three DAOs.
- `di/RepositoryModule` provides the Preferences `DataStore` used by `SessionManager`.

## Session & startup

- `utils/SessionManager` stores the logged-in user id in DataStore and exposes it as `Flow<Long?>`.
- `MainActivity` collects the first emission to pick the start destination: **Dashboard** if a session
  exists, otherwise **Login**. Rendering is gated on that first emission to avoid a flicker.

## Threading & concurrency

- Room runs I/O on its own dispatcher; DAO methods are `suspend` or return `Flow`.
- ViewModels launch in `viewModelScope`, so work is cancelled automatically when the screen goes away.
- UI state is observed with `collectAsState()` and is always read on the main thread.

## Extending the app

- **New screen:** add a `*UiState` + `@HiltViewModel`, a stateless composable, and a `Routes` entry +
  `composable {}` in `NavGraph`.
- **New persisted field/table:** update the entity/DAO, **bump `AppDatabase` version**, and provide a
  Room migration (currently the schema is v1 with no migrations).
- **New cross-aggregate read:** prefer a `@Relation` data class in `relation/` over manual joins.