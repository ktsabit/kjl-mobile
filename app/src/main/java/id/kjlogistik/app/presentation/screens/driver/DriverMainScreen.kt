package id.kjlogistik.app.presentation.screens.driver

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import id.kjlogistik.app.data.session.SessionManager
import id.kjlogistik.app.presentation.viewmodels.auth.LoginViewModel
import id.kjlogistik.app.presentation.viewmodels.driver.DriverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverMainScreen(
    navController: NavController,
    sessionManager: SessionManager = hiltViewModel<LoginViewModel>().sessionManager,
    driverViewModel: DriverViewModel = hiltViewModel()
) {
    val uiState by driverViewModel.uiState.collectAsState()

    // This recomposes the screen whenever you navigate back to it,
    // ensuring the check for an active run is always fresh.
    LaunchedEffect(navController.currentBackStackEntry) {
        driverViewModel.checkForActiveRun()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver Dashboard") },
                actions = {
                    IconButton(onClick = {
                        sessionManager.clearAuthToken()
                        navController.navigate("login_screen") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // AnimatedContent provides a smooth transition when the state changes
            AnimatedContent(targetState = uiState.hasActiveRun, label = "RunStateAnimation") { hasActiveRun ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    if (hasActiveRun) {
                        // FIX: Pass nullable status safely
                        ContinueRunCard(navController, uiState.manifest?.status, uiState.isLoading)
                    } else {
                        StartNewRunCard(navController, driverViewModel, uiState.isLoading)
                    }
                }
            }
        }
    }
}

@Composable
private fun StartNewRunCard(navController: NavController, viewModel: DriverViewModel, isLoading: Boolean) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocalShipping,
            contentDescription = "Delivery Run",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ready for your next run?",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create a new delivery manifest and scan your packages to begin.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                // This now correctly only creates a manifest when there is no active one.
                viewModel.createManifest {
                    navController.navigate("pre_departure_verification_screen")
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Start New Delivery Run")
            }
        }
    }
}

@Composable
private fun ContinueRunCard(navController: NavController, status: String?, isLoading: Boolean) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocalShipping,
            contentDescription = "Delivery Run",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "You have an active run!",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Continue scanning or delivering packages for your current manifest.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                // FIX: Safely determine the destination based on status
                val destination = when (status) {
                    "DRAFT" -> "pre_departure_verification_screen"
                    "OUT_FOR_DELIVERY" -> "in_progress_run_screen"
                    else -> "driver_main_screen" // Fallback
                }
                navController.navigate(destination)
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Continue Delivery Run")
            }
        }
    }
}