package id.kjlogistik.app.data.model

import com.google.gson.annotations.SerializedName

// --- Top Level API Response Models ---

data class PaginatedManifestResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("next") val next: String?,
    @SerializedName("previous") val previous: String?,
    @SerializedName("results") val results: List<Manifest>
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("refresh") val refreshToken: String,
    @SerializedName("access") val accessToken: String
)

data class UserMeResponse(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("hub") val hub: String?,
    @SerializedName("hub_name") val hubName: String?,
    @SerializedName("groups") val groups: List<String>
)

// --- Nested Data Structures from Manifest ---

data class Manifest(
    @SerializedName("id") val id: String,
    @SerializedName("manifest_number") val manifestNumber: String,
    @SerializedName("status") val status: String,
    @SerializedName("origin_hub") val originHub: Hub,
    @SerializedName("destination_hub") val destinationHub: Hub,
    @SerializedName("total_packages") val totalPackages: Int,
    @SerializedName("scanned_packages_count") val scannedPackagesCount: Int,
    @SerializedName("arrival_scanned_count") val arrivalScannedCount: Int
)

data class Hub(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("city") val city: String?, // Assuming city might be directly in hub object based on context
    @SerializedName("address") val address: Address
)

data class Address(
    @SerializedName("id") val id: String,
    @SerializedName("street") val street: String,
    @SerializedName("city") val city: String,
    @SerializedName("country") val country: String
)


// --- Request/Response for Scan Operations (Unchanged) ---

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

// Other models as needed, e.g., Waybill, Driver...
// These are not directly used by the UI in the current scope but are here for completeness
data class Waybill(
    @SerializedName("id") val id: String,
    @SerializedName("waybill_number") val waybillNumber: String,
    @SerializedName("packages_qty") val packagesQty: Int
)

data class Driver(
    @SerializedName("id") val id: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("hub_name") val hubName: String
)