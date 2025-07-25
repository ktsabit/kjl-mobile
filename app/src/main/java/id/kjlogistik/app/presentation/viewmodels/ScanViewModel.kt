package id.kjlogistik.app.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.kjlogistik.app.data.model.ScanRequest
import id.kjlogistik.app.data.repository.AuthRepository
import id.kjlogistik.app.data.repository.Result
import id.kjlogistik.app.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay // For delayed state reset
import android.util.Log // For debugging
import javax.inject.Inject

// Represents the UI state for the Scan Screen
data class ScanUiState(
    val scannedRawValue: String? = null, // The raw string value from the QR code
    val isLoading: Boolean = false,      // Indicates if an operation (scan, API call) is in progress
    val errorMessage: String? = null,    // Message for user on error
    val scanSuccessMessage: String? = null, // Message for user on successful scan
    val isScanSuccessful: Boolean? = null // null: initial/reset, true: success, false: failure
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context // Context is needed for GmsBarcodeScanning.getClient
) : ViewModel() {

    // MutableStateFlow to hold and expose the UI state to Composables
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    // Configuration for Google ML Kit Barcode Scanner
    private val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE) // Only interested in QR codes
        .enableAutoZoom() // Enable auto-zoom for better scanning
        .build()

    // Get an instance of the GMS Barcode Scanner client
    private val scanner = GmsBarcodeScanning.getClient(context, options)

    /**
     * Initiates the QR code scanning process using Google ML Kit.
     * Updates UI state based on scan outcome (success, cancel, failure).
     */
    fun startQrCodeScan() {
        // Reset relevant UI state before starting a new scan
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            scanSuccessMessage = null,
            isScanSuccessful = null, // Clear previous scan result
            scannedRawValue = null // Clear previous raw value display
        )

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                _uiState.value = _uiState.value.copy(scannedRawValue = rawValue)

                if (!rawValue.isNullOrBlank()) {
                    scanAndSendToServer(rawValue) // Process and send valid raw value to backend
                } else {
                    // Handle case where scanned barcode is empty/null (unlikely for QR but good practice)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Scanned barcode is empty.",
                        isScanSuccessful = false // Indicate scan process failed to get valid data
                    )
                    Log.e("ScanViewModel", "Scanned barcode raw value is null or empty.")
                    delayResetState() // Reset UI after showing message
                }
            }
            .addOnCanceledListener {
                // User cancelled the scan operation
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Scan cancelled.",
                    isScanSuccessful = null // Keep it in initial state on cancel
                )
                Log.d("ScanViewModel", "QR code scan cancelled by user.")
            }
            .addOnFailureListener { e ->
                // An error occurred during the scan process itself (e.g., camera issue)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to start scanner: ${e.localizedMessage ?: e.message}",
                    isScanSuccessful = false // Indicate scanner failure
                )
                Log.e("ScanViewModel", "QR code scan failed: ${e.message}", e)
                delayResetState() // Reset UI after showing message
            }
    }

    /**
     * Parses the raw QR code value and sends the extracted data to the backend.
     * This function handles the "shpid:batchid" format.
     */
    private fun scanAndSendToServer(qrRawValue: String) {
        val authToken = sessionManager.fetchAuthToken()
        if (authToken.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isScanSuccessful = false,
                errorMessage = "Authentication token missing. Please log in again."
            )
            Log.e("ScanViewModel", "No authentication token found. User likely not logged in or session expired.")
            delayResetState() // Reset UI after showing message
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, scanSuccessMessage = null) // Ensure loading state is active
            try {
                // Parse the "shipping_id:batch_id" format
                val parts = qrRawValue.split(":")

                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    val shippingIdFromQr = parts[0]
                    val batchIdFromQr = parts[1]

                    // Create the ScanRequest object.
                    // IMPORTANT: Ensure the order matches your ScanRequest's constructor
                    // and its @SerializedName annotations.
                    // If ScanRequest is (batchId, shippingId) and QR is (shipping:batch),
                    // then: ScanRequest(batchId = batchIdFromQr, shippingId = shippingIdFromQr)
                    // Based on your previous example "12jH381w:Je012nb3" (shipping:batch),
                    // and your ScanRequest data class (@SerializedName("batch_id") val batchId, @SerializedName("shipping_id") val shippingId):
                    val request = ScanRequest(
                        batchId = batchIdFromQr,
                        shippingId = shippingIdFromQr
                    )

                    // Call the repository to send data to the API
                    val result = authRepository.scanQrCode(authToken, request)

                    when (result) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isScanSuccessful = true,
                                scanSuccessMessage = result.data.message,
                                errorMessage = null
                            )
                            Log.i("ScanViewModel", "Scan successful: ${result.data.message}")
                            delayResetState() // Schedule UI reset after showing success
                        }
                        is Result.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isScanSuccessful = false,
                                errorMessage = result.message ?: "Failed to send scan data to server."
                            )
                            Log.e("ScanViewModel", "Scan API error: ${result.message}", result.exception)
                            delayResetState() // Schedule UI reset after showing error
                        }
                        Result.Loading -> {
                            // This state is managed by isLoading. No specific action needed here.
                        }
                    }
                } else {
                    // QR data is not in the expected "shpid:batchid" format
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isScanSuccessful = false,
                        errorMessage = "Invalid QR data format. Expected 'shippingId:batchId'."
                    )
                    Log.e("ScanViewModel", "QR data parsing error: Unexpected format. Raw: $qrRawValue")
                    delayResetState() // Schedule UI reset after showing error
                }
            } catch (e: Exception) {
                // Catch any unexpected exceptions during parsing or API call
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isScanSuccessful = false,
                    errorMessage = "An unexpected error occurred: ${e.localizedMessage ?: e.message}"
                )
                Log.e("ScanViewModel", "Unexpected error during scan operation: ${e.message}", e)
                delayResetState() // Schedule UI reset after showing error
            }
        }
    }

    /**
     * Resets the UI state back to its initial/default values.
     * This is intended for clearing the screen after a result has been displayed.
     */
    fun resetScanState() {
        _uiState.value = ScanUiState()
    }

    /**
     * Schedules a delayed reset of the UI state to allow visual feedback to persist.
     */
    private fun delayResetState() {
        viewModelScope.launch {
            delay(3000L) // Adjust duration (in milliseconds) as per desired visibility
            _uiState.value = ScanUiState() // Reset state to clear messages/icons
        }
    }
}