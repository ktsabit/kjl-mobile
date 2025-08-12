// file: app/src/main/java/id/kjlogistik/app/presentation/screens/DriverMainScreen.kt

package id.kjlogistik.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import id.kjlogistik.app.data.session.SessionManager
import id.kjlogistik.app.presentation.theme.KJLAppTheme
import id.kjlogistik.app.presentation.viewmodels.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun DriverMainScreen(
    navController: NavController,
    sessionManager: SessionManager = hiltViewModel<LoginViewModel>().sessionManager
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KJL App", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = {
                        // Clear the token and navigate back to the login screen
                        sessionManager.clearAuthToken()
                        navController.navigate("login_screen") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp, // You will need to import this
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        }

    ) {
        paddingValues -> Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Welcome, Driver!")
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun DriverMainScreenPreview() {
//    KJLAppTheme {
//        DriverMainScreen()
//    }
//}