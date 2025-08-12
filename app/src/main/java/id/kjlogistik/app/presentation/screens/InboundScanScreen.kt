package id.kjlogistik.app.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import id.kjlogistik.app.presentation.viewmodels.InboundScanPackageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboundScanScreen(
    navController: NavController,
    manifestId: String,
    manifestNumber: String,
    totalPackages: Int,
    scannedPackagesCount: Int,
    viewModel: InboundScanPackageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.setManifestDetails(manifestId, manifestNumber, totalPackages, scannedPackagesCount)
    }

    LaunchedEffect(uiState.scanSuccessMessage) {
        uiState.scanSuccessMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.finalAlertMessage) {
        uiState.finalAlertMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inbound Scan") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Manifest: ${uiState.manifestNumber}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Packages Scanned: ${uiState.scannedPackagesCount} / ${uiState.totalPackages}",
                style = MaterialTheme.typography.titleLarge,
                color = if (uiState.scannedPackagesCount == uiState.totalPackages) Color.Green else Color.Unspecified
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val scanner = GmsBarcodeScanning.getClient(context)
                    val barcodeFuture = scanner.startScan()
                    barcodeFuture.addOnSuccessListener { barcode ->
                        barcode.rawValue?.let { qrCodeContent ->
                            viewModel.scanPackage(qrCodeContent)
                        }
                    }
                    barcodeFuture.addOnFailureListener { e ->
                        viewModel.handleScanFailure("Scan failed: ${e.message}")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Text("Start Scanning")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isScanning) {
                CircularProgressIndicator()
            }
        }
    }
}