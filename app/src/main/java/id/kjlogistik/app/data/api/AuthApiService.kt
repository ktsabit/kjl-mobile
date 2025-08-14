package id.kjlogistik.app.data.api

import id.kjlogistik.app.data.model.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {
    @POST("auth/jwt/create")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/jwt/refresh/")
    fun refreshToken(@Body request: RefreshRequest): Call<LoginResponse> // CORRECTED: Now returns Call

    @GET("auth/users/me")
    suspend fun getUserMe(
        @Header("Authorization") authToken: String
    ): Response<UserMeResponse>

    @POST("api/packages/inbound-scan/")
    suspend fun pickupScanPackage(
        @Header("Authorization") authToken: String,
        @Body request: InboundScanRequest
    ): Response<InboundScanResponse>

    @GET("api/manifests/")
    suspend fun getManifestsForDeparture(
        @Header("Authorization") authToken: String,
        @Query("status") status: String,
        @Query("origin_hub") hubId: String
    ): Response<PaginatedManifestResponse>

    @POST("api/manifests/{id}/departure-scan/")
    suspend fun departureScanPackage(
        @Header("Authorization") authToken: String,
        @Path("id") manifestId: String,
        @Body request: DepartureScanRequest
    ): Response<DepartureScanResponse>

    @GET("api/manifests/")
    suspend fun getManifestsForArrival(
        @Header("Authorization") authToken: String,
        @Query("status") status: String,
        @Query("destination_hub") hubId: String
    ): Response<PaginatedManifestResponse>

    @POST("api/manifests/{id}/arrival-scan/")
    suspend fun arrivalScanPackage(
        @Header("Authorization") authToken: String,
        @Path("id") manifestId: String,
        @Body request: ArrivalScanRequest
    ): Response<ArrivalScanResponse>
}