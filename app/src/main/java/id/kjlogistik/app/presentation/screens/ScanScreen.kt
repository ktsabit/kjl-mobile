package id.kjlogistik.app.presentation.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle // For success icon
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.kjlogistik.app.presentation.theme.KJLAppTheme
import androidx.hilt.navigation.compose.hiltViewModel
import id.kjlogistik.app.presentation.viewmodels.ScanViewModel
// Imports for Preview purposes only
import id.kjlogistik.app.data.repository.AuthRepository
import id.kjlogistik.app.data.api.AuthApiService
import id.kjlogistik.app.data.model.LoginRequest
import id.kjlogistik.app.data.model.LoginResponse
import id.kjlogistik.app.data.model.InboundScanRequest
import id.kjlogistik.app.data.model.InboundScanResponse
import id.kjlogistik.app.data.model.UserMeResponse
import id.kjlogistik.app.data.session.SessionManager
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen() {
    // Get ViewModel instance provided by Hilt
    val scanViewModel: ScanViewModel = hiltViewModel()
    // Collect UI state changes from the ViewModel
    val uiState by scanViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Observe scan success message for Toast display
    LaunchedEffect(uiState.scanSuccessMessage) {
        uiState.scanSuccessMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Observe error message for Toast display
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .imePadding(), // Adjusts padding for software keyboard
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Button to trigger QR code scan
            Button(
                onClick = { scanViewModel.startQrCodeScan(isDamaged = false) }, // Defaulting isDamaged to false
                enabled = !uiState.isLoading, // Disable button while scanning or processing
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Scan Barcode")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Display raw scanned value (useful for debugging and user feedback)
            Text("Raw Value:", style = MaterialTheme.typography.titleSmall)
            Text(uiState.scannedRawValue ?: "N/A", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(16.dp))

            // Display scan result status and message based on `isScanSuccessful` state
            // This block will show the success/failure icons and text for the duration
            // set by `delayResetState` in the ViewModel.
            when (uiState.isScanSuccessful) {
                true -> {
                    // Success state: Green checkmark and success message
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Scan Success",
                        tint = Color.Green,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        uiState.scanSuccessMessage ?: "Scan OK.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Green
                    )
                }
                false -> {
                    // Failure state: Red cross and error message
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Scan Failed",
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        uiState.errorMessage ?: "Scan failed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red
                    )
                }
                null -> {
                    // Initial or reset state: Prompt user to scan
                    // This is displayed when `isScanSuccessful` is null (initial or after reset)
                    Text(
                        uiState.errorMessage ?: "Scan a barcode first.", // Display error if present, otherwise default
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// Preview Composable for ScanScreen
@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun ScanScreenPreview() {
    KJLAppTheme {
        // Dummy implementations for Preview
        val dummyAuthApiService = object : AuthApiService {
            override suspend fun login(request: LoginRequest): Response<LoginResponse> {
                return Response.success(LoginResponse(refreshToken = "dummy_refresh", accessToken = "dummy_access"))
            }

            // Dummy implementation for getUserMe for preview
            override suspend fun getUserMe(authToken: String): Response<UserMeResponse> {
                return Response.success(
                    UserMeResponse(
                        id = "preview_user_id",
                        email = "preview@example.com",
                        username = "preview_housekeeper",
                        fullName = "Preview User",
                        client = null,
                        hub = "preview_hub_id",
                        hubName = "Preview Hub",
                        groups = listOf("Housekeeper")
                    )
                )
            }

            // Dummy implementation for inboundScanPackage for preview
            override suspend fun inboundScanPackage(
                authToken: String,
                request: InboundScanRequest
            ): Response<InboundScanResponse> {
                return Response.success(InboundScanResponse("Package ${request.qrCodeContent} verified (Preview)", "success"))
            }
        }
        val dummySessionManager = SessionManager(LocalContext.current)
        val dummyAuthRepository = AuthRepository(dummyAuthApiService, dummySessionManager)

        // Store a dummy token for preview if needed (e.g., if you have logic depending on token presence)
        LaunchedEffect(Unit) {
            dummySessionManager.saveAuthToken("preview_token_123")
        }

        // Pass dummy dependencies to the ViewModel for the Preview
        val previewScanViewModel = ScanViewModel(dummyAuthRepository, dummySessionManager, LocalContext.current)
        ScanScreen()
    }
}
