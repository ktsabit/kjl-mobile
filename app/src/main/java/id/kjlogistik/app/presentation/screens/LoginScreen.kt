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
import id.kjlogistik.app.data.api.AuthApiService
import id.kjlogistik.app.data.model.LoginRequest
import id.kjlogistik.app.data.model.LoginResponse
// CORRECTED IMPORTS: Use InboundScanRequest and InboundScanResponse
import id.kjlogistik.app.data.model.InboundScanRequest
import id.kjlogistik.app.data.model.InboundScanResponse
import id.kjlogistik.app.data.model.UserMeResponse
import id.kjlogistik.app.data.repository.AuthRepository
import id.kjlogistik.app.data.session.SessionManager
import id.kjlogistik.app.presentation.theme.KJLAppTheme
import androidx.hilt.navigation.compose.hiltViewModel
import id.kjlogistik.app.presentation.viewmodels.LoginViewModel
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit, // Callback for successful login
) {
    val loginViewModel: LoginViewModel = hiltViewModel()
    val uiState by loginViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Observe login success
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            uiState.authToken?.let { token ->
                Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                onLoginSuccess(token)
                loginViewModel.resetState()
            }
        }
    }

    // Observe error messages
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
                // Simulate a successful login with dummy tokens
                return Response.success(
                    LoginResponse(
                        refreshToken = "dummy_refresh_token_preview",
                        accessToken = "dummy_access_token_preview"
                    )
                )
            }

            // Dummy implementation for getUserMe for preview
            override suspend fun getUserMe(authToken: String): Response<UserMeResponse> {
                // Simulate a successful getUserMe response with "Housekeeper" group
                return Response.success(
                    UserMeResponse(
                        id = "preview_user_id",
                        email = "preview@example.com",
                        username = "preview_housekeeper",
                        fullName = "Preview User",
                        client = null,
                        hub = "preview_hub_id",
                        hubName = "Preview Hub",
                        groups = listOf("Housekeeper") // Crucial for previewing successful login
                    )
                )
            }

            // CORRECTED: Implement inboundScanPackage instead of the old scanQrCode
            override suspend fun inboundScanPackage(
                authToken: String,
                request: InboundScanRequest // Use InboundScanRequest
            ): Response<InboundScanResponse> { // Use InboundScanResponse
                return Response.success(InboundScanResponse("Preview Scan OK", "success"))
            }
        }, SessionManager(LocalContext.current)) // Pass a dummy SessionManager to AuthRepository

        // Mock SessionManager for preview
        val dummySessionManager = SessionManager(LocalContext.current)
        val previewLoginViewModel = LoginViewModel(dummyAuthRepository, dummySessionManager)
        LoginScreen(onLoginSuccess = { /* Do nothing in preview */ })
    }
}
