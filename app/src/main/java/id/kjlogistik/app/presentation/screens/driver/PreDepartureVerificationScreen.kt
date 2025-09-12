package id.kjlogistik.app.presentation.screens.driver

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import id.kjlogistik.app.presentation.viewmodels.driver.DriverViewModel

// Helper function to extract the package number from the QR code
private fun getPackageNumber(qrCodeContent: String): String {
    return qrCodeContent.split(":").firstOrNull() ?: "N/A"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreDepartureVerificationScreen(
    navController: NavController,
    viewModel: DriverViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val scannedCount = uiState.manifest?.progress?.scannedCount ?: 0
    val totalCount = uiState.manifest?.progress?.totalCount ?: 0
    val allPackagesScanned = totalCount > 0 && scannedCount == totalCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pre-Departure Verification") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            viewModel.startRun {
                                navController.navigate("in_progress_run_screen") {
                                    popUpTo("driver_main_screen")
                                }
                            }
                        },
                        enabled = allPackagesScanned,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Start Run")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.startScanner { qrCode ->
                                viewModel.loadPackage(qrCode)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Package", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Package to Load")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.manifest == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column {
                    // Overall Progress Card
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "MANIFEST",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                uiState.manifest?.manifestNumber ?: uiState.manifest?.manifestId ?: "Loading...",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (totalCount > 0) scannedCount.toFloat() / totalCount.toFloat() else 0f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Overall Progress: $scannedCount of $totalCount packages scanned",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Grouped Package List
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.manifest?.waybills ?: emptyList()) { waybillGroup ->
                            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Text(
                                        text = "AWB ${waybillGroup.waybillNumber}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    waybillGroup.packages.forEach { pkg ->
                                        PackageListItem(
                                            packageNumber = getPackageNumber(pkg.qrCodeContent),
                                            isScanned = pkg.status == "LOADED"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageListItem(packageNumber: String, isScanned: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = if (isScanned) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (isScanned) "Loaded" else "Pending",
            tint = if (isScanned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Package #$packageNumber",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isScanned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}