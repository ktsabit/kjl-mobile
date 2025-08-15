package id.kjlogistik.app.presentation.viewmodels.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kjlogistik.app.data.model.Manifest
import id.kjlogistik.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboundScanUiState(
    val manifests: List<Manifest> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class InboundScanViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboundScanUiState())
    val uiState: StateFlow<InboundScanUiState> = _uiState.asStateFlow()

    fun fetchArrivalManifests() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = authRepository.getManifestsForArrival()) {
                is AuthRepository.ManifestListResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        manifests = result.manifests
                    )
                }
                is AuthRepository.ManifestListResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
}