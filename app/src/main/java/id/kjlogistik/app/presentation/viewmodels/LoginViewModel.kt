package id.kjlogistik.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.kjlogistik.app.data.model.LoginRequest
import id.kjlogistik.app.data.repository.AuthRepository
import id.kjlogistik.app.data.repository.Result
import id.kjlogistik.app.data.session.SessionManager // NEW
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.util.Log // For debugging

// Represents the UI state of the login screen
data class LoginUiState(
    val usernameInput: String = "",
    val passwordInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val authToken: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager // NEW: Inject SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(newUsername: String) {
        _uiState.value = _uiState.value.copy(usernameInput = newUsername, errorMessage = null)
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.value = _uiState.value.copy(passwordInput = newPassword, errorMessage = null)
    }

    fun login() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val username = _uiState.value.usernameInput
            val password = _uiState.value.passwordInput // No need for copy() here, it's a simple string

            // Basic validation (you'd expand this)
            if (username.isBlank() || password.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Username and password cannot be empty."
                )
                return@launch
            }

            val request = LoginRequest(username, password)
            when (val result = authRepository.login(request)) {
                is Result.Success -> {
                    val token = result.data.authToken
                    sessionManager.saveAuthToken(token) // NEW: Save token to session manager
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        authToken = token, // Keep this here for UI observer
                        errorMessage = null
                    )
                    Log.d("LoginViewModel", "Login successful! Token saved: $token")
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message ?: "An unknown error occurred during login."
                    )
                    Log.e("LoginViewModel", "Login failed: ${result.message}", result.exception)
                }
                Result.Loading -> {
                    // This state is handled by setting isLoading to true before the call
                    // No further action needed here for this specific sealed class setup
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState() // Resets all fields and states
        // Also clear session token if you want to force re-login on state reset
        // sessionManager.clearAuthToken() // OPTIONAL: uncomment if you want reset to log out
    }
}