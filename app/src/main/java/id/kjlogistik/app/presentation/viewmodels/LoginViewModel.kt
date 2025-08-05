package id.kjlogistik.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.kjlogistik.app.data.model.LoginRequest
import id.kjlogistik.app.data.repository.AuthRepository // Keep this import for AuthRepository
// import id.kjlogistik.app.data.repository.Result // REMOVE: This is the problematic import
import id.kjlogistik.app.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.util.Log

// Represents the UI state of the login screen
data class LoginUiState(
    val usernameInput: String = "",
    val passwordInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val authToken: String? = null // This will be fetched from SessionManager if needed
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
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
            val password = _uiState.value.passwordInput

            // Basic validation (you'd expand this)
            if (username.isBlank() || password.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Username and password cannot be empty."
                )
                return@launch
            }

            val request = LoginRequest(username, password)
            // CORRECTED: Use AuthRepository.LoginResult
            when (val result = authRepository.login(request)) {
                is AuthRepository.LoginResult.Success -> {
                    // Token is saved within AuthRepository, so we fetch it from SessionManager
                    val token = sessionManager.fetchAuthToken()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        authToken = token, // Update UI state with the fetched token
                        errorMessage = null
                    )
                    Log.d("LoginViewModel", "Login successful! Message: ${result.message}")
                }
                is AuthRepository.LoginResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message // Use the message from the error result
                    )
                    Log.e("LoginViewModel", "Login failed: ${result.message}")
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
