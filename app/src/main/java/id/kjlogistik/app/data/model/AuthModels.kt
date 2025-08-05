// app/data/model/AuthModels.kt
package id.kjlogistik.app.data.model

import com.google.gson.annotations.SerializedName

// Request body for login
data class LoginRequest(
    val username: String,
    val password: String
)

// Response body for successful login (now includes access and refresh tokens)
data class LoginResponse(
    @SerializedName("refresh") val refreshToken: String,
    @SerializedName("access") val accessToken: String
)

// Generic error response from API
data class ErrorResponse(
    @SerializedName("error") val error: String,
    @SerializedName("message") val message: String? = null
)

// UPDATED: Request body for inbound scan operation
data class InboundScanRequest( // Renamed from ScanRequest
    @SerializedName("qr_code_content") val qrCodeContent: String,
    @SerializedName("is_damaged") val isDamaged: Boolean = false, // NEW: Added is_damaged with default
    @SerializedName("location_hub_id") val locationHubId: String,
)

// UPDATED: Response body for successful inbound scan operation
data class InboundScanResponse( // Renamed from ScanResponse
    @SerializedName("message") val message: String,
    @SerializedName("status") val status: String,
    @SerializedName("timestamp") val timestamp: String? = null
)

// Existing: Response body for /auth/users/me endpoint
data class UserMeResponse(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("client") val client: String?,
    @SerializedName("hub") val hub: String?,
    @SerializedName("hub_name") val hubName: String?,
    @SerializedName("groups") val groups: List<String>
)
