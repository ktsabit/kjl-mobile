package id.kjlogistik.app.presentation.screens.warehouse

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import id.kjlogistik.app.presentation.viewmodels.warehouse.PickupScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupScanScreen(navController: NavController) { // Added NavController
    val scanViewModel: PickupScanViewModel = hiltViewModel()
    val uiState by scanViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // This makes sure toasts don't linger or fire unnecessarily
    LaunchedEffect(uiState.scanSuccessMessage, uiState.errorMessage) {
        uiState.scanSuccessMessage?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        uiState.errorMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pickup Scan") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Navigation back
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { scanViewModel.startQrCodeScan(isDamaged = false) },
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp))
                    Text("Scanning...", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
                }
                uiState.isScanSuccessful == true -> {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF2E7D32), modifier = Modifier.size(80.dp))
                    Text("Success", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2E7D32), modifier = Modifier.padding(top = 16.dp))
                    Text(uiState.scannedRawValue ?: "", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                }
                uiState.isScanSuccessful == false -> {
                    Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(80.dp))
                    Text("Scan Failed", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
                    Text(uiState.errorMessage ?: "An unknown error occurred.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                }
                else -> {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Ready to scan", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(80.dp))
                    Text("Ready to Scan", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
                    Text("Press the button below to start scanning.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}