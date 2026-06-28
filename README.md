# Ai_Tranning — Offline Project Management App

A fully **offline** Android app for managing work in a simple hierarchy: **User → Projects → Tasks**.
There is no backend and no network access — a local **Room** (SQLite) database replaces the server, and
repositories replace API calls.

Built with Jetpack Compose, MVVM, Hilt, Room, Coroutines/StateFlow, and Navigation Compose.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Architecture Overview](#architecture-overview)
- [Authentication Flow](#authentication-flow)
- [Setup & Installation](#setup--installation)
- [Build & Run](#build--run)
- [Testing](#testing)
- [Configuration & "Environment Variables"](#configuration--environment-variables)
- [Further Documentation](#further-documentation)

---

## Features

- **Local accounts** — register and log in entirely offline; credentials are stored hashed in SQLite.
- **Persistent session** — stays logged in across restarts via DataStore; logout clears it.
- **Projects** — each user owns a list of projects.
- **Tasks** — each project owns tasks with a status (`TODO` / `IN_PROGRESS` / `DONE`), a priority
  (`LOW` / `MEDIUM` / `HIGH`), and an optional due date.
- **Cascade delete** — deleting a project removes its tasks; deleting a user removes everything they own.

---

## Tech Stack

| Concern        | Choice                                              |
|----------------|-----------------------------------------------------|
| Language       | Kotlin (Java 17 toolchain)                          |
| UI             | Jetpack Compose + Material 3                         |
| Architecture   | MVVM (Compose → ViewModel → Repository → DAO)       |
| DI             | Hilt                                                 |
| Database       | Room (SQLite)                                        |
| Async / state  | Kotlin Coroutines + `StateFlow`                     |
| Navigation     | Navigation Compose                                   |
| Preferences    | DataStore (Preferences)                             |
| Testing        | JUnit 4, MockK, coroutines-test, Compose UI Test    |
| Build          | Gradle (Kotlin DSL) + version catalog               |

**SDK levels:** `minSdk 24`, `targetSdk 36`, `compileSdk 36`.

---

## Project Structure

```
Ai_Tranning/
├─ app/
│  ├─ build.gradle.kts            # Module build script & dependencies
│  └─ src/
│     ├─ main/java/com/example/ai_tranning/
│     │  ├─ AiTrainingApp.kt      # @HiltAndroidApp — DI root
│     │  ├─ MainActivity.kt       # Single activity; chooses start destination from session
│     │  │
│     │  ├─ data/
│     │  │  ├─ local/
│     │  │  │  ├─ AppDatabase.kt          # Room database (v1, "ai_tranning_db")
│     │  │  │  ├─ converter/Converters.kt # Room @TypeConverters
│     │  │  │  ├─ dao/                     # UserDao, ProjectDao, TaskDao
│     │  │  │  ├─ entity/                  # UserEntity, ProjectEntity, TaskEntity
│     │  │  │  └─ relation/                # ProjectWithTasks, UserWithProjects
│     │  │  └─ repository/                 # UserRepository, ProjectRepository, TaskRepository
│     │  │
│     │  ├─ di/
│     │  │  ├─ DatabaseModule.kt   # Provides Room db + DAOs
│     │  │  └─ RepositoryModule.kt # Provides DataStore
│     │  │
│     │  ├─ ui/
│     │  │  ├─ components/         # Shared composables (e.g. PriorityBadge)
│     │  │  ├─ navigation/NavGraph.kt  # Routes + NavHost
│     │  │  ├─ screens/            # login, register, dashboard, project, task
│     │  │  └─ theme/              # Color, Theme, Type
│     │  │
│     │  ├─ utils/SessionManager.kt   # DataStore-backed session (logged-in user id)
│     │  └─ viewmodel/                # One ViewModel per screen
│     │
│     ├─ test/                    # JVM unit tests (MockK, coroutines-test)
│     └─ androidTest/             # Instrumented tests (in-memory Room, Compose UI)
│
├─ gradle/libs.versions.toml      # Version catalog (single source of dependency versions)
├─ build.gradle.kts               # Root build script
├─ settings.gradle.kts            # Module includes & repositories
├─ local.properties              # Local-only: Android SDK path (NOT committed)
└─ docs/                          # Architecture, authentication, testing docs
```

---

## Architecture Overview

The app follows **MVVM** with a unidirectional data flow:

```
┌───────────┐   user events    ┌────────────┐   suspend/Flow   ┌──────────────┐   DAO    ┌────────┐
│  Compose  │ ───────────────► │  ViewModel │ ───────────────► │  Repository  │ ───────► │  Room  │
│  Screen   │ ◄─────────────── │ (StateFlow)│ ◄─────────────── │ (@Singleton) │ ◄─────── │ SQLite │
└───────────┘   UI state       └────────────┘     data         └──────────────┘          └────────┘
```

- **Compose screens** are stateless; they render a `UiState` and forward events to the ViewModel.
- **ViewModels** (`@HiltViewModel`) hold UI state in `StateFlow`, validate input, and call repositories
  from `viewModelScope`.
- **Repositories** (`@Singleton`) own business rules and are the only layer that talks to DAOs. In this
  offline app they stand in for what a remote API would normally do.
- **Room** persists everything. Relationships use `ForeignKey.CASCADE`:
  - **User** 1:N **Project** 1:N **Task**
  - Deleting a project auto-deletes its tasks; deleting a user auto-deletes their projects and tasks.

Dependency injection is handled by **Hilt**: `AiTrainingApp` is the root, `DatabaseModule` provides the
Room database and DAOs, and `RepositoryModule` provides the DataStore.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for a deeper walkthrough.

### Navigation flow

```
(MainActivity reads SessionManager)
        │
        ├─ session present ─► Dashboard ─► Project details ─► Task create/edit
        │
        └─ no session ─────► Login ⇄ Register ─► Dashboard
```

---

## Authentication Flow

Authentication is fully local. Passwords are **SHA-256 hashed** before storage; the plaintext is never
persisted. The logged-in user's id is kept in DataStore so the session survives restarts.

**Register**

1. `RegisterScreen` collects username, email, password, confirmation.
2. `RegisterViewModel` validates: required fields → passwords match → length ≥ 6.
3. `UserRepository.registerUser` checks username/email uniqueness, hashes the password, inserts the user.
4. On success the new user id is saved to `SessionManager` (auto-login) and the app navigates to the dashboard.

**Login**

1. `LoginScreen` collects username + password.
2. `LoginViewModel` validates that both fields are non-blank.
3. `UserRepository.loginUser` hashes the password and matches `(username, passwordHash)` in a single query.
4. On success the user id is saved to `SessionManager`; on failure a generic `"Invalid username or password"`
   message is shown (the error does not reveal whether the username or the password was wrong).

> ⚠️ **Security note:** SHA-256 is fast and unsalted — fine for this offline demo, but **not** appropriate
> for a production app handling real credentials. See [`docs/AUTHENTICATION.md`](docs/AUTHENTICATION.md) for
> the full flow, threat model, and recommended hardening (salted Argon2id/bcrypt, rate limiting, etc.).

---

## Setup & Installation

### Prerequisites

- **Android Studio** (latest stable recommended) or the Android command-line tools.
- **JDK 17** (the project targets the Java 17 toolchain).
- **Android SDK** with platform **API 36** installed.

### Steps

1. **Clone the repository.**
   ```bash
   git clone <repository-url>
   cd Ai_Tranning
   ```
2. **Point the build at your Android SDK.** Create/verify `local.properties` in the project root:
   ```properties
   sdk.dir=/absolute/path/to/Android/Sdk
   ```
   (Android Studio writes this automatically the first time you open the project.)
3. **Open in Android Studio** and let it sync Gradle, **or** build from the command line (below).

---

## Build & Run

```bash
./gradlew assembleDebug          # Build the debug APK
./gradlew installDebug           # Install on a connected device/emulator
./gradlew assembleRelease        # Build the release APK
./gradlew clean                  # Clean build outputs
```

On Windows use `gradlew.bat` instead of `./gradlew`.

The debug APK is written to `app/build/outputs/apk/debug/`.

---

## Testing

```bash
./gradlew testDebugUnitTest      # JVM unit tests (fast; no device needed)
./gradlew connectedAndroidTest   # Instrumented tests (requires an emulator/device)
```

Run a single unit-test class:

```bash
./gradlew testDebugUnitTest --tests "com.example.ai_tranning.data.repository.UserRepositoryTest"
```

**What's covered for the auth module:**

- **Unit tests** (`app/src/test/...`, MockK + coroutines-test):
  `UserRepositoryTest`, `LoginViewModelTest`, `RegisterViewModelTest`, `SessionManagerTest`.
- **Integration tests** (`app/src/androidTest/...`, in-memory Room):
  `UserDaoTest`, `UserRepositoryIntegrationTest`.

These cover positive/negative paths, validation, edge cases, and security properties (password is hashed
not stored in plaintext, generic error messages, duplicate prevention, session lifecycle). See
[`docs/TESTING.md`](docs/TESTING.md) for the full breakdown.

---

## Configuration & "Environment Variables"

This is a self-contained offline app, so it has **no runtime environment variables, API keys, or `.env`
file** — there is nothing to point at because there is no network. The closest equivalents are local
build/config values:

| Setting              | Where                                   | Notes                                                        |
|----------------------|-----------------------------------------|-------------------------------------------------------------|
| `sdk.dir`            | `local.properties`                      | Path to your Android SDK. **Machine-local; not committed.** |
| Room database name   | `DatabaseModule.kt` → `"ai_tranning_db"`| On-device SQLite file name.                                  |
| DataStore file       | `RepositoryModule.kt` → `"user_prefs"`  | Preferences/session store file name.                        |
| `applicationId`      | `app/build.gradle.kts`                  | `com.example.ai_tranning`.                                   |
| `min/target/compileSdk` | `app/build.gradle.kts`               | 24 / 36 / 36.                                                |
| Dependency versions  | `gradle/libs.versions.toml`             | Single source of truth for all versions.                    |

> If a release signing config is added later, keystore credentials should go in a **git-ignored**
> `keystore.properties` file (or environment variables in CI) — never hard-coded in `build.gradle.kts`.

---

## Further Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — layers, data flow, DI, persistence, navigation.
- [`docs/AUTHENTICATION.md`](docs/AUTHENTICATION.md) — auth flow, password handling, threat model, hardening.
- [`docs/TESTING.md`](docs/TESTING.md) — test strategy, what each suite covers, how to run.
- [`CLAUDE.md`](CLAUDE.md) — quick reference for build commands and conventions.
- **In-code KDoc** — every public class and function carries KDoc; hover in Android Studio or generate
  HTML with a Dokka setup if desired.