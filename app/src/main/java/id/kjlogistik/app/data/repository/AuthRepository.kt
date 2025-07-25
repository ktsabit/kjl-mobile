package id.kjlogistik.app.data.repository

import id.kjlogistik.app.data.api.AuthApiService
import id.kjlogistik.app.data.model.ErrorResponse
import id.kjlogistik.app.data.model.LoginRequest
import id.kjlogistik.app.data.model.LoginResponse
import id.kjlogistik.app.data.model.ScanRequest // NEW
import id.kjlogistik.app.data.model.ScanResponse // NEW
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

// A sealed class to represent the outcome of an operation (Success, Error, Loading)
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class AuthRepository(private val authApiService: AuthApiService) {

    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApiService.login(request)
                if (response.isSuccessful) {
                    response.body()?.let {
                        Result.Success(it)
                    } ?: Result.Error(IOException("Empty response body"), "Unknown error")
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        Gson().fromJson(errorBody, ErrorResponse::class.java)?.message
                    } catch (e: Exception) {
                        null
                    } ?: "Login failed: ${response.code()}"
                    Result.Error(HttpException(response), errorMessage)
                }
            } catch (e: IOException) {
                Result.Error(e, "Network error. Please check your internet connection.")
            } catch (e: HttpException) {
                Result.Error(e, "Server error: ${e.message()}")
            } catch (e: Exception) {
                Result.Error(e, "An unexpected error occurred.")
            }
        }
    }

    // NEW: Scan function in repository
    suspend fun scanQrCode(authToken: String, request: ScanRequest): Result<ScanResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Ensure the token is prefixed with "Bearer " as per common API standards
                val formattedToken = if (authToken.startsWith("Bearer ")) authToken else "Bearer $authToken"
                val response = authApiService.scanQrCode(formattedToken, request)
                if (response.isSuccessful) {
                    response.body()?.let {
                        Result.Success(it)
                    } ?: Result.Error(IOException("Empty response body"), "Unknown scan response")
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        Gson().fromJson(errorBody, ErrorResponse::class.java)?.message
                    } catch (e: Exception) {
                        null
                    } ?: "Scan failed: ${response.code()}"
                    Result.Error(HttpException(response), errorMessage)
                }
            } catch (e: IOException) {
                Result.Error(e, "Network error during scan. Please check your internet connection.")
            } catch (e: HttpException) {
                Result.Error(e, "Server error during scan: ${e.message()}")
            } catch (e: Exception) {
                Result.Error(e, "An unexpected error occurred during scan.")
            }
        }
    }
}