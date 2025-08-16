package id.kjlogistik.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import id.kjlogistik.app.data.session.SessionManager
import id.kjlogistik.app.presentation.screens.auth.LoginScreen
import id.kjlogistik.app.presentation.screens.driver.DriverMainScreen
import id.kjlogistik.app.presentation.screens.driver.InProgressRunScreen
import id.kjlogistik.app.presentation.screens.driver.ScanToLoadScreen
import id.kjlogistik.app.presentation.screens.warehouse.InboundScanManifestListScreen
import id.kjlogistik.app.presentation.screens.warehouse.InboundScanScreen
import id.kjlogistik.app.presentation.screens.warehouse.MainScreen
import id.kjlogistik.app.presentation.screens.warehouse.OutboundScanManifestListScreen
import id.kjlogistik.app.presentation.screens.warehouse.OutboundScanScreen
import id.kjlogistik.app.presentation.screens.warehouse.PickupScanScreen
import id.kjlogistik.app.presentation.theme.KJLAppTheme
import id.kjlogistik.app.presentation.viewmodels.auth.LoginViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    // Retrieve the sessionManager instance that was injected into MainActivity
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
    }
}