package id.kjlogistik.app.presentation.viewmodels.driver

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.kjlogistik.app.data.model.Manifest
import id.kjlogistik.app.data.model.Waybill
import id.kjlogistik.app.data.repository.AuthRepository
import id.kjlogistik.app.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DriverUiState(
    val manifest: Manifest? = null,
    val waybills: List<Waybill> = emptyList(),
    val deliveredWaybillIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val hasActiveRun: Boolean = false,
    val isRunCompleted: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DriverViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriverUiState())
    val uiState: StateFlow<DriverUiState> = _uiState.asStateFlow()

    init {
        checkForActiveRun()
    }

    fun checkForActiveRun() {
        viewModelScope.launch {
            val activeManifestId = sessionManager.fetchActiveManifestId()
            if (activeManifestId != null) {
                _uiState.value = _uiState.value.copy(hasActiveRun = true)
                loadManifestDetails(activeManifestId)
            } else {
                // Explicitly reset the state when there's no active run
                _uiState.value = DriverUiState()
            }
        }
    }

    private fun loadManifestDetails(manifestId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val manifestResult = authRepository.getManifestDetails(manifestId)) {
                is AuthRepository.GetManifestResult.Success -> {
                    val manifest = manifestResult.manifest
                    _uiState.value = _uiState.value.copy(manifest = manifest)
                    // Make the second call for waybills
                    when (val waybillsResult = authRepository.getWaybillsForManifest(manifest.id)) {
                        is AuthRepository.GetWaybillsResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                waybills = waybillsResult.waybills
                            )
                        }
                        is AuthRepository.GetWaybillsResult.Error -> {
                            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = waybillsResult.message)
                        }
                    }
                }
                is AuthRepository.GetManifestResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = manifestResult.message)
                    sessionManager.clearActiveManifestId()
                    _uiState.value = _uiState.value.copy(hasActiveRun = false)
                }
            }
        }
    }

    fun createManifest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.createManifest()) {
                is AuthRepository.CreateManifestResult.Success -> {
                    sessionManager.saveActiveManifestId(result.manifest.id)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        manifest = result.manifest,
                        hasActiveRun = true,
                        waybills = emptyList(), // Ensure lists are cleared for the new run
                        deliveredWaybillIds = emptySet()
                    )
                    onSuccess()
                }
                is AuthRepository.CreateManifestResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun addWaybillToManifest(qrCodeContent: String) {
        val manifestId = _uiState.value.manifest?.id
        if (manifestId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "No active manifest found.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.addWaybillToManifest(manifestId, qrCodeContent)) {
                is AuthRepository.AddWaybillResult.Success -> {
                    // --- THIS IS THE FIX ---
                    // After successfully adding, immediately reload all data from the server
                    // to ensure the UI reflects the true, persisted state.
                    loadManifestDetails(manifestId)
                    Toast.makeText(context, "Waybill Added", Toast.LENGTH_SHORT).show()
                }
                is AuthRepository.AddWaybillResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun startRun(onSuccess: () -> Unit) {
        val manifestId = _uiState.value.manifest?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.startRun(manifestId)) {
                is AuthRepository.StartRunResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    loadManifestDetails(manifestId) // Refresh data to get "OUT_FOR_DELIVERY" status
                    onSuccess()
                }
                is AuthRepository.StartRunResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun markPackageAsDelivered(qrCodeContent: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.markPackageAsDelivered(qrCodeContent)) {
                is AuthRepository.MarkDeliveredResult.Success -> {
                    val waybillToMark = _uiState.value.waybills.find { wb -> qrCodeContent.contains(wb.waybillNumber ?: "a-a-a-a") }
                    if (waybillToMark != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            deliveredWaybillIds = _uiState.value.deliveredWaybillIds + waybillToMark.id
                        )
                        Toast.makeText(context, "Package Marked as Delivered!", Toast.LENGTH_SHORT).show()
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        Toast.makeText(context, "Package delivered, but could not match to a waybill.", Toast.LENGTH_LONG).show()
                    }
                }
                is AuthRepository.MarkDeliveredResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun startScanner(onScanResult: (String) -> Unit) {
        val scanner = GmsBarcodeScanning.getClient(context)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                barcode.rawValue?.let {
                    onScanResult(it)
                }
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(errorMessage = "Scan failed: ${e.message}")
            }
    }

    fun completeRun() {
        sessionManager.clearActiveManifestId()
        _uiState.value = DriverUiState() // Reset state
        Toast.makeText(context, "Run Completed!", Toast.LENGTH_LONG).show()
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}