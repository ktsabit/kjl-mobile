package id.kjlogistik.app.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("refresh") val refreshToken: String,
    @SerializedName("access") val accessToken: String
)

data class RefreshRequest(
    val refresh: String
)


data class ErrorResponse(
    @SerializedName("error") val error: String,
    @SerializedName("message") val message: String? = null
)

data class InboundScanRequest(
    @SerializedName("qr_code_content") val qrCodeContent: String,
    @SerializedName("is_damaged") val isDamaged: Boolean = false,
    @SerializedName("location_hub_id") val locationHubId: String,
)

data class InboundScanResponse(
    @SerializedName("message") val message: String,
    @SerializedName("status") val status: String,
    @SerializedName("timestamp") val timestamp: String? = null
)

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

data class Manifest(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("manifest_number") val manifestNumber: String,
    @SerializedName("total_packages") val totalPackages: Int,
    @SerializedName("scanned_packages_count") val scannedPackagesCount: Int,
    @SerializedName("arrival_scanned_count") val arrivalScannedCount: Int,
    @SerializedName("waybills") val waybills: List<Waybill>? = null
)

data class Waybill(
    @SerializedName("waybill_number") val waybillNumber: String,
    @SerializedName("packages_qty") val packagesQty: Int
)

data class DepartureScanRequest(
    @SerializedName("qr_code_content") val qrCodeContent: String
)

data class DepartureScanResponse(
    val message: String
)

data class ArrivalScanRequest(
    @SerializedName("qr_code_content") val qrCodeContent: String
)

data class ArrivalScanResponse(
    val message: String
)