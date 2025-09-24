package id.kjlogistik.app.data.repository

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import id.kjlogistik.app.data.api.AuthApiService
import id.kjlogistik.app.data.model.*
import id.kjlogistik.app.data.session.SessionManager
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject
import java.io.IOException
import id.kjlogistik.app.data.model.Package
import id.kjlogistik.app.R

class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
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
        data class Error(val message: String, val isDuplicate: Boolean = false) : ManifestScanResult()
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
        data class Success(val response: MarkDeliveredResponse) : MarkDeliveredResult()
        data class Error(val message: String, val isDuplicate: Boolean = false) : MarkDeliveredResult()
    }
    sealed class GetManifestResult {
        data class Success(val manifest: Manifest) : GetManifestResult()
        data class Error(val message: String) : GetManifestResult()
    }

    sealed class GetWaybillsResult {
        data class Success(val waybills: List<Waybill>) : GetWaybillsResult()
        data class Error(val message: String) : GetWaybillsResult()
    }

    sealed class LoadPackageResult {
        data class Success(val aPackage: Package) : LoadPackageResult()
        data class Error(val message: String) : LoadPackageResult()
    }

    sealed class VersionCheckResult {
        data class Success(val response: AppVersionResponse) : VersionCheckResult()
        data class Error(val message: String) : VersionCheckResult()
    }


    suspend fun checkAppVersion(): VersionCheckResult {
        // Replace with your actual username and repo name
        val versionUrl = "https://raw.githubusercontent.com/ktsabit/kjl-app-config/refs/heads/main/version.json"

        // Replace with your actual GitHub PAT
        val token = context.getString(R.string.github_pat)
        val githubToken = "Bearer $token"

        return try {
            val response = authApiService.getLatestAppVersion(versionUrl, githubToken)
            if (response.isSuccessful && response.body() != null) {
                VersionCheckResult.Success(response.body()!!)
            } else {
                VersionCheckResult.Error("Failed to check version: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            VersionCheckResult.Error("An error occurred: ${e.message}")
        }
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

            if (response.isSuccessful && response.body() != null) {
                return MarkDeliveredResult.Success(response.body()!!)
            } else {
                // THE FIX: Handle the 409 Conflict for duplicates
                if (response.code() == 409) {
                    val errorBody = response.errorBody()?.string()
                    val parsedMessage = try {
                        Gson().fromJson(errorBody, ApiErrorResponse::class.java)?.message ?: "Package already delivered."
                    } catch (e: Exception) { "Package already delivered." }
                    return MarkDeliveredResult.Error(parsedMessage, isDuplicate = true)
                }
                return MarkDeliveredResult.Error("Failed to mark package as delivered: ${response.errorBody()?.string()}")
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
        isDamaged: Boolean
    ): ScanResult {
        val authToken = sessionManager.fetchAuthToken()
        if (authToken.isNullOrEmpty()) {
            return ScanResult.Error("Authentication token not found. Please log in.")
        }
        return try {
            // First, get the current user's hub ID
            val userResponse = authApiService.getUserMe("Bearer $authToken")
            if (!userResponse.isSuccessful || userResponse.body()?.hub == null) {
                return ScanResult.Error("Could not retrieve user's hub information.")
            }
            val hubId = userResponse.body()!!.hub!!

            // Now, make the scan request with the retrieved hub ID
            val request = PickupScanRequest(
                qrCodeContent = qrCodeContent,
                isDamaged = isDamaged,
                locationHubId = hubId
            )
            val scanResponse = authApiService.pickupScanPackage("Bearer $authToken", request)
            if (scanResponse.isSuccessful) {
                ScanResult.Success(scanResponse.body()?.message ?: "Scan successful.")
            } else {
                val errorBody = scanResponse.errorBody()?.string()
                ScanResult.Error("Scan failed: ${errorBody ?: scanResponse.message()}")
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

        try {
            val request = DepartureScanRequest(qrCodeContent = qrCodeContent)
            val response = authApiService.departureScanPackage("Bearer $authToken", manifestId, request)

            if (response.isSuccessful) {
                return ManifestScanResult.Success(response.body()?.message ?: "Scan successful.")
            } else {
                if (response.code() == 409) {
                    val errorBody = response.errorBody()?.string()
                    val parsedMessage = try {
                        Gson().fromJson(errorBody, ApiErrorResponse::class.java)?.message ?: "Package already scanned."
                    } catch (e: Exception) {
                        "Package already scanned."
                    }
                    return ManifestScanResult.Error(parsedMessage, isDuplicate = true)
                }
                return ManifestScanResult.Error("Scan failed with code: ${response.code()}", isDuplicate = false)
            }
        } catch (e: IOException) {
            return ManifestScanResult.Error("Network error. Please check your connection.", isDuplicate = false)
        } catch (e: Exception) {
            return ManifestScanResult.Error("An unexpected error occurred.", isDuplicate = false)
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
        try {
            val request = ArrivalScanRequest(qrCodeContent = qrCodeContent)
            val response = authApiService.arrivalScanPackage("Bearer $authToken", manifestId, request)
            if (response.isSuccessful) {
                return ManifestScanResult.Success(response.body()?.message ?: "Arrival scan successful.")
            } else {
                if (response.code() == 409) {
                    val errorBody = response.errorBody()?.string()
                    val parsedMessage = try {
                        Gson().fromJson(errorBody, ApiErrorResponse::class.java)?.message ?: "Package already scanned for arrival."
                    } catch (e: Exception) {
                        "Package already scanned for arrival."
                    }
                    return ManifestScanResult.Error(parsedMessage, isDuplicate = true)
                }
                return ManifestScanResult.Error("Scan failed with code: ${response.code()}", isDuplicate = false)
            }
        } catch (e: IOException) {
            return ManifestScanResult.Error("Network error. Please check your connection.", isDuplicate = false)
        }
        catch (e: Exception) {
            return ManifestScanResult.Error("An unexpected error occurred: ${e.message}")
        }
    }

    suspend fun getUserMe(authToken: String): Response<UserMeResponse> {
        return authApiService.getUserMe(authToken)
    }

    suspend fun loadPackage(manifestId: String, qrCodeContent: String): LoadPackageResult {
        val authToken = sessionManager.fetchAuthToken()
        if (authToken.isNullOrEmpty()) {
            return LoadPackageResult.Error("Authentication token not found.")
        }
        return try {
            val request = LoadPackageRequest(qrCodeContent)
            val response = authApiService.loadPackage("Bearer $authToken", manifestId, request)
            if (response.isSuccessful && response.body() != null) {
                LoadPackageResult.Success(response.body()!!)
            } else {
                LoadPackageResult.Error("Failed to load package: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            LoadPackageResult.Error("An error occurred: ${e.message}")
        }
    }
}