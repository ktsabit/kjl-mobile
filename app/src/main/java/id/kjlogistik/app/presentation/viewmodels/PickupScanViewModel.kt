package id.kjlogistik.app.presentation.viewmodels

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
    @ApplicationContext private val context: Context
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
            try {
                val options = GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                val scanner = GmsBarcodeScanning.getClient(context, options)

                val scannedQrCodeContent = suspendCancellableCoroutine<String?> { continuation ->
                    scanner.startScan()
                        .addOnSuccessListener { barcode ->
                            continuation.resume(barcode.rawValue)
                        }
                        .addOnFailureListener { e ->
                            val errorMessage = "Scanner failed: ${e.message ?: "An unexpected error occurred."}"
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = errorMessage,
                                isScanSuccessful = false
                            )
                            continuation.resume(null)
                        }
                        .addOnCanceledListener {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "QR code scan cancelled.",
                                isScanSuccessful = false
                            )
                            continuation.resume(null)
                        }
                }

                if (scannedQrCodeContent.isNullOrBlank()) {
                    if (_uiState.value.errorMessage == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "QR code scan cancelled or no content found.",
                            isScanSuccessful = false
                        )
                    }
                    delayResetState()
                    return@launch
                }

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



                val currentHubId = authRepository.getUserMe("Bearer $authToken");

                Log.d("PickupScanViewModel", "Current Hub ID: ${currentHubId.body()}")


                when (val result = authRepository.pickupScanPackage(
                    qrCodeContent = scannedQrCodeContent,
                    isDamaged = isDamaged,
                    locationHubId = currentHubId.body()?.hub ?: ""
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
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error during scan process: ${e.message ?: "Unknown error"}",
                    isScanSuccessful = false
                )
                Log.e("PickupScanViewModel", "Exception during scan process: ${e.message}", e)
                delayResetState()
            }
        }
    }

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