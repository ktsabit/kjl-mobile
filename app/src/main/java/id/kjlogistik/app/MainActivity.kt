package id.kjlogistik.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import id.kjlogistik.app.data.session.SessionManager
import id.kjlogistik.app.presentation.screens.auth.LoginScreen
import id.kjlogistik.app.presentation.screens.auth.ProfileScreen
import id.kjlogistik.app.presentation.screens.driver.DriverMainScreen
import id.kjlogistik.app.presentation.screens.driver.InProgressRunScreen
import id.kjlogistik.app.presentation.screens.driver.PreDepartureVerificationScreen
import id.kjlogistik.app.presentation.screens.driver.ScanToLoadScreen
import id.kjlogistik.app.presentation.screens.warehouse.InboundScanManifestListScreen
import id.kjlogistik.app.presentation.screens.warehouse.InboundScanScreen
import id.kjlogistik.app.presentation.screens.warehouse.MainScreen
import id.kjlogistik.app.presentation.screens.warehouse.OutboundScanManifestListScreen
import id.kjlogistik.app.presentation.screens.warehouse.OutboundScanScreen
import id.kjlogistik.app.presentation.screens.warehouse.PickupScanScreen
import id.kjlogistik.app.presentation.theme.KJLAppTheme
import id.kjlogistik.app.presentation.viewmodels.AppUpdateViewModel
import id.kjlogistik.app.presentation.viewmodels.auth.LoginViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    // --- ADD THIS PERMISSION LAUNCHER ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // You can handle the case where the user denies the permission,
        // but for Chucker it will just mean no notifications will show.
    }

    private fun askNotificationPermission() {
        // This is only required for API level 33+ (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // --- END OF ADDITION ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // --- CALL THE PERMISSION REQUEST HERE ---
        askNotificationPermission()
        // --- END OF CALL ---

        setContent {
            KJLAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val updateViewModel: AppUpdateViewModel = hiltViewModel()
    val updateState by updateViewModel.updateState.collectAsState()
    val uriHandler = LocalUriHandler.current
    val sessionManager: SessionManager = hiltViewModel<LoginViewModel>().sessionManager

    val authToken = sessionManager.fetchAuthToken()
    val userGroups = sessionManager.fetchUserGroups()


    val startDestination = if (authToken != null && userGroups.isNotEmpty()) {
        if (userGroups.contains("Driver")) {
            "driver_main_screen"
        } else {
            "main_screen"
        }
    } else {
        "login_screen"
    }

    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdates()
    }

    if (updateState.showUpdateDialog) {
        UpdateDialog(
            isForceUpdate = updateState.isForceUpdate,
            onUpdateClick = {
                updateState.updateUrl?.let { uriHandler.openUri(it) }
            },
            onDismiss = {
                if (!updateState.isForceUpdate) {
                    updateViewModel.dismissUpdateDialog()
                }
            }
        )
    }


    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
//        composable("login_screen") {
//            LoginScreen(
//                onLoginSuccess = {
//                    navController.navigate("main_screen") {
//                        popUpTo("login_screen") { inclusive = true }
//                    }
//                }
//            )
//        }
        composable("login_screen") {
            LoginScreen(
                onLoginSuccess = { userGroups ->
                    val destination = if (userGroups.contains("Driver")) {
                        "driver_main_screen"
                    } else {
                        "main_screen"
                    }
                    navController.navigate(destination) {
                        popUpTo("login_screen") { inclusive = true }
                    }
                }
            )
        }

//        composable("main_screen") {
//            MainScreen(navController = navController)
//        }

        composable("scan_to_load_screen") {
            ScanToLoadScreen(navController)
        }
        composable("in_progress_run_screen") {
            InProgressRunScreen(navController)
        }


        composable("main_screen") {
            MainScreen(navController = navController, sessionManager = sessionManager)
        }
        composable("driver_main_screen") {
            DriverMainScreen(navController = navController, sessionManager = sessionManager)
        }
        composable("pickup_scan_screen") {
            PickupScanScreen(navController = navController)
        }
        composable("outbound_scan_manifest_list_screen") {
            OutboundScanManifestListScreen(navController)
        }
        composable(
            "outbound_scan_screen/{manifestId}/{manifestNumber}/{totalPackages}/{scannedPackagesCount}",
            arguments = listOf(
                navArgument("manifestId") { type = NavType.StringType },
                navArgument("manifestNumber") { type = NavType.StringType },
                navArgument("totalPackages") { type = NavType.IntType },
                navArgument("scannedPackagesCount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            OutboundScanScreen(
                navController = navController,
                manifestId = backStackEntry.arguments?.getString("manifestId")!!,
                manifestNumber = backStackEntry.arguments?.getString("manifestNumber")!!,
                totalPackages = backStackEntry.arguments?.getInt("totalPackages")!!,
                scannedPackagesCount = backStackEntry.arguments?.getInt("scannedPackagesCount")!!
            )
        }
        composable("inbound_scan_manifest_list_screen") {
            InboundScanManifestListScreen(navController)
        }
        composable(
            "inbound_scan_screen/{manifestId}/{manifestNumber}/{totalPackages}/{scannedPackagesCount}",
            arguments = listOf(
                navArgument("manifestId") { type = NavType.StringType },
                navArgument("manifestNumber") { type = NavType.StringType },
                navArgument("totalPackages") { type = NavType.IntType },
                navArgument("scannedPackagesCount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            InboundScanScreen(
                navController = navController,
                manifestId = backStackEntry.arguments?.getString("manifestId")!!,
                manifestNumber = backStackEntry.arguments?.getString("manifestNumber")!!,
                totalPackages = backStackEntry.arguments?.getInt("totalPackages")!!,
                scannedPackagesCount = backStackEntry.arguments?.getInt("arrivalScannedCount")!!
            )
        }
        composable("pre_departure_verification_screen") {
            PreDepartureVerificationScreen(navController = navController)
        }
        composable("profile_screen") {
            ProfileScreen(navController)
        }

    }
}

@Composable
fun UpdateDialog(
    isForceUpdate: Boolean,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Update Available") },
        text = { Text("A new version of the app is available. Please update to continue.") },
        confirmButton = {
            Button(onClick = onUpdateClick) {
                Text("Update Now")
            }
        },
        dismissButton = {
            if (!isForceUpdate) {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    )
}