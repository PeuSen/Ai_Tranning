package com.example.ai_tranning.viewmodel

import com.example.ai_tranning.data.local.entity.UserEntity
import com.example.ai_tranning.data.repository.UserRepository
import com.example.ai_tranning.util.MainDispatcherRule
import com.example.ai_tranning.utils.SessionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [LoginViewModel].
 *
 * Both collaborators — [UserRepository] and [SessionManager] — are mocked. The
 * [MainDispatcherRule] installs an unconfined test dispatcher so the coroutine launched by
 * [LoginViewModel.login] runs synchronously and the resulting [LoginUiState] can be asserted
 * immediately.
 *
 * Coverage:
 *  - Input binding (`onUsernameChange` / `onPasswordChange` clear stale errors).
 *  - Client-side validation (blank fields are rejected before any repository call).
 *  - Positive: valid credentials log in and persist the session.
 *  - Negative: invalid credentials surface the repository error and do not persist a session.
 *  - State transitions: `isLoading` toggles correctly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: UserRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        repository = mockk()
        sessionManager = mockk(relaxed = true) // saveUserId is fire-and-forget here
        viewModel = LoginViewModel(repository, sessionManager)
    }

    @Test
    fun `initial state is empty and idle`() {
        val state = viewModel.uiState.value
        assertEquals("", state.username)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertFalse(state.isLoggedIn)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onUsernameChange updates username and clears previous error`() {
        viewModel.login() // produces a validation error (blank fields)
        assertEquals("Please fill in all fields", viewModel.uiState.value.errorMessage)

        viewModel.onUsernameChange("alice")

        assertEquals("alice", viewModel.uiState.value.username)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onPasswordChange updates password and clears previous error`() {
        viewModel.login()
        assertEquals("Please fill in all fields", viewModel.uiState.value.errorMessage)

        viewModel.onPasswordChange("secret")

        assertEquals("secret", viewModel.uiState.value.password)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `login with blank username shows validation error and skips repository`() = runTest {
        viewModel.onPasswordChange("secret")

        viewModel.login()

        assertEquals("Please fill in all fields", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoggedIn)
        coVerify(exactly = 0) { repository.loginUser(any(), any()) }
    }

    @Test
    fun `login with blank password shows validation error and skips repository`() = runTest {
        viewModel.onUsernameChange("alice")

        viewModel.login()

        assertEquals("Please fill in all fields", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { repository.loginUser(any(), any()) }
    }

    @Test
    fun `login with whitespace-only fields is treated as blank`() = runTest {
        viewModel.onUsernameChange("   ")
        viewModel.onPasswordChange("   ")

        viewModel.login()

        assertEquals("Please fill in all fields", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { repository.loginUser(any(), any()) }
    }

    @Test
    fun `successful login sets isLoggedIn and saves the session`() = runTest {
        val user = UserEntity(id = 11, username = "alice", email = "a@x.com", passwordHash = "h")
        coEvery { repository.loginUser("alice", "secret") } returns Result.success(user)
        viewModel.onUsernameChange("alice")
        viewModel.onPasswordChange("secret")

        viewModel.login()

        val state = viewModel.uiState.value
        assertTrue(state.isLoggedIn)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        coVerify(exactly = 1) { sessionManager.saveUserId(11) }
    }

    @Test
    fun `failed login surfaces error and does not save a session`() = runTest {
        coEvery { repository.loginUser(any(), any()) } returns
            Result.failure(Exception("Invalid username or password"))
        viewModel.onUsernameChange("alice")
        viewModel.onPasswordChange("wrong")

        viewModel.login()

        val state = viewModel.uiState.value
        assertFalse(state.isLoggedIn)
        assertFalse(state.isLoading)
        assertEquals("Invalid username or password", state.errorMessage)
        coVerify(exactly = 0) { sessionManager.saveUserId(any()) }
    }

    @Test
    fun `login is not loading after completion`() = runTest {
        coEvery { repository.loginUser(any(), any()) } returns
            Result.success(UserEntity(id = 1, username = "a", email = "a@x.com", passwordHash = "h"))
        viewModel.onUsernameChange("a")
        viewModel.onPasswordChange("b")

        viewModel.login()

        assertFalse(viewModel.uiState.value.isLoading)
    }
}