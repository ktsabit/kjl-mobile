package id.kjlogistik.app.data.api

import id.kjlogistik.app.data.model.LoginRequest
import id.kjlogistik.app.data.model.LoginResponse
import id.kjlogistik.app.data.model.ScanRequest
import id.kjlogistik.app.data.model.ScanResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header // NEW: Required for Authorization header

interface AuthApiService {
    @POST("api/login") // Replace with your actual login endpoint
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // NEW: Scan endpoint
    @POST("api/scan") // Example endpoint, adjust as per your backend
    suspend fun scanQrCode(
        @Header("Authorization") authToken: String, // Expects "Bearer <token>"
        @Body request: ScanRequest
    ): Response<ScanResponse>
}