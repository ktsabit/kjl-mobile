package id.kjlogistik.app.data.model

import com.google.gson.annotations.SerializedName

// Request body for login
data class LoginRequest(
    val username: String,
    val password: String
)

// Response body for successful login
data class LoginResponse(
    @SerializedName("authToken") val authToken: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("message") val message: String? = null
)

// Generic error response from API
data class ErrorResponse(
    @SerializedName("error") val error: String,
    @SerializedName("message") val message: String? = null
)

// NEW: Request body for scan operation
data class ScanRequest(
    @SerializedName("batch_id") val batchId: String,
    @SerializedName("shipping_id") val shippingId: String
)

// NEW: Response body for successful scan operation
data class ScanResponse(
    @SerializedName("message") val message: String,
    @SerializedName("status") val status: String,
    @SerializedName("timestamp") val timestamp: String? = null
)