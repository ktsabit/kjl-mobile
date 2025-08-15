package id.kjlogistik.app.presentation.screens.warehouse

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import id.kjlogistik.app.presentation.viewmodels.warehouse.InboundScanPackageViewModel // Change ViewModel for the other screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboundScanScreen( // Change function name for the other screen
    navController: NavController,
    manifestId: String,
    manifestNumber: String,
    totalPackages: Int,
    scannedPackagesCount: Int,
    viewModel: InboundScanPackageViewModel = hiltViewModel() // Change ViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.setManifestDetails(manifestId, manifestNumber, totalPackages, scannedPackagesCount)
    }

    LaunchedEffect(uiState.scanSuccessMessage, uiState.errorMessage, uiState.finalAlertMessage) {
        val message = uiState.scanSuccessMessage ?: uiState.errorMessage ?: uiState.finalAlertMessage
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages() // Clear message after showing
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (uiState.totalPackages > 0) uiState.scannedPackagesCount.toFloat() / uiState.totalPackages.toFloat() else 0f,
        label = "ScanProgressAnimation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arrival Scan") }, // Change title
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            val isComplete = uiState.scannedPackagesCount >= uiState.totalPackages && uiState.totalPackages > 0
            ExtendedFloatingActionButton(
                onClick = {
                    if (isComplete) {
                        Toast.makeText(context, "Manifest is complete.", Toast.LENGTH_SHORT).show()
                        return@ExtendedFloatingActionButton
                    }
                    val scanner = GmsBarcodeScanning.getClient(context)
                    scanner.startScan()
                        .addOnSuccessListener { barcode -> barcode.rawValue?.let(viewModel::scanPackage) }
                        .addOnFailureListener { e -> viewModel.handleScanFailure("Scan failed: ${e.message}") }
                },
                text = { Text(if (isComplete) "Complete" else "Scan Package") },
                icon = { Icon(if (isComplete) Icons.Default.Check else Icons.Default.QrCodeScanner, contentDescription = null) },
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = uiState.manifestNumber,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 16.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${uiState.scannedPackagesCount}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "of ${uiState.totalPackages}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (uiState.isScanning) {
                Spacer(modifier = Modifier.height(32.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.6f))
            }
        }
    }
}