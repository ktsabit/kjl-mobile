package id.kjlogistik.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kjlogistik.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OutboundScanPackageUiState(
    val manifestId: String = "",
    val manifestNumber: String = "",
    val totalPackages: Int = 0,
    val scannedPackagesCount: Int = 0,
    val isScanning: Boolean = false,
    val scanSuccessMessage: String? = null,
    val errorMessage: String? = null,
    val finalAlertMessage: String? = null
)

@HiltViewModel
class OutboundScanPackageViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(OutboundScanPackageUiState())
    val uiState: StateFlow<OutboundScanPackageUiState> = _uiState.asStateFlow()

    fun setManifestDetails(manifestId: String, manifestNumber: String, totalPackages: Int, scannedPackagesCount: Int) {
        _uiState.value = _uiState.value.copy(
            manifestId = manifestId,
            manifestNumber = manifestNumber,
            totalPackages = totalPackages,
            scannedPackagesCount = scannedPackagesCount
        )
    }

    fun scanPackage(qrCodeContent: String) {
        _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null)
        viewModelScope.launch {
            if (_uiState.value.scannedPackagesCount >= _uiState.value.totalPackages) {
                _uiState.value = _uiState.value.copy(isScanning = false, errorMessage = "All packages for this manifest have been scanned.")
                return@launch
            }

            when (val result = authRepository.departureScanPackage(
                manifestId = _uiState.value.manifestId,
                qrCodeContent = qrCodeContent
            )) {
                is AuthRepository.ManifestScanResult.Success -> {
                    val newCount = _uiState.value.scannedPackagesCount + 1
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        scannedPackagesCount = newCount,
                        scanSuccessMessage = result.message
                    )
                    if (newCount == _uiState.value.totalPackages) {
                        _uiState.value = _uiState.value.copy(
                            finalAlertMessage = "Manifest Departed Successfully!",
                            scanSuccessMessage = null
                        )
                    }
                }
                is AuthRepository.ManifestScanResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun handleScanFailure(message: String) {
        _uiState.value = _uiState.value.copy(
            isScanning = false,
            errorMessage = message
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(scanSuccessMessage = null, errorMessage = null)
    }
}