package id.kjlogistik.app.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.kjlogistik.app.R
import id.kjlogistik.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUpdateState(
    val showUpdateDialog: Boolean = false,
    val isForceUpdate: Boolean = false,
    val updateUrl: String? = null
)

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _updateState = MutableStateFlow(AppUpdateState())
    val updateState = _updateState.asStateFlow()

    fun checkForUpdates() {
        viewModelScope.launch {
            when (val result = authRepository.checkAppVersion()) {
                is AuthRepository.VersionCheckResult.Success -> {
                    val remoteVersion = result.response.latestVersion
                    val currentVersion = context.getString(R.string.app_version_name)

                    if (remoteVersion > currentVersion) {
                        _updateState.value = AppUpdateState(
                            showUpdateDialog = true,
                            isForceUpdate = result.response.isForceUpdate,
                            updateUrl = result.response.updateUrl
                        )
                    }
                }
                is AuthRepository.VersionCheckResult.Error -> {
                    // Fail silently. The app should still work if the check fails.
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateState.value = _updateState.value.copy(showUpdateDialog = false)
    }
}