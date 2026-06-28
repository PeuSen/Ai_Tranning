package com.example.ai_tranning.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_tranning.data.repository.UserRepository
import com.example.ai_tranning.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Immutable UI state for the registration screen, exposed via [RegisterViewModel.uiState].
 *
 * @property username current username field value.
 * @property email current email field value.
 * @property password current password field value.
 * @property confirmPassword current password-confirmation field value.
 * @property isLoading `true` while a registration request is in flight.
 * @property errorMessage validation or registration error to display, or `null` if none.
 * @property isRegistered one-shot success flag the screen observes to navigate onward.
 */
data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistered: Boolean = false
)

/**
 * ViewModel backing the registration screen.
 *
 * Owns form state and the client-side validation ladder, then delegates account creation to
 * [UserRepository]. On success the new user is auto-logged-in: the id is persisted via
 * [SessionManager] and [RegisterUiState.isRegistered] is set.
 *
 * @property userRepository registration operations.
 * @property sessionManager persists the new user's id.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())

    /** Observable registration screen state. */
    val uiState: StateFlow<RegisterUiState> = _uiState

    /** Updates the username field and clears any visible error. */
    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, errorMessage = null)
    }

    /** Updates the email field and clears any visible error. */
    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    /** Updates the password field and clears any visible error. */
    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    /** Updates the password-confirmation field and clears any visible error. */
    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, errorMessage = null)
    }

    /**
     * Validates the form and attempts to register the account.
     *
     * Validation runs in order and stops at the first failure:
     * 1. all required fields (username, email, password) must be non-blank;
     * 2. [RegisterUiState.password] and [RegisterUiState.confirmPassword] must match;
     * 3. the password must be at least 6 characters.
     *
     * If validation passes, registration runs in [viewModelScope]: on success the session is saved
     * and [RegisterUiState.isRegistered] is set; on failure (e.g. duplicate username/email) the
     * repository's error message is surfaced.
     */
    fun register() {
        val state = _uiState.value
        if (state.username.isBlank() || state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please fill in all fields")
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(errorMessage = "Passwords do not match")
            return
        }
        if (state.password.length < 6) {
            _uiState.value = state.copy(errorMessage = "Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            userRepository.registerUser(state.username, state.email, state.password)
                .onSuccess { userId ->
                    sessionManager.saveUserId(userId)
                    _uiState.value = _uiState.value.copy(isLoading = false, isRegistered = true)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
        }
    }
}