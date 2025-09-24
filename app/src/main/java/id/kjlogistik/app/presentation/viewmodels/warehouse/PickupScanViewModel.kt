package id.kjlogistik.app.presentation.viewmodels.warehouse

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.kjlogistik.app.data.repository.AuthRepository
import id.kjlogistik.app.data.session.SessionManager
import id.kjlogistik.app.util.BarcodeScannerUtil // Import the new utility
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

data class PickupScanUiState(
    val scannedRawValue: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val scanSuccessMessage: String? = null,
    val isScanSuccessful: Boolean? = null
)

@HiltViewModel
class PickupScanViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context,
    private val barcodeScannerUtil: BarcodeScannerUtil // Inject the utility
) : ViewModel() {

    private val _uiState = MutableStateFlow(PickupScanUiState())
    val uiState: StateFlow<PickupScanUiState> = _uiState.asStateFlow()

    fun startQrCodeScan(isDamaged: Boolean) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            scanSuccessMessage = null,
            isScanSuccessful = null,
            scannedRawValue = null
        )

        viewModelScope.launch {
            val result = barcodeScannerUtil.scanBarcode()
            result.onSuccess { scannedQrCodeContent ->
                // This is the call you pointed out. Now it calls the real function below.
                processScannedCode(scannedQrCodeContent, isDamaged)
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Scan failed: ${it.message}",
                    isScanSuccessful = false
                )
                delayResetState()
            }
        }
    }

    // --- THIS IS THE MISSING FUNCTION ---
    private fun processScannedCode(scannedQrCodeContent: String, isDamaged: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(scannedRawValue = scannedQrCodeContent)

            val authToken = sessionManager.fetchAuthToken()
            if (authToken == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Authentication token not found. Please log in.",
                    isScanSuccessful = false
                )
                delayResetState()
                return@launch
            }

            when (val result = authRepository.pickupScanPackage(
                qrCodeContent = scannedQrCodeContent,
                isDamaged = isDamaged
            )) {
                is AuthRepository.ScanResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        scanSuccessMessage = result.message,
                        isScanSuccessful = true
                    )
                    Log.d("PickupScanViewModel", "Scan successful: ${result.message}")
                    delayResetState()
                }
                is AuthRepository.ScanResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message,
                        isScanSuccessful = false
                    )
                    Log.e("PickupScanViewModel", "Scan failed: ${result.message}")
                    delayResetState()
                }
            }
        }
    }
    // --- END OF FIX ---


    private fun delayResetState() {
        viewModelScope.launch {
            delay(3000)
            _uiState.value = _uiState.value.copy(
                scannedRawValue = null,
                errorMessage = null,
                scanSuccessMessage = null,
                isScanSuccessful = null
            )
        }
    }
}