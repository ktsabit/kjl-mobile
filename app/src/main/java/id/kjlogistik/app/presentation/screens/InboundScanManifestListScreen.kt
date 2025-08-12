package id.kjlogistik.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import id.kjlogistik.app.presentation.components.ManifestListItem
import id.kjlogistik.app.presentation.viewmodels.InboundScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboundScanManifestListScreen(
    navController: NavController,
    viewModel: InboundScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchArrivalManifests()
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Select Manifest for Arrival", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }
                uiState.errorMessage != null -> {
                    Text(text = uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
                uiState.manifests.isNotEmpty() -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(uiState.manifests) { manifest ->
                            ManifestListItem(manifest) {
                                navController.navigate(
                                    "inbound_scan_screen/" +
                                            "${manifest.id}/" +
                                            "${manifest.manifestNumber}/" +
                                            "${manifest.totalPackages}/" +
                                            "${manifest.scannedPackagesCount}"
                                )
                            }
                        }
                    }
                }
                else -> {
                    Text("No arrived manifests found.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}