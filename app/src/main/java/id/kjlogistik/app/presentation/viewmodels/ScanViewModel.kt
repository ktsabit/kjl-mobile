package id.kjlogistik.app.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
//import com.google.mlkit.vision.codescanner.GmsBarcodeScannerException // NEW: Import for specific scanner exceptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.kjlogistik.app.data.repository.AuthRepository
import id.kjlogistik.app.data.session.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine // NEW: For bridging callbacks to coroutines
import javax.inject.Inject
import kotlin.coroutines.resume

// Represents the UI state of the scan screen
data class ScanUiState(
    val scannedRawValue: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val scanSuccessMessage: String? = null,
    val isScanSuccessful: Boolean? = null // null: initial/reset, true: success, false: failure
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    // Function to start the QR code scan using ML Kit
    fun startQrCodeScan(isDamaged: Boolean) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            scanSuccessMessage = null,
            isScanSuccessful = null,
            scannedRawValue = null // Clear previous scan value
        )

        viewModelScope.launch {
            try {
                // Configure ML Kit scanner options (e.g., only QR codes)
                val options = GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_CODE_128)
                    .build()
                val scanner = GmsBarcodeScanning.getClient(context, options)

                // Use suspendCancellableCoroutine to convert the callback-based ML Kit scan
                // into a suspend function, making it easier to work with coroutines.
                val scannedQrCodeContent = suspendCancellableCoroutine<String?> { continuation ->
                    scanner.startScan()
                        .addOnSuccessListener { barcode ->
                            // If scan is successful, resume the coroutine with the raw value
                            continuation.resume(barcode.rawValue)
                        }
                        .addOnFailureListener { e ->
                            // Handle specific scanner errors and update UI state
                            val errorMessage = when (e) {
//                                is GmsBarcodeScannerException -> {
//                                    when (e.errorCode) {
//                                        GmsBarcodeScanner.ERROR_CAMERA_PERMISSION_DENIED -> "Camera permission denied. Please grant access in settings."
//                                        GmsBarcodeScanner.ERROR_SCANNER_NOT_AVAILABLE -> "Barcode scanner not available on this device."
//                                        else -> "Barcode scanner error: ${e.message ?: "Unknown error"}"
//                                    }
//                                }
                                else -> "Scanner failed: ${e.message ?: "An unexpected error occurred."}"
                            }
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = errorMessage,
                                isScanSuccessful = false
                            )
                            // Resume with null to indicate scan failure/cancellation
                            continuation.resume(null)
                        }
                        .addOnCanceledListener {
                            // Handle scan cancellation (e.g., user pressed back button)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "QR code scan cancelled.",
                                isScanSuccessful = false
                            )
                            // Resume with null to indicate scan cancellation
                            continuation.resume(null)
                        }
                }

                // If scanned content is null or blank, it means the scan was cancelled or failed
                if (scannedQrCodeContent.isNullOrBlank()) {
                    // If an error message was already set by the failure/cancellation listener,
                    // we don't need to set a generic one here.
                    if (_uiState.value.errorMessage == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "QR code scan cancelled or no content found.",
                            isScanSuccessful = false
                        )
                    }
                    delayResetState() // Reset UI state after showing the message
                    return@launch // Exit the coroutine
                }

                _uiState.value = _uiState.value.copy(scannedRawValue = scannedQrCodeContent)

                // Example hub ID (replace with actual logic to get the current hub ID)
                val currentHubId = "cd98ba56-00af-4c62-9bdb-88d024f9aaaa"

                // Proceed with the API call using the scanned content
                when (val result = authRepository.inboundScanPackage(
                    qrCodeContent = scannedQrCodeContent,
                    isDamaged = isDamaged,
                    locationHubId = currentHubId
                )) {
                    is AuthRepository.ScanResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            scanSuccessMessage = result.message,
                            isScanSuccessful = true
                        )
                        Log.d("ScanViewModel", "Scan successful: ${result.message}")
                        delayResetState()
                    }
                    is AuthRepository.ScanResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            isScanSuccessful = false
                        )
                        Log.e("ScanViewModel", "Scan failed: ${result.message}")
                        delayResetState()
                    }
                }
            } catch (e: Exception) {
                // Catch any other unexpected exceptions during the entire scan process
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error during scan process: ${e.message ?: "Unknown error"}",
                    isScanSuccessful = false
                )
                Log.e("ScanViewModel", "Exception during scan process: ${e.message}", e)
                delayResetState()
            }
        }
    }

    // Resets the UI state after a short delay to show success/failure message
    private fun delayResetState() {
        viewModelScope.launch {
            delay(3000) // Show success/error for 3 seconds
            _uiState.value = _uiState.value.copy(
                scannedRawValue = null,
                errorMessage = null,
                scanSuccessMessage = null,
                isScanSuccessful = null
            )
        }
    }

    // You might want a public reset function for external triggers if needed
    fun resetScanState() {
        _uiState.value = ScanUiState()
    }
}
