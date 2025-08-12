package id.kjlogistik.app.presentation.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.kjlogistik.app.data.api.AuthApiService
import id.kjlogistik.app.data.model.ArrivalScanRequest
import id.kjlogistik.app.data.model.ArrivalScanResponse
import id.kjlogistik.app.data.model.DepartureScanRequest
import id.kjlogistik.app.data.model.DepartureScanResponse
import id.kjlogistik.app.data.model.InboundScanRequest
import id.kjlogistik.app.data.model.InboundScanResponse
import id.kjlogistik.app.data.model.LoginRequest
import id.kjlogistik.app.data.model.LoginResponse
import id.kjlogistik.app.data.model.Manifest
import id.kjlogistik.app.data.model.UserMeResponse
import id.kjlogistik.app.data.repository.AuthRepository
import id.kjlogistik.app.data.session.SessionManager
import id.kjlogistik.app.presentation.theme.KJLAppTheme
import id.kjlogistik.app.presentation.viewmodels.LoginViewModel
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (userGroups: List<String>) -> Unit,
    ) {
    val loginViewModel: LoginViewModel = hiltViewModel()
    val uiState by loginViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            uiState.authToken?.let {
                Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                onLoginSuccess(uiState.userGroups)
                loginViewModel.resetState()
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome Back!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = uiState.usernameInput,
                onValueChange = loginViewModel::onUsernameChange,
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Username Icon") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.passwordInput,
                onValueChange = loginViewModel::onPasswordChange,
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = loginViewModel::login,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Login", style = MaterialTheme.typography.titleMedium)
                }
            }

            TextButton(
                onClick = { /* Handle navigation to Forgot Password */ },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Forgot Password?")
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    KJLAppTheme {
        val dummyAuthRepository = AuthRepository(object : AuthApiService {
            override suspend fun login(request: LoginRequest): Response<LoginResponse> {
                return Response.success(LoginResponse(refreshToken = "dummy_refresh", accessToken = "dummy_access"))
            }

            override suspend fun getUserMe(authToken: String): Response<UserMeResponse> {
                return Response.success(UserMeResponse(id = "preview_id", email = "preview@example.com", username = "preview_housekeeper", fullName = "Preview User", client = null, hub = "preview_hub_id", hubName = "Preview Hub", groups = listOf("Housekeeper")))
            }

            override suspend fun pickupScanPackage(authToken: String, request: InboundScanRequest): Response<InboundScanResponse> {
                return Response.success(InboundScanResponse("Preview Scan OK", "success"))
            }

            override suspend fun getManifestsForDeparture(authToken: String, status: String): Response<List<Manifest>> {
                return Response.success(emptyList())
            }

            override suspend fun departureScanPackage(authToken: String, manifestId: String, request: DepartureScanRequest): Response<DepartureScanResponse> {
                return Response.success(DepartureScanResponse("Preview Scan OK"))
            }

            override suspend fun getManifestsForArrival(authToken: String, status: String): Response<List<Manifest>> {
                return Response.success(emptyList())
            }

            override suspend fun arrivalScanPackage(authToken: String, manifestId: String, request: ArrivalScanRequest): Response<ArrivalScanResponse> {
                return Response.success(ArrivalScanResponse("Preview Scan OK"))
            }
        }, SessionManager(LocalContext.current))
        LoginScreen(onLoginSuccess = { /* Do nothing in preview */ })
    }
}