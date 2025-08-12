package id.kjlogistik.app.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import id.kjlogistik.app.presentation.theme.KJLAppTheme
import id.kjlogistik.app.presentation.viewmodels.PickupScanViewModel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupScanScreen() {
    val scanViewModel: PickupScanViewModel = hiltViewModel()
    val uiState by scanViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.scanSuccessMessage) {
        uiState.scanSuccessMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

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
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Pickup Scan",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = { scanViewModel.startQrCodeScan(isDamaged = false) },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Scan Barcode")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Raw Value:", style = MaterialTheme.typography.titleSmall)
            Text(uiState.scannedRawValue ?: "N/A", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState.isScanSuccessful) {
                true -> {
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
                    Text(
                        uiState.errorMessage ?: "Scan a barcode first.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}