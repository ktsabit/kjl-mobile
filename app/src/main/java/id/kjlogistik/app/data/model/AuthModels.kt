package id.kjlogistik.app.data.model

import com.google.gson.annotations.SerializedName

// --- Data class for the new grouped structure ---
data class WaybillGroup(
    @SerializedName("waybill_number") val waybillNumber: String,
    @SerializedName("packages") val packages: List<Package>
)

// --- Updated Manifest to use the new structure ---
data class Manifest(
    @SerializedName("id") val id: String?,
    @SerializedName("manifest_number") val manifestNumber: String?,
    @SerializedName("manifest_id") val manifestId: String?,
    @SerializedName("status") val status: String,
    @SerializedName("progress") val progress: ManifestProgress?,
    // This is the key change for the new UI
    @SerializedName("waybills") val waybills: List<WaybillGroup>?,

    // Fields from original Manifest model (for lists, can be deprecated)
    @SerializedName("origin_hub") val originHub: Hub?,
    @SerializedName("destination_hub") val destinationHub: Hub?,
    @SerializedName("total_packages") val totalPackages: Int?,
    @SerializedName("scanned_packages_count") val scannedPackagesCount: Int?,
    @SerializedName("arrival_scanned_count") val arrivalScannedCount: Int?
)

// --- Updated Package to use QR code content ---
data class Package(
    @SerializedName("qr_code_content") val qrCodeContent: String,
    @SerializedName("status") val status: String
)


// --- Other models remain largely the same ---

data class ApiErrorResponse(
    val message: String?,
    val error: String?
)

data class PaginatedManifestResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("next") val next: String?,
    @SerializedName("previous") val previous: String?,
    @SerializedName("results") val results: List<Manifest>
)

data class PaginatedWaybillResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("next") val next: String?,
    @SerializedName("previous") val previous: String?,
    @SerializedName("results") val results: List<Waybill>
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("refresh") val refreshToken: String,
    @SerializedName("access") val accessToken: String
)

data class AppVersionResponse(
    @SerializedName("latest_version") val latestVersion: String,
    @SerializedName("is_force_update") val isForceUpdate: Boolean,
    @SerializedName("update_url") val updateUrl: String?
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

data class ManifestProgress(
    @SerializedName("scanned_count") val scannedCount: Int,
    @SerializedName("total_count") val totalCount: Int
)

data class Hub(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("city") val city: String?,
    @SerializedName("address") val address: Address
)

data class Waybill(
    @SerializedName("id")
    val id: String,
    @SerializedName("waybill_number")
    val waybillNumber: String?,
    @SerializedName("packages_qty")
    val packagesQty: Int,
    @SerializedName("recipient")
    val recipient: Recipient,
    @SerializedName("client")
    val client: Client,
    @SerializedName("packages")
    val packages: List<PackageInfo>
)

data class Recipient(
    @SerializedName("name")
    val name: String,
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("address")
    val address: Address
)

data class Client(
    @SerializedName("name")
    val name: String,
    @SerializedName("code")
    val code: String
)

data class PackageInfo(
    @SerializedName("id")
    val id: String,
    @SerializedName("qr_code_content")
    val qrCodeContent: String
)

data class Address(
    @SerializedName("id") val id: String,
    @SerializedName("street") val street: String,
    @SerializedName("city") val city: String,
    @SerializedName("country") val country: String,
    @SerializedName("postal_code") val postalCode: String?
)

data class RefreshRequest(
    val refresh: String
)

data class ErrorResponse(
    @SerializedName("error") val error: String,
    @SerializedName("message") val message: String? = null
)

data class PickupScanRequest(
    @SerializedName("qr_code_content") val qrCodeContent: String,
    @SerializedName("is_damaged") val isDamaged: Boolean = false,
    @SerializedName("location_hub_id") val locationHubId: String,
)

data class PickupScanResponse(
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

data class CreateManifestRequest(
    @SerializedName("is_direct_delivery_manifest") val isDirectDeliveryManifest: Boolean,
    @SerializedName("driver_id") val driverId: String,
    @SerializedName("origin_hub_id") val originHubId: String
)

data class AddWaybillRequest(
    @SerializedName("qr_code_content") val qrCodeContent: String
)

data class MarkDeliveredRequest(
    @SerializedName("qr_code_content") val qrCodeContent: String
)

data class Driver(
    @SerializedName("id") val id: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("hub_name") val hubName: String
)

data class LoadPackageRequest(
    @SerializedName("qr_code_content") val qrCodeContent: String
)


data class MarkDeliveredResponse(
    @SerializedName("message") val message: String,
    @SerializedName("waybill_number") val waybillNumber: String,
    // THE FIX: Add the new flag from the backend
    @SerializedName("is_waybill_complete") val isWaybillComplete: Boolean
)