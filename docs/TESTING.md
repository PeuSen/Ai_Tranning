# Testing

This project has two test source sets:

| Source set      | Location              | Runs on            | Tooling                          |
|-----------------|-----------------------|--------------------|----------------------------------|
| Unit tests      | `app/src/test/`       | local JVM (fast)   | JUnit 4, MockK, coroutines-test  |
| Instrumented    | `app/src/androidTest/`| device/emulator    | AndroidJUnit4, in-memory Room, Compose UI Test |

## Running

```bash
# Unit tests (no device needed)
./gradlew testDebugUnitTest

# A single class
./gradlew testDebugUnitTest --tests "com.example.ai_tranning.viewmodel.LoginViewModelTest"

# Instrumented/integration tests (requires a running emulator or connected device)
./gradlew connectedAndroidTest
```

HTML reports: `app/build/reports/tests/testDebugUnitTest/index.html`.

## Test utilities

- **`MainDispatcherRule`** (`app/src/test/.../util/MainDispatcherRule.kt`) — swaps `Dispatchers.Main`
  for an `UnconfinedTestDispatcher` so ViewModel coroutines launched in `viewModelScope` run
  synchronously and state can be asserted immediately. Required for any ViewModel test.

## Authentication test matrix

### Unit — `UserRepositoryTest` (DAO mocked with MockK)
- **Positive:** successful registration returns the new id; successful login returns the user.
- **Negative:** duplicate username and duplicate email are rejected (and no insert happens); wrong
  password / unknown user return the generic failure.
- **Ordering:** username uniqueness is checked before email.
- **Edge cases:** empty password and unicode (UTF-8) passwords hash without error.
- **Security:** stored value is the SHA-256 hash (64 hex chars), never the plaintext; hashing is
  deterministic; different passwords produce different hashes; register and login use the same hash so
  credentials round-trip; login queries the DAO with the hash, not the raw password.

### Unit — `LoginViewModelTest`
- Initial state is empty/idle.
- Field changes update state and clear stale errors.
- Blank / whitespace-only fields fail validation and skip the repository.
- Successful login sets `isLoggedIn` and persists the session.
- Failed login surfaces the error and does **not** persist a session.
- `isLoading` is reset after completion.

### Unit — `RegisterViewModelTest`
- Initial state is empty/idle; field changes clear errors.
- Validation ladder is enforced in order: required fields → password match → minimum length
  (incl. boundary: exactly 6 chars passes; missing field beats mismatch).
- Successful registration sets `isRegistered` and saves the session.
- Duplicate username/email failures surface to the UI state and skip session save.

### Unit — `SessionManagerTest` (real DataStore on a temp file)
- No session → emits `null`.
- `saveUserId` persists and reads back; a second save overwrites the first.
- `clearSession` returns to `null`; clearing an empty store is a safe no-op.

### Integration — `UserDaoTest` (in-memory Room)
- Insert returns a generated id.
- Lookups by id/username/email (hit and miss).
- `login` matches only the exact `(username, passwordHash)` pair (wrong hash / wrong username fail).
- Username/email lookups are case-sensitive (documents current SQLite `BINARY` collation behaviour).
- Multiple users persist independently.

### Integration — `UserRepositoryIntegrationTest` (repository + in-memory Room)
- End-to-end **register → login** with the same credentials succeeds and ids match.
- Login fails with the wrong password for a real, registered user.
- Duplicate username/email rejected end-to-end.
- The persisted hash is not the plaintext (and is 64 hex chars).
- `getCurrentUser` returns the registered user.

## Notes on scope

- There are **no JWT, token-expiry, or rate-limiting tests** because those mechanisms are not part of the
  app's (offline) design — see [`AUTHENTICATION.md`](AUTHENTICATION.md). The security tests instead assert
  the properties the app actually guarantees.
- External dependencies are mocked where it isolates the unit under test (`UserDao` in
  `UserRepositoryTest`; `UserRepository` + `SessionManager` in the ViewModel tests). The integration tests
  deliberately use a **real** in-memory database to exercise the full slice.