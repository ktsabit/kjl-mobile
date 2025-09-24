package id.kjlogistik.app.presentation.screens.driver

import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonPin
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import id.kjlogistik.app.presentation.viewmodels.driver.DriverViewModel
import kotlinx.coroutines.delay

private fun getPackageNumber(qrCodeContent: String): String {
    return qrCodeContent.split(":").firstOrNull() ?: "N/A"
}

@OptIn( ExperimentalMaterial3Api::class)
@Composable
fun InProgressRunScreen(
    navController: NavController,
    viewModel: DriverViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val totalWaybills = uiState.manifest?.waybills?.size ?: 0
    val deliveredWaybillsCount = uiState.deliveredWaybillNumbers.size
    val allWaybillsDelivered = totalWaybills > 0 && deliveredWaybillsCount == totalWaybills

    val backgroundColor by animateColorAsState(
        targetValue = if (uiState.isDuplicateDelivery) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else Color.Transparent,
        label = "BackgroundColorAnimation"
    )

    LaunchedEffect(uiState.isDuplicateDelivery) {
        if (uiState.isDuplicateDelivery) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGen.startTone(ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE, 2000)
                delay(2000)
                toneGen.release()
            } catch (e: Exception) { /* Ignore */ }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            if (!uiState.isDuplicateDelivery) {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
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
            if (!allWaybillsDelivered) {
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
                .background(backgroundColor)
                .padding(paddingValues)
        ) {
            // Overall Progress Card
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "RUN PROGRESS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$deliveredWaybillsCount of $totalWaybills Waybills Delivered",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
                        CircularProgressIndicator(
                            progress = {
                                if (totalWaybills == 0) 0f
                                else deliveredWaybillsCount.toFloat() / totalWaybills.toFloat()
                            },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 4.dp
                        )
                    }
                }
            }

            if (allWaybillsDelivered) {
                // Completion Screen
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Run Complete", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Run Complete!", style = MaterialTheme.typography.headlineMedium)
                        Text("All waybills have been delivered.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            viewModel.completeRun()
                            navController.popBackStack()
                        }) {
                            Text("Complete Run & Return")
                        }
                    }
                }
            } else {
                // Grouped Package List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.manifest?.waybills ?: emptyList()) { waybillGroup ->
                        val isWaybillComplete = uiState.deliveredWaybillNumbers.contains(waybillGroup.waybillNumber)
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(
                                    text = waybillGroup.waybillNumber,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWaybillComplete) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                DetailRow(icon = Icons.Outlined.Person, label = "Sender", value = waybillGroup.senderName)
                                DetailRow(icon = Icons.Outlined.PersonPin, label = "Recipient", value = waybillGroup.recipientName)
                                DetailRow(icon = Icons.Outlined.Place, label = "Destination", value = waybillGroup.recipientCity)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                waybillGroup.packages.forEach { pkg ->
                                    PackageDeliveryItem(
                                        packageNumber = getPackageNumber(pkg.qrCodeContent),
                                        isDelivered = pkg.status == "DELIVERED"
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

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    append("$label: ")
                }
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                    append(value)
                }
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PackageDeliveryItem(packageNumber: String, isDelivered: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = if (isDelivered) Icons.Default.CheckCircle else Icons.Default.LocalShipping,
            contentDescription = if (isDelivered) "Delivered" else "Out for Delivery",
            tint = if (isDelivered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Package #$packageNumber",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDelivered) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}