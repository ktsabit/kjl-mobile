package id.kjlogistik.app // <- ENSURE THIS MATCHES YOUR ACTUAL PACKAGE NAME

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dagger.hilt.android.AndroidEntryPoint
import id.kjlogistik.app.data.session.SessionManager
import id.kjlogistik.app.presentation.screens.LoginScreen
import id.kjlogistik.app.presentation.screens.ScanScreen
import id.kjlogistik.app.presentation.theme.KJLAppTheme
import javax.inject.Inject // Import Inject

// IMPORTANT: BASE_URL is now provided via Hilt's NetworkModule. Remove it from here.
// const val BASE_URL = "https://your-api-base-url.com/"

@AndroidEntryPoint // Annotate MainActivity for Hilt
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            KJLAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
//                    LoginScreen(
//                        onLoginSuccess = { authToken ->
//                            // Use the injected sessionManager to save the token
//                            sessionManager.saveAuthToken(authToken)
//                            println("Authentication Token saved by SessionManager: $authToken")
//                            // TODO: Navigate to your main app content
//                        }
//                        // authRepository parameter is no longer needed here, Hilt provides it to ViewModel
//                    )
//                    ScanScreen()
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login_screen") {
        composable("login_screen") {
            LoginScreen(
                onLoginSuccess = { authToken ->
                    // You can pass the authToken to the next screen if needed,
                    // but generally, the SessionManager should be the source of truth.
                    navController.navigate("scan_screen") {
                        popUpTo("login_screen") { inclusive = true }
                    }
                }
            )
        }
        composable("scan_screen") {
            ScanScreen()
        }
        // Add other routes here
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    KJLAppTheme {
        // Previews with Hilt are more complex. For simplicity, we'll just show a placeholder
        // or ensure the preview doesn't rely on Hilt-injected components directly.
        // A real preview for a Hilt-enabled screen might involve a custom Hilt test rule.
        // For now, we'll just show the LoginScreen (which has its own preview setup).
        // If you need to preview MainActivity itself with Hilt, it gets more involved.
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Text("MainActivity Preview (Hilt setup not shown in preview)")
        }
    }
}
