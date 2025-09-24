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
import id.kjlogistik.app.util.BarcodeScannerUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DriverUiState(
    val manifest: Manifest? = null,
    val waybills: List<Waybill> = emptyList(),
    val deliveredWaybillNumbers: Set<String> = emptySet(),
    val scannedPackageIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val hasActiveRun: Boolean = false,
    val isRunCompleted: Boolean = false,
    val errorMessage: String? = null,
    val isDuplicateDelivery: Boolean = false
)

@HiltViewModel
class DriverViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context,
    private val barcodeScannerUtil: BarcodeScannerUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriverUiState())
    val uiState: StateFlow<DriverUiState> = _uiState.asStateFlow()
    private val scannedQRCodes = mutableSetOf<String>()
    private val deliveredQRCodes = mutableSetOf<String>()

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
                _uiState.value = DriverUiState()
            }
        }
    }

    private fun loadManifestDetails(manifestId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val manifestResult = authRepository.getManifestDetails(manifestId)) {
                is AuthRepository.GetManifestResult.Success -> {
                    val manifestFromApi = manifestResult.manifest
                    val correctedManifest = manifestFromApi.copy(id = manifestId)

                    _uiState.value = _uiState.value.copy(manifest = correctedManifest)

                    when (val waybillsResult = authRepository.getWaybillsForManifest(manifestId)) {
                        is AuthRepository.GetWaybillsResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                manifest = correctedManifest,
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
                    val newManifestId = result.manifest.id ?: ""
                    if (newManifestId.isNotBlank()) {
                        sessionManager.saveActiveManifestId(newManifestId)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        manifest = result.manifest,
                        hasActiveRun = true,
                        waybills = emptyList(),
                        deliveredWaybillNumbers = emptySet()
                    )
                    onSuccess()
                }
                is AuthRepository.CreateManifestResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun startRun(onSuccess: () -> Unit) {
        val manifestId = sessionManager.fetchActiveManifestId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.startRun(manifestId)) {
                is AuthRepository.StartRunResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    loadManifestDetails(manifestId)
                    onSuccess()
                }
                is AuthRepository.StartRunResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun markPackageAsDelivered(qrCodeContent: String) {
        if (deliveredQRCodes.contains(qrCodeContent)) {
            _uiState.value = _uiState.value.copy(isDuplicateDelivery = true, errorMessage = "Package already marked as delivered.")
            viewModelScope.launch {
                delay(1500)
                _uiState.value = _uiState.value.copy(isDuplicateDelivery = false, errorMessage = null)
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, isDuplicateDelivery = false)
            when (val result = authRepository.markPackageAsDelivered(qrCodeContent)) {
                is AuthRepository.MarkDeliveredResult.Success -> {
                    deliveredQRCodes.add(qrCodeContent)

                    // THE FIX: Update the status of the specific package within the manifest state
                    val updatedManifest = _uiState.value.manifest?.let { currentManifest ->
                        val updatedWaybills = currentManifest.waybills?.map { waybillGroup ->
                            val updatedPackages = waybillGroup.packages.map { pkg ->
                                if (pkg.qrCodeContent == qrCodeContent) {
                                    pkg.copy(status = "DELIVERED")
                                } else {
                                    pkg
                                }
                            }
                            waybillGroup.copy(packages = updatedPackages)
                        }
                        currentManifest.copy(waybills = updatedWaybills)
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        manifest = updatedManifest
                    )

                    // Update the overall progress counter only if the waybill is now complete
                    if (result.response.isWaybillComplete) {
                        _uiState.value = _uiState.value.copy(
                            deliveredWaybillNumbers = _uiState.value.deliveredWaybillNumbers + result.response.waybillNumber
                        )
                    }

                    Toast.makeText(context, "Package Marked as Delivered!", Toast.LENGTH_SHORT).show()
                }
                is AuthRepository.MarkDeliveredResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message,
                        isDuplicateDelivery = result.isDuplicate
                    )
                    if (result.isDuplicate) {
                        deliveredQRCodes.add(qrCodeContent)
                        viewModelScope.launch {
                            delay(1500)
                            _uiState.value = _uiState.value.copy(isDuplicateDelivery = false, errorMessage = null)
                        }
                    }
                }
            }
        }
    }

//    fun startScanner(onScanResult: (String) -> Unit) {
//        val scanner = GmsBarcodeScanning.getClient(context)
//        scanner.startScan()
//            .addOnSuccessListener { barcode ->
//                barcode.rawValue?.let {
//                    onScanResult(it)
//                }
//            }
//            .addOnFailureListener { e ->
//                _uiState.value = _uiState.value.copy(errorMessage = "Scan failed: ${e.message}")
//            }
//    }

    fun startScanner(onScanResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = barcodeScannerUtil.scanBarcode()
            result.onSuccess {
                onScanResult(it)
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = "Scan failed: ${it.message}")
            }
        }
    }



    fun completeRun() {
        sessionManager.clearActiveManifestId()
        _uiState.value = DriverUiState()
        Toast.makeText(context, "Run Completed!", Toast.LENGTH_LONG).show()
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun addWaybillToManifest(qrCodeContent: String) {
        val manifestId = sessionManager.fetchActiveManifestId()
        if (manifestId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "No active manifest found in session.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.addWaybillToManifest(manifestId, qrCodeContent)) {
                is AuthRepository.AddWaybillResult.Success -> {
                    loadManifestDetails(manifestId)
                    Toast.makeText(context, "Waybill Added", Toast.LENGTH_SHORT).show()
                }
                is AuthRepository.AddWaybillResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun loadPackage(qrCodeContent: String) {
        if (scannedQRCodes.contains(qrCodeContent)) {
            Toast.makeText(context, "Already Scanned", Toast.LENGTH_SHORT).show()
            return
        }

        val manifestId = sessionManager.fetchActiveManifestId()
        if (manifestId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "No active manifest UUID found in session.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.loadPackage(manifestId, qrCodeContent)) {
                is AuthRepository.LoadPackageResult.Success -> {
                    scannedQRCodes.add(qrCodeContent)
                    loadManifestDetails(manifestId)
                }
                is AuthRepository.LoadPackageResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}