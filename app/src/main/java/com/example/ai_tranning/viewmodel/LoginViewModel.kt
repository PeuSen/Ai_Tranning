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
 * Immutable UI state for the login screen, exposed via [LoginViewModel.uiState].
 *
 * @property username current value of the username field.
 * @property password current value of the password field.
 * @property isLoading `true` while a login request is in flight (drives the progress indicator).
 * @property errorMessage validation or authentication error to display, or `null` if none.
 * @property isLoggedIn one-shot success flag the screen observes to navigate to the dashboard.
 */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

/**
 * ViewModel backing the login screen.
 *
 * Holds form state in a [StateFlow], performs lightweight client-side validation, and delegates
 * credential checking to [UserRepository]. On success it persists the user id via [SessionManager]
 * so the session survives app restarts, then flips [LoginUiState.isLoggedIn].
 *
 * @property userRepository authentication operations.
 * @property sessionManager persists the logged-in user id.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())

    /** Observable login screen state. */
    val uiState: StateFlow<LoginUiState> = _uiState

    /** Updates the username field and clears any visible error. */
    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(username = username, errorMessage = null)
    }

    /** Updates the password field and clears any visible error. */
    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    /**
     * Validates the form and attempts to log in.
     *
     * If either field is blank, sets a validation error and returns without calling the repository.
     * Otherwise runs the login in [viewModelScope]: on success persists the session and sets
     * [LoginUiState.isLoggedIn]; on failure surfaces the repository's error message. [LoginUiState.isLoading]
     * is toggled around the request.
     */
    fun login() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please fill in all fields")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            userRepository.loginUser(state.username, state.password)
                .onSuccess { user ->
                    sessionManager.saveUserId(user.id)
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
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