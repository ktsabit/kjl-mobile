// In: app/src/main/java/id/kjlogistik/app/presentation/viewmodels/auth/ProfileViewModel.kt

package id.kjlogistik.app.presentation.viewmodels.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.kjlogistik.app.R // <-- CRITICAL: MAKE SURE THIS IMPORT IS CORRECT
import id.kjlogistik.app.data.repository.AuthRepository
import id.kjlogistik.app.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val username: String = "",
    val hubName: String = "",
    val fullName: String = "",
    val groups: List<String> = emptyList(),
    val appVersion: String = ""
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val token = sessionManager.fetchAuthToken()
            var version = ""
            try {
                // Reading the resource. If this fails, version will be "N/A"
                version = context.getString(R.string.app_version_name)
            } catch (e: Exception) {
                version = "N/A"
            }

            if (token != null) {
                val userDetails = authRepository.getUserMe("Bearer $token")
                if (userDetails.isSuccessful) {
                    val user = userDetails.body()
                    _uiState.value = _uiState.value.copy(
                        username = user?.username ?: "Unknown",
                        hubName = user?.hubName ?: "Unknown",
                        fullName = user?.fullName ?: "Unknown",
                        groups = user?.groups ?: emptyList(),
                        appVersion = version
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(appVersion = version)
            }
        }
    }
}