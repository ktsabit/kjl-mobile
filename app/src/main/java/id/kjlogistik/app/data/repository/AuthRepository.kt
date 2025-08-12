package id.kjlogistik.app.data.repository

import id.kjlogistik.app.data.api.AuthApiService
import id.kjlogistik.app.data.model.*
import id.kjlogistik.app.data.session.SessionManager
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject
import java.io.IOException

class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val sessionManager: SessionManager
) {

    sealed class LoginResult {
        data class Success(val message: String, val groups: List<String>) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }
    sealed class ScanResult {
        data class Success(val message: String) : ScanResult()
        data class Error(val message: String) : ScanResult()
    }

    sealed class ManifestListResult {
        data class Success(val manifests: List<Manifest>) : ManifestListResult()
        data class Error(val message: String) : ManifestListResult()
    }

    sealed class ManifestScanResult {
        data class Success(val message: String) : ManifestScanResult()
        data class Error(val message: String) : ManifestScanResult()
    }



    suspend fun login(request: LoginRequest): LoginResult {
        try {
            val loginResponse = authApiService.login(request)
            if (loginResponse.isSuccessful) {
                val loginBody = loginResponse.body()
                val accessToken = loginBody?.accessToken
                if (accessToken.isNullOrEmpty()) {
                    return LoginResult.Error("Login successful, but no access token received.")
                }

                val userMeResponse = authApiService.getUserMe("Bearer $accessToken")
                if (userMeResponse.isSuccessful) {
                    val userMeBody = userMeResponse.body()
                    val userGroups = userMeBody?.groups
                    if (userGroups != null) {
                        val allowedGroups = listOf("Housekeeper", "Driver")
                        if (userGroups.any { it in allowedGroups }) {
                            sessionManager.saveAuthToken(accessToken)
                            sessionManager.saveUserGroups(userGroups)
                            val message = if (userGroups.contains("Housekeeper")) {
                                "Login successful. Welcome, Housekeeper!"
                            } else {
                                "Login successful. Welcome, Driver!"
                            }
                            return LoginResult.Success(message, userGroups) // Pass userGroups back
                        } else {
                            sessionManager.clearAuthToken()
                            return LoginResult.Error("Access denied: User is not a Housekeeper or Driver.")
                        }
                    } else {
                        sessionManager.clearAuthToken()
                        return LoginResult.Error("Access denied: User is not a Housekeeper.")
                    }
                } else {
                    val errorBody = userMeResponse.errorBody()?.string()
                    return LoginResult.Error("Failed to fetch user details: ${errorBody ?: userMeResponse.message()}")
                }
            } else {
                val errorBody = loginResponse.errorBody()?.string()
                return LoginResult.Error("Login failed: ${errorBody ?: loginResponse.message()}")
            }
        } catch (e: HttpException) {
            return LoginResult.Error("Network error: ${e.code()} - ${e.message()}")
        } catch (e: IOException) {
            return LoginResult.Error("Connectivity error: ${e.message}")
        } catch (e: Exception) {
            return LoginResult.Error("An unexpected error occurred: ${e.message}")
        }
    }

    suspend fun pickupScanPackage(
        qrCodeContent: String,
        isDamaged: Boolean,
        locationHubId: String
    ): ScanResult {
        val authToken = sessionManager.fetchAuthToken()
        if (authToken.isNullOrEmpty()) {
            return ScanResult.Error("Authentication token not found. Please log in.")
        }
        return try {
            val request = InboundScanRequest(
                qrCodeContent = qrCodeContent,
                isDamaged = isDamaged,
                locationHubId = locationHubId
            )
            val response = authApiService.pickupScanPackage("Bearer $authToken", request)
            if (response.isSuccessful) {
                val responseBody = response.body()
                ScanResult.Success(responseBody?.message ?: "Scan successful.")
            } else {
                val errorBody = response.errorBody()?.string()
                ScanResult.Error("Scan failed: ${errorBody ?: response.message()}")
            }
        } catch (e: HttpException) {
            ScanResult.Error("Network error during scan: ${e.code()} - ${e.message()}")
        } catch (e: IOException) {
            ScanResult.Error("Connectivity error during scan: ${e.message}")
        } catch (e: Exception) {
            ScanResult.Error("An unexpected error occurred during scan: ${e.message}")
        }
    }

    suspend fun getManifestsForDeparture(): ManifestListResult {
        val authToken = sessionManager.fetchAuthToken()
        if (authToken.isNullOrEmpty()) {
            return ManifestListResult.Error("Authentication token not found. Please log in.")
        }
        return try {
            val response = authApiService.getManifestsForDeparture("Bearer $authToken", "PENDING_DEPARTURE_SCAN")
            if (response.isSuccessful) {
                ManifestListResult.Success(response.body() ?: emptyList())
            } else {
                val errorBody = response.errorBody()?.string()
                ManifestListResult.Error("Failed to fetch manifests: ${errorBody ?: response.message()}")
            }
        } catch (e: Exception) {
            ManifestListResult.Error("An error occurred: ${e.message}")
        }
    }

    suspend fun departureScanPackage(manifestId: String, qrCodeContent: String): ManifestScanResult {
        val authToken = sessionManager.fetchAuthToken()
        if (authToken.isNullOrEmpty()) {
            return ManifestScanResult.Error("Authentication token not found. Please log in.")
        }
        return try {
            val request = DepartureScanRequest(qrCodeContent = qrCodeContent)
            val response = authApiService.departureScanPackage("Bearer $authToken", manifestId, request)
            if (response.isSuccessful) {
                ManifestScanResult.Success(response.body()?.message ?: "Departure scan successful.")
            } else {
                val errorBody = response.errorBody()?.string()
                ManifestScanResult.Error("Departure scan failed: ${errorBody ?: response.message()}")
            }
        } catch (e: Exception) {
            ManifestScanResult.Error("An error occurred: ${e.message}")
        }
    }

    suspend fun getManifestsForArrival(): ManifestListResult {
        val authToken = sessionManager.fetchAuthToken()
        if (authToken.isNullOrEmpty()) {
            return ManifestListResult.Error("Authentication token not found. Please log in.")
        }
        return try {
            val response = authApiService.getManifestsForArrival("Bearer $authToken", "IN_TRANSIT")
            if (response.isSuccessful) {
                ManifestListResult.Success(response.body() ?: emptyList())
            } else {
                val errorBody = response.errorBody()?.string()
                ManifestListResult.Error("Failed to fetch manifests: ${errorBody ?: response.message()}")
            }
        } catch (e: Exception) {
            ManifestListResult.Error("An error occurred: ${e.message}")
        }
    }

    suspend fun arrivalScanPackage(manifestId: String, qrCodeContent: String): ManifestScanResult {
        val authToken = sessionManager.fetchAuthToken()
        if (authToken.isNullOrEmpty()) {
            return ManifestScanResult.Error("Authentication token not found. Please log in.")
        }
        return try {
            val request = ArrivalScanRequest(qrCodeContent = qrCodeContent)
            val response = authApiService.arrivalScanPackage("Bearer $authToken", manifestId, request)
            if (response.isSuccessful) {
                ManifestScanResult.Success(response.body()?.message ?: "Arrival scan successful.")
            } else {
                val errorBody = response.errorBody()?.string()
                ManifestScanResult.Error("Arrival scan failed: ${errorBody ?: response.message()}")
            }
        } catch (e: Exception) {
            ManifestScanResult.Error("An error occurred: ${e.message}")
        }
    }

    suspend fun getUserMe(authToken: String): Response<UserMeResponse> {
        return authApiService.getUserMe(authToken)
    }
}