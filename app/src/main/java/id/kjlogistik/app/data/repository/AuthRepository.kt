// app/data/repository/AuthRepository.kt
package id.kjlogistik.app.data.repository

import id.kjlogistik.app.data.api.AuthApiService
import id.kjlogistik.app.data.model.LoginRequest
import id.kjlogistik.app.data.model.LoginResponse
import id.kjlogistik.app.data.model.InboundScanRequest // NEW: Import new scan request model
import id.kjlogistik.app.data.model.InboundScanResponse // NEW: Import new scan response model
import id.kjlogistik.app.data.session.SessionManager
import retrofit2.HttpException
import javax.inject.Inject
import java.io.IOException

class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val sessionManager: SessionManager
) {

    // Sealed class to represent the outcome of a login attempt
    sealed class LoginResult {
        data class Success(val message: String) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }

    // Sealed class to represent the outcome of a scan attempt
    sealed class ScanResult { // NEW: Sealed class for scan results
        data class Success(val message: String) : ScanResult()
        data class Error(val message: String) : ScanResult()
    }

    /**
     * Handles the user login process, including token retrieval and group validation.
     * @param request The LoginRequest containing username and password.
     * @return A LoginResult indicating success or failure with a message.
     */
    suspend fun login(request: LoginRequest): LoginResult {
        try {
            // Step 1: Attempt to get JWT tokens (access and refresh)
            val loginResponse = authApiService.login(request)

            if (loginResponse.isSuccessful) {
                val loginBody = loginResponse.body()
                val accessToken = loginBody?.accessToken

                if (accessToken.isNullOrEmpty()) {
                    return LoginResult.Error("Login successful, but no access token received.")
                }

                // Step 2: Use the access token to fetch user details
                // The Authorization header expects "Bearer <token>"
                val userMeResponse = authApiService.getUserMe("Bearer $accessToken")

                if (userMeResponse.isSuccessful) {
                    val userMeBody = userMeResponse.body()
                    val userGroups = userMeBody?.groups

                    // Step 3: Validate if the user belongs to the "Housekeeper" group
                    if (userGroups != null && userGroups.contains("Housekeeper")) {
                        // If valid, save the access token
                        sessionManager.saveAuthToken(accessToken)
                        return LoginResult.Success("Login successful. Welcome, Housekeeper!")
                    } else {
                        // If not a Housekeeper, clear any potential token and return error
                        sessionManager.clearAuthToken()
                        return LoginResult.Error("Access denied: User is not a Housekeeper.")
                    }
                } else {
                    // Handle error from /auth/users/me endpoint
                    val errorBody = userMeResponse.errorBody()?.string()
                    return LoginResult.Error("Failed to fetch user details: ${errorBody ?: userMeResponse.message()}")
                }
            } else {
                // Handle error from /auth/jwt/create endpoint
                val errorBody = loginResponse.errorBody()?.string()
                return LoginResult.Error("Login failed: ${errorBody ?: loginResponse.message()}")
            }
        } catch (e: HttpException) {
            // Handle HTTP errors (e.g., 401 Unauthorized, 403 Forbidden)
            return LoginResult.Error("Network error: ${e.code()} - ${e.message()}")
        } catch (e: IOException) {
            // Handle network connectivity issues (e.g., no internet)
            return LoginResult.Error("Connectivity error: ${e.message}")
        } catch (e: Exception) {
            // Catch any other unexpected errors
            return LoginResult.Error("An unexpected error occurred: ${e.message}")
        }
    }

    /**
     * Performs an inbound package scan operation.
     * @param qrCodeContent The content of the scanned QR code.
     * @param isDamaged Boolean indicating if the package is damaged.
     * @param locationHubId The ID of the hub where the scan occurs.
     * @return A ScanResult indicating success or failure with a message.
     */
    suspend fun inboundScanPackage(
        qrCodeContent: String,
        isDamaged: Boolean,
        locationHubId: String
    ): ScanResult {
        val authToken = sessionManager.fetchAuthToken()
        if (authToken.isNullOrEmpty()) {
            return ScanResult.Error("Authentication token not found. Please log in.")
        }

        try {
            val request = InboundScanRequest(
                qrCodeContent = qrCodeContent,
                isDamaged = isDamaged,
                locationHubId = locationHubId
            )
            val response = authApiService.inboundScanPackage("Bearer $authToken", request)

            if (response.isSuccessful) {
                val responseBody = response.body()
                return ScanResult.Success(responseBody?.message ?: "Scan successful.")
            } else {
                val errorBody = response.errorBody()?.string()
                return ScanResult.Error("Scan failed: ${errorBody ?: response.message()}")
            }
        } catch (e: HttpException) {
            return ScanResult.Error("Network error during scan: ${e.code()} - ${e.message()}")
        } catch (e: IOException) {
            return ScanResult.Error("Connectivity error during scan: ${e.message}")
        } catch (e: Exception) {
            return ScanResult.Error("An unexpected error occurred during scan: ${e.message}")
        }
    }

    // You can add other authentication-related methods here, like logout
    fun logout() {
        sessionManager.clearAuthToken()
        // In a real app, you might also want to invalidate the refresh token on the backend
    }

    // Method to check if a user is currently logged in (has an access token)
    fun isLoggedIn(): Boolean {
        return sessionManager.fetchAuthToken() != null
    }

    // Method to get the current access token
    fun getCurrentAuthToken(): String? {
        return sessionManager.fetchAuthToken()
    }
}
