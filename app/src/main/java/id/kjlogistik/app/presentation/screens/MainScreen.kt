package id.kjlogistik.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import id.kjlogistik.app.presentation.theme.KJLAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KJL App", style = MaterialTheme.typography.titleLarge) }
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
//            Text(
//                text = "Select a Scan Type",
//                style = MaterialTheme.typography.headlineLarge,
//                color = MaterialTheme.colorScheme.primary,
//                modifier = Modifier.padding(bottom = 32.dp)
//            )
            ScanMenuButton(text = "Pickup Scan") {
                navController.navigate("pickup_scan_screen")
            }
            Spacer(modifier = Modifier.height(16.dp))
            ScanMenuButton(text = "Outbound Scan (Departure)") {
                navController.navigate("outbound_scan_manifest_list_screen")
            }
            Spacer(modifier = Modifier.height(16.dp))
            ScanMenuButton(text = "Inbound Scan (Arrival)") {
                navController.navigate("inbound_scan_manifest_list_screen")
            }
        }
    }
}

@Composable
fun ScanMenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge)
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    KJLAppTheme {
        MainScreen(rememberNavController())
    }
}