package com.example.ai_tranning.viewmodel

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
 * Unit tests for [RegisterViewModel].
 *
 * Collaborators are mocked and [MainDispatcherRule] makes the `register` coroutine run
 * synchronously.
 *
 * Coverage:
 *  - Input binding clears stale errors.
 *  - Validation ladder: required fields -> password match -> minimum length, in that order.
 *  - Positive: valid input registers and persists the session.
 *  - Negative: repository failures (e.g. duplicate username/email) surface to the UI state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: UserRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        repository = mockk()
        sessionManager = mockk(relaxed = true)
        viewModel = RegisterViewModel(repository, sessionManager)
    }

    /** Fills the form with a valid, internally-consistent set of values. */
    private fun fillValidForm(
        username: String = "alice",
        email: String = "alice@example.com",
        password: String = "password123",
        confirm: String = "password123"
    ) {
        viewModel.onUsernameChange(username)
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onConfirmPasswordChange(confirm)
    }

    @Test
    fun `initial state is empty and idle`() {
        val state = viewModel.uiState.value
        assertEquals("", state.username)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertFalse(state.isLoading)
        assertFalse(state.isRegistered)
        assertNull(state.errorMessage)
    }

    @Test
    fun `field changes clear previous error`() {
        viewModel.register() // blank -> error
        assertEquals("Please fill in all fields", viewModel.uiState.value.errorMessage)

        viewModel.onEmailChange("a@x.com")

        assertEquals("a@x.com", viewModel.uiState.value.email)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ---------------------------------------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `register with any blank required field shows fill-in error`() = runTest {
        fillValidForm(email = "") // email missing

        viewModel.register()

        assertEquals("Please fill in all fields", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { repository.registerUser(any(), any(), any()) }
    }

    @Test
    fun `register with mismatched passwords shows mismatch error`() = runTest {
        fillValidForm(password = "password123", confirm = "different")

        viewModel.register()

        assertEquals("Passwords do not match", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { repository.registerUser(any(), any(), any()) }
    }

    @Test
    fun `register with short password shows minimum-length error`() = runTest {
        fillValidForm(password = "12345", confirm = "12345") // 5 chars < 6

        viewModel.register()

        assertEquals("Password must be at least 6 characters", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { repository.registerUser(any(), any(), any()) }
    }

    @Test
    fun `password of exactly six characters passes the length check`() = runTest {
        coEvery { repository.registerUser(any(), any(), any()) } returns Result.success(1L)
        fillValidForm(password = "123456", confirm = "123456")

        viewModel.register()

        coVerify(exactly = 1) { repository.registerUser("alice", "alice@example.com", "123456") }
    }

    @Test
    fun `validation order - missing field beats password mismatch`() = runTest {
        // Username blank AND passwords mismatched: the fill-in error must take precedence.
        fillValidForm(username = "", password = "password123", confirm = "different")

        viewModel.register()

        assertEquals("Please fill in all fields", viewModel.uiState.value.errorMessage)
    }

    // ---------------------------------------------------------------------------------------------
    // Positive / negative outcomes
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `successful registration sets isRegistered and saves the session`() = runTest {
        coEvery { repository.registerUser("alice", "alice@example.com", "password123") } returns
            Result.success(99L)
        fillValidForm()

        viewModel.register()

        val state = viewModel.uiState.value
        assertTrue(state.isRegistered)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        coVerify(exactly = 1) { sessionManager.saveUserId(99) }
    }

    @Test
    fun `duplicate username failure surfaces to the UI state`() = runTest {
        coEvery { repository.registerUser(any(), any(), any()) } returns
            Result.failure(Exception("Username already exists"))
        fillValidForm()

        viewModel.register()

        val state = viewModel.uiState.value
        assertFalse(state.isRegistered)
        assertFalse(state.isLoading)
        assertEquals("Username already exists", state.errorMessage)
        coVerify(exactly = 0) { sessionManager.saveUserId(any()) }
    }

    @Test
    fun `duplicate email failure surfaces to the UI state`() = runTest {
        coEvery { repository.registerUser(any(), any(), any()) } returns
            Result.failure(Exception("Email already exists"))
        fillValidForm()

        viewModel.register()

        assertEquals("Email already exists", viewModel.uiState.value.errorMessage)
    }
}