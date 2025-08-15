package id.kjlogistik.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import id.kjlogistik.app.presentation.components.ManifestListItem
import id.kjlogistik.app.presentation.viewmodels.OutboundScanViewModel // Change to InboundScanViewModel for the other screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboundScanManifestListScreen( // Change function name for the other screen
    navController: NavController,
    viewModel: OutboundScanViewModel = hiltViewModel() // Change ViewModel for the other screen
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchDepartureManifests() // Change function call for the other screen
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Departure Manifest") }, // Change title for the other screen
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(text = uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
                    }
                }
                uiState.manifests.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp), // <-- FIXES THE SHADOW ISSUE
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.manifests) { manifest ->
                            ManifestListItem(manifest) {
                                // Your navigation logic remains the same
                                navController.navigate(
                                    "outbound_scan_screen/" + // Change route for the other screen
                                            "${manifest.id}/" +
                                            "${manifest.manifestNumber}/" +
                                            "${manifest.totalPackages}/" +
                                            "${manifest.scannedPackagesCount}" // Use correct count
                                )
                            }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No pending manifests found.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}