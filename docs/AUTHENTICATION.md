# Authentication

Ai_Tranning authenticates users **entirely locally** — there is no auth server, no tokens, and no
network. This document describes the flow, how credentials are handled, the threat model, and how the
design would need to change for production.

## Components

| Layer       | Class                | Responsibility                                                        |
|-------------|----------------------|----------------------------------------------------------------------|
| UI          | `LoginScreen`        | Username/password form; observes `LoginUiState`.                     |
| UI          | `RegisterScreen`     | Username/email/password/confirm form; observes `RegisterUiState`.    |
| ViewModel   | `LoginViewModel`     | Field state + blank-field validation; calls repository; saves session.|
| ViewModel   | `RegisterViewModel`  | Field state + validation ladder; calls repository; saves session.     |
| Repository  | `UserRepository`     | Uniqueness rules, SHA-256 hashing, credential matching.              |
| Persistence | `UserDao` / `UserEntity` | SQL queries and the `users` table.                              |
| Session     | `SessionManager`     | Persists the logged-in user id in DataStore.                        |

## Registration flow

```
RegisterScreen ──onChange──► RegisterViewModel
                                  │  validate:
                                  │   1. username/email/password non-blank   → "Please fill in all fields"
                                  │   2. password == confirmPassword         → "Passwords do not match"
                                  │   3. password.length >= 6                 → "Password must be at least 6 characters"
                                  ▼
                          UserRepository.registerUser(username, email, password)
                                  │   - getUserByUsername != null            → "Username already exists"
                                  │   - getUserByEmail    != null            → "Email already exists"
                                  │   - insert UserEntity(passwordHash = SHA-256(password))
                                  ▼
                          Result.success(newUserId)
                                  │
                          SessionManager.saveUserId(newUserId)   (auto-login)
                                  ▼
                          RegisterUiState.isRegistered = true → navigate to Dashboard
```

## Login flow

```
LoginScreen ──onChange──► LoginViewModel
                              │  validate: username & password non-blank → "Please fill in all fields"
                              ▼
                      UserRepository.loginUser(username, password)
                              │   userDao.login(username, SHA-256(password))
                              │     - match found → Result.success(user)
                              │     - no match    → Result.failure("Invalid username or password")
                              ▼
                      SessionManager.saveUserId(user.id)
                              ▼
                      LoginUiState.isLoggedIn = true → navigate to Dashboard
```

## Session lifecycle

- On successful login/registration, the user id is written to DataStore via `SessionManager.saveUserId`.
- `SessionManager.loggedInUserId` is a `Flow<Long?>` (`null` = no session).
- `MainActivity` reads it on launch to start on **Dashboard** (session present) or **Login** (absent).
- Logging out calls `SessionManager.clearSession()`, which removes the stored id.

## Password handling

- Passwords are hashed with **SHA-256** (`UserRepository.hashPassword`) and stored as a 64-character
  lowercase hex string in `UserEntity.passwordHash`. The plaintext is **never** persisted.
- The **same** hash function is used for registration and login, so credentials round-trip.
- Login compares `(username, passwordHash)` in a single SQL query (`UserDao.login`), so the plaintext
  never participates in the comparison.
- Login failures return a **generic** message and do not reveal whether the username or the password was
  wrong (reduces account enumeration).

## Threat model (current, offline app)

| Concern                         | Status in this app                                                        |
|---------------------------------|---------------------------------------------------------------------------|
| Network interception            | N/A — no network traffic.                                                  |
| Server-side breach              | N/A — no server.                                                           |
| Plaintext password at rest      | Mitigated — only the SHA-256 hash is stored.                              |
| Account enumeration via errors  | Mitigated — generic login error.                                          |
| Duplicate accounts              | Mitigated — username and email uniqueness enforced before insert.        |
| Local device compromise / root  | **Not** mitigated — DB and prefs are app-private but readable on a rooted device. |
| Offline brute force of the DB   | **Weak** — unsalted SHA-256 is fast and vulnerable to rainbow tables.    |

## ⚠️ Not production-grade — what to change

The auth design is deliberately simple for an offline demo. Before shipping anything that handles real
user credentials, harden it:

1. **Use a slow, salted password KDF.** Replace SHA-256 with **Argon2id**, **bcrypt**, or **scrypt**, each
   with a unique per-user random salt (and store the salt + parameters alongside the hash). This is the
   single most important change.
2. **Constant-time comparison.** Compare hashes with a constant-time function rather than relying on a SQL
   equality match.
3. **Input/format validation.** Validate email format and enforce a stronger password policy than
   "≥ 6 characters".
4. **Rate limiting / lockout.** Although there is no remote endpoint, repeated local attempts could be
   throttled (e.g. exponential backoff after N failures) to slow offline guessing.
5. **Encrypt data at rest.** Use **SQLCipher** for the Room database and the encrypted variants of DataStore
   for the session, keyed via the Android **Keystore**.
6. **If a backend is ever added:** move credential verification server-side, issue short-lived **JWTs** (or
   opaque session tokens) with refresh, transport over **TLS**, and validate token signature + expiry on
   every request. None of that exists today because the app is intentionally offline.

> The note above is also why the test suite does **not** include JWT/expiry/rate-limit tests: those
> mechanisms are not part of the current (offline) design. The security-focused tests instead assert the
> properties that *do* exist — hashed (never plaintext) storage, deterministic hashing, generic error
> messages, and duplicate prevention.

## Where the tests live

- `app/src/test/.../data/repository/UserRepositoryTest.kt` — hashing & rules (mocked DAO).
- `app/src/test/.../viewmodel/LoginViewModelTest.kt`, `RegisterViewModelTest.kt` — validation & state.
- `app/src/test/.../utils/SessionManagerTest.kt` — session persistence (real temp DataStore).
- `app/src/androidTest/.../data/local/UserDaoTest.kt` — DAO against in-memory Room.
- `app/src/androidTest/.../data/repository/UserRepositoryIntegrationTest.kt` — end-to-end register→login.

See [`TESTING.md`](TESTING.md) for the full matrix.