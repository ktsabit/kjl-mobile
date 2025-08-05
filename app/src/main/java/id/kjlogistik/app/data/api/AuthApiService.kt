// app/data/api/AuthApiService.kt
package id.kjlogistik.app.data.api

import id.kjlogistik.app.data.model.LoginRequest
import id.kjlogistik.app.data.model.LoginResponse
import id.kjlogistik.app.data.model.InboundScanRequest // NEW: Import new scan request model
import id.kjlogistik.app.data.model.InboundScanResponse // NEW: Import new scan response model
import id.kjlogistik.app.data.model.UserMeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.GET

interface AuthApiService {
    // Updated: Endpoint for JWT token creation
    @POST("auth/jwt/create")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Existing: Endpoint to get user details (e.g., groups)
    @GET("auth/users/me")
    suspend fun getUserMe(
        @Header("Authorization") authToken: String
    ): Response<UserMeResponse>

    // UPDATED: New scan endpoint for inbound packages
    @POST("api/packages/inbound-scan/") // Updated endpoint
    suspend fun inboundScanPackage( // Renamed method
        @Header("Authorization") authToken: String, // Expects "Bearer <token>"
        @Body request: InboundScanRequest // Use new request model
    ): Response<InboundScanResponse> // Use new response model
}
