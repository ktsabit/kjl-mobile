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

    sealed class CreateManifestResult {
        data class Success(val manifest: Manifest) : CreateManifestResult()
        data class Error(val message: String) : CreateManifestResult()
    }

    sealed class AddWaybillResult {
        data class Success(val waybill: Waybill) : AddWaybillResult()
        data class Error(val message: String) : AddWaybillResult()
    }

    sealed class StartRunResult {
        object Success : StartRunResult()
        data class Error(val message: String) : StartRunResult()
    }

    sealed class MarkDeliveredResult {
        object Success : MarkDeliveredResult()
        data class Error(val message: String) : MarkDeliveredResult()
    }

    sealed class GetManifestResult {
        data class Success(val manifest: Manifest) : GetManifestResult()
        data class Error(val message: String) : GetManifestResult()
    }

    sealed class GetWaybillsResult {
        data class Success(val waybills: List<Waybill>) : GetWaybillsResult()
        data class Error(val message: String) : GetWaybillsResult()
    }


    suspend fun getManifestDetails(manifestId: String): GetManifestResult {
        val authToken = sessionManager.fetchAuthToken()
        if (authToken.isNullOrEmpty()) {
            return GetManifestResult.Error("Authentication token not found.")
        }
        return try {
            val response = authApiService.getManifestDetails("Bearer $authToken", manifestId)
            if (response.isSuccessful && response.body() != null) {
                GetManifestResult.Success(response.body()!!)
            } else {
                GetManifestResult.Error("Failed to fetch manifest details: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            GetManifestResult.Error("An error occurred: ${e.message}")
        }
    }

    suspend fun getWaybillsForManifest(manifestId: String): GetWaybillsResult {
        val authToken = sessionManager.fetchAuthToken()
        if (authToken.isNullOrEmpty()) {
            return GetWaybillsResult.Error("Authentication token not found.")
        }
        return try {
            val response = authApiService.getWaybillsForManifest("Bearer $authToken", manifestId)
            if (response.isSuccessful && response.body() != null) {
                GetWaybillsResult.Success(response.body()!!.results)
            } else {
                GetWaybillsResult.Error("Failed to fetch waybills: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            GetWaybillsResult.Error("An error occurred: ${e.message}")
        }
    }


    suspend fun createManifest(): CreateManifestResult {
        val authToken = sessionManager.fetchAuthToken()
        if (authToken.isNullOrEmpty()) {
            return CreateManifestResult.Error("Authentication token not found.")
        }
        try {
            val userMeResponse = getUserMe("Bearer $authToken")
            if (!userMeResponse.isSuccessful || userMeResponse.body() == null) {
                return CreateManifestResult.Error("Failed to retrieve user details.")
            }
            val user = userMeResponse.body()!!
            val request = CreateManifestRequest(
                isDirectDeliveryManifest = true,
                driverId = user.id,
                originHubId = user.hub ?: return CreateManifestResult.Error("User hub ID is missing.")
            )
            val response = authApiService.createManifest("Bearer $authToken", request)
            return if (response.isSuccessful && response.body() != null) {
                CreateManifestResult.Success(response.body()!!)
            } else {
                CreateManifestResult.Error("Failed to create manifest: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            return CreateManifestResult.Error("An error occurred: ${e.message}")
        }
    }

    suspend fun addWaybillToManifest(manifestId: String, qrCodeContent: String): AddWaybillResult {
        val authToken = sessionManager.fetchAuthToken()
        if (authToken.isNullOrEmpty()) {
            return AddWaybillResult.Error("Authentication token not found.")
        }
        try {
            val request = AddWaybillRequest(qrCodeContent)
            val response = authApiService.addWaybillToManifest("Bearer $authToken", manifestId, request)
            return if (response.isSuccessful && response.body() != null) {
                AddWaybillResult.Success(response.body()!!)
            } else {
                AddWaybillResult.Error("Failed to add waybill: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            return AddWaybillResult.Error("An error occurred: ${e.message}")
        }
    }

    suspend fun startRun(manifestId: String): StartRunResult {
        val authToken = sessionManager.fetchAuthToken()
        if (authToken.isNullOrEmpty()) {
            return StartRunResult.Error("Authentication token not found.")
        }
        try {
            val response = authApiService.startRun("Bearer $authToken", manifestId)
            return if (response.isSuccessful) {
                StartRunResult.Success
            } else {
                StartRunResult.Error("Failed to start run: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            return StartRunResult.Error("An error occurred: ${e.message}")
        }
    }

    suspend fun markPackageAsDelivered(qrCodeContent: String): MarkDeliveredResult {
        val authToken = sessionManager.fetchAuthToken()
        if (authToken.isNullOrEmpty()) {
            return MarkDeliveredResult.Error("Authentication token not found.")
        }
        try {
            val request = MarkDeliveredRequest(qrCodeContent)
            val response = authApiService.markPackageAsDelivered("Bearer $authToken", request)
            return if (response.isSuccessful) {
                MarkDeliveredResult.Success
            } else {
                MarkDeliveredResult.Error("Failed to mark package as delivered: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            return MarkDeliveredResult.Error("An error occurred: ${e.message}")
        }
    }


    // ... (existing functions remain unchanged) ...
    suspend fun login(request: LoginRequest): LoginResult {
        try {
            val loginResponse = authApiService.login(request)
            if (loginResponse.isSuccessful) {
                val loginBody = loginResponse.body()
                val accessToken = loginBody?.accessToken
                val refreshToken = loginBody?.refreshToken
                if (accessToken.isNullOrEmpty()) {
                    return LoginResult.Error("Login successful, but no access token received.")
                }

                if (refreshToken.isNullOrEmpty()) {
                    return LoginResult.Error("Login successful, but no refresh token received.")
                }

                val userMeResponse = authApiService.getUserMe("Bearer $accessToken")
                if (userMeResponse.isSuccessful) {
                    val userMeBody = userMeResponse.body()
                    val userGroups = userMeBody?.groups
                    if (userGroups != null) {
                        val allowedGroups = listOf("Housekeeper", "Driver")
                        if (userGroups.any { it in allowedGroups }) {
                            sessionManager.saveAuthToken(accessToken, refreshToken)
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
            val userMeResponse = getUserMe("Bearer $authToken")
            if (userMeResponse.isSuccessful) {
                val hubId = userMeResponse.body()?.hub
                if (hubId != null) {
                    val response = authApiService.getManifestsForDeparture("Bearer $authToken", "PENDING_DEPARTURE_SCAN", hubId)
                    if (response.isSuccessful) {
                        ManifestListResult.Success(response.body()?.results ?: emptyList())
                    } else {
                        val errorBody = response.errorBody()?.string()
                        ManifestListResult.Error("Failed to fetch manifests: ${errorBody ?: response.message()}")
                    }
                } else {
                    ManifestListResult.Error("Hub ID not found for the user.")
                }
            } else {
                ManifestListResult.Error("Failed to fetch user details.")
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
            val userMeResponse = getUserMe("Bearer $authToken")
            if (userMeResponse.isSuccessful) {
                val hubId = userMeResponse.body()?.hub
                if (hubId != null) {
                    val response = authApiService.getManifestsForArrival("Bearer $authToken", "IN_TRANSIT", hubId)
                    if (response.isSuccessful) {
                        ManifestListResult.Success(response.body()?.results ?: emptyList())
                    } else {
                        val errorBody = response.errorBody()?.string()
                        ManifestListResult.Error("Failed to fetch manifests: ${errorBody ?: response.message()}")
                    }
                } else {
                    ManifestListResult.Error("Hub ID not found for the user.")
                }
            } else {
                ManifestListResult.Error("Failed to fetch user details.")
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