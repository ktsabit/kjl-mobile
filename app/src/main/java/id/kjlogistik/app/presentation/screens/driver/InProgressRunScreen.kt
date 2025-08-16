package id.kjlogistik.app.presentation.screens.driver

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import id.kjlogistik.app.presentation.viewmodels.driver.DriverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InProgressRunScreen(
    navController: NavController,
    viewModel: DriverViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val allPackagesDelivered = uiState.waybills.isNotEmpty() && uiState.deliveredWaybillIds.size == uiState.waybills.size

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delivery Run") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!allPackagesDelivered) {
                ExtendedFloatingActionButton(
                    text = { Text("Scan Delivered Package") },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Package") },
                    onClick = {
                        viewModel.startScanner { qrCode ->
                            viewModel.markPackageAsDelivered(qrCode)
                        }
                    }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "RUN PROGRESS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${uiState.deliveredWaybillIds.size} of ${uiState.waybills.size} Delivered",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
                        CircularProgressIndicator(
                            progress = {
                                if (uiState.waybills.isEmpty()) 0f
                                else uiState.deliveredWaybillIds.size.toFloat() / uiState.waybills.size.toFloat()
                            },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = "${((if (uiState.waybills.isEmpty()) 0.0 else uiState.deliveredWaybillIds.size.toDouble() / uiState.waybills.size.toDouble()) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (allPackagesDelivered) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Run Complete", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Run Complete!", style = MaterialTheme.typography.headlineMedium)
                        Text("All packages have been marked as delivered.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            // --- THIS IS THE FIX ---
                            viewModel.completeRun()
                            navController.popBackStack()
                        }) {
                            Text("Complete Run & Return")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.waybills) { waybill ->
                        val isDelivered = uiState.deliveredWaybillIds.contains(waybill.id)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            ListItem(
                                colors = ListItemDefaults.colors(
                                    containerColor = if(isDelivered) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                headlineContent = {
                                    Text(waybill.waybillNumber ?: "Unknown Waybill", fontWeight = FontWeight.SemiBold)
                                },
                                supportingContent = { Text("Packages: ${waybill.packagesQty}") },
                                trailingContent = {
                                    if (isDelivered) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Delivered", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}