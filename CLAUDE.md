# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Fully offline Android project management app (User -> Projects -> Tasks). No network/API calls — Room replaces the backend, repositories replace API calls.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew testDebugUnitTest --tests "com.example.ai_tranning.ExampleUnitTest"  # Single test
./gradlew connectedAndroidTest   # Instrumented tests (requires emulator/device)
./gradlew clean                  # Clean build
```

## Tech Stack

- **Language**: Kotlin (Java 11 source compat)
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM
- **DI**: Hilt
- **Database**: Room (SQLite)
- **Async**: Kotlin Coroutines + StateFlow
- **Navigation**: Navigation Compose
- **Preferences**: DataStore
- **Testing**: JUnit, MockK, Compose UI Test
- **Build**: Gradle 9.4.1, Kotlin DSL, version catalogs (`gradle/libs.versions.toml`)
- **Min SDK**: 24 / **Target SDK**: 36

## Architecture & Data Flow

```
Compose UI -> ViewModel -> Repository -> Room DAO -> SQLite
```

All ViewModels use `StateFlow` for UI state. Repositories are `@Singleton` injected via Hilt.

### Room Relationships (cascade delete)
- **User** 1:N **Project** 1:N **Task**
- Deleting a project auto-deletes its tasks via `ForeignKey.CASCADE`

### Key Layers

- `data/local/entity/` — Room entities: `UserEntity`, `ProjectEntity`, `TaskEntity`
- `data/local/dao/` — Room DAOs: `UserDao`, `ProjectDao`, `TaskDao`
- `data/local/relation/` — `ProjectWithTasks`, `UserWithProjects`
- `data/local/AppDatabase.kt` — Room database (version 1, db name: `ai_tranning_db`)
- `data/repository/` — `UserRepository`, `ProjectRepository`, `TaskRepository`
- `di/` — Hilt modules: `DatabaseModule` (Room), `RepositoryModule` (DataStore)
- `viewmodel/` — One per screen: Login, Register, Dashboard, Project, Task
- `ui/screens/` — Compose screens: login, register, dashboard, project, task
- `ui/navigation/NavGraph.kt` — All routes defined in `Routes` object
- `ui/components/` — Shared composables (e.g., `PriorityBadge`)
- `utils/SessionManager.kt` — DataStore-based session (stores logged-in user ID)

### Navigation Flow

Splash -> Login -> Register -> Dashboard -> Project Details -> Task Details

`MainActivity` checks `SessionManager` to auto-navigate to Dashboard if already logged in.

### Hilt Setup

- `AiTrainingApp` (`@HiltAndroidApp`) registered in AndroidManifest as `android:name`
- `MainActivity` is `@AndroidEntryPoint`
- All ViewModels use `@HiltViewModel`

### Task Model

- Status: `TODO`, `IN_PROGRESS`, `DONE`
- Priority: `LOW`, `MEDIUM`, `HIGH`
- Optional `dueDate` (stored as `Long?` millis)