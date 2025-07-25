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
// Removed unused imports from here:
// import id.kjlogistik.app.data.api.AuthApiService
// import id.kjlogistik.app.data.model.LoginRequest
// import id.kjlogistik.app.data.model.LoginResponse
// import id.kjlogistik.app.data.repository.AuthRepository
import id.kjlogistik.app.presentation.theme.KJLAppTheme // Assuming you have a theme defined
import androidx.hilt.navigation.compose.hiltViewModel // Import hiltViewModel
import id.kjlogistik.app.data.api.AuthApiService
import id.kjlogistik.app.data.model.LoginRequest
import id.kjlogistik.app.data.model.LoginResponse
import id.kjlogistik.app.data.model.ScanRequest
import id.kjlogistik.app.data.model.ScanResponse
import id.kjlogistik.app.data.repository.AuthRepository
import id.kjlogistik.app.data.session.SessionManager
// Removed androidx.lifecycle.viewmodel.compose.viewModel, as hiltViewModel is used
import id.kjlogistik.app.presentation.viewmodels.LoginViewModel
import retrofit2.Response

// Removed retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit, // Callback for successful login
) {
    // Get ViewModel instance using hiltViewModel()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val uiState by loginViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Observe login success
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            uiState.authToken?.let { token ->
                Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show() // Keep for immediate user feedback
                onLoginSuccess(token)
                loginViewModel.resetState() // Reset state after navigation, if appropriate (e.g., to clear password field)
            }
        }
    }

    // Observe error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            // No need to reset state here, ViewModel handles it after an error, or just show the error.
            // If you want to clear the error message immediately after showing, add a function in ViewModel
            // loginViewModel.clearErrorMessage() // Example if you add this
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
        // Your current preview setup is fine for simple previews, no changes needed for this part.
        // It's good that you acknowledge it's a dummy for preview.
        val dummyAuthRepository = AuthRepository(object : AuthApiService {
            override suspend fun login(request: LoginRequest): Response<LoginResponse> {
                return Response.success(
                    LoginResponse(
                        "dummy_token",
                        "123",
                        "Preview Login Success"
                    )
                )
            }

            // NEW: Dummy implementation for scanQrCode for preview
            override suspend fun scanQrCode(
                authToken: String,
                request: ScanRequest
            ): Response<ScanResponse> {
                return Response.success(ScanResponse("Preview Scan OK", "success"))
            }
        })
        // Mock SessionManager for preview
        val dummySessionManager = SessionManager(LocalContext.current)
        val previewLoginViewModel = LoginViewModel(dummyAuthRepository, dummySessionManager) // Pass dummy SessionManager
        LoginScreen(onLoginSuccess = { /* Do nothing in preview */ })
    }
}