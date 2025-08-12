package id.kjlogistik.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kjlogistik.app.data.model.LoginRequest
import id.kjlogistik.app.data.repository.AuthRepository
import id.kjlogistik.app.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

data class LoginUiState(
    val usernameInput: String = "",
    val passwordInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val authToken: String? = null,
    val userGroups: List<String> = emptyList()
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    val sessionManager: SessionManager,

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

            if (username.isBlank() || password.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Username and password cannot be empty."
                )
                return@launch
            }

            val request = LoginRequest(username, password)
            when (val result = authRepository.login(request)) {
                is AuthRepository.LoginResult.Success -> {
                    val token = sessionManager.fetchAuthToken()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        authToken = token,
                        errorMessage = null,
                        userGroups = result.groups,
                    )
                    Log.d("LoginViewModel", "Login successful! Message: ${result.message}")
                }
                is AuthRepository.LoginResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                    Log.e("LoginViewModel", "Login failed: ${result.message}")
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState()
    }
}