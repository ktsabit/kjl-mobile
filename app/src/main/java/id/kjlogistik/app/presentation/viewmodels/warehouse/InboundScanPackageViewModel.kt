package id.kjlogistik.app.presentation.viewmodels.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.kjlogistik.app.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboundScanPackageUiState(
    val manifestId: String = "",
    val manifestNumber: String = "",
    val totalPackages: Int = 0,
    val scannedPackagesCount: Int = 0,
    val isScanning: Boolean = false,
    val scanSuccessMessage: String? = null,
    val errorMessage: String? = null,
    val finalAlertMessage: String? = null,
    val isDuplicateScan: Boolean = false // State for duplicate scan error
)

@HiltViewModel
class InboundScanPackageViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(InboundScanPackageUiState())
    val uiState: StateFlow<InboundScanPackageUiState> = _uiState.asStateFlow()

    fun setManifestDetails(manifestId: String, manifestNumber: String, totalPackages: Int, scannedPackagesCount: Int) {
        _uiState.value = _uiState.value.copy(
            manifestId = manifestId,
            manifestNumber = manifestNumber,
            totalPackages = totalPackages,
            scannedPackagesCount = scannedPackagesCount
        )
    }

    fun scanPackage(qrCodeContent: String) {
        _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null, isDuplicateScan = false)
        viewModelScope.launch {
            if (_uiState.value.scannedPackagesCount >= _uiState.value.totalPackages) {
                _uiState.value = _uiState.value.copy(isScanning = false, errorMessage = "All packages for this manifest have been scanned.")
                return@launch
            }

            when (val result = authRepository.arrivalScanPackage(
                manifestId = _uiState.value.manifestId,
                qrCodeContent = qrCodeContent
            )) {
                is AuthRepository.ManifestScanResult.Success -> {
                    val newCount = _uiState.value.scannedPackagesCount + 1
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        scannedPackagesCount = newCount,
                        scanSuccessMessage = "Waybill Complete"
                    )
                    if (newCount == _uiState.value.totalPackages) {
                        _uiState.value = _uiState.value.copy(
                            finalAlertMessage = "Manifest Received Successfully!",
                            scanSuccessMessage = null
                        )
                    }
                }
                is AuthRepository.ManifestScanResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        errorMessage = result.message,
                        isDuplicateScan = result.isDuplicate
                    )
                    if (result.isDuplicate) {
                        delay(1500) // Keep the error state for 1.5 seconds
                        _uiState.value = _uiState.value.copy(isDuplicateScan = false, errorMessage = null)
                    }
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