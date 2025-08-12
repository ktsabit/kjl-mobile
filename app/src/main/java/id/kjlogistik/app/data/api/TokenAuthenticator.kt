// file: app/src/main/java/id/kjlogistik/app/data/api/TokenAuthenticator.kt
package id.kjlogistik.app.data.api

import android.util.Log
import id.kjlogistik.app.data.model.RefreshRequest
import id.kjlogistik.app.data.session.SessionManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton
import id.kjlogistik.app.di.BASE_URL // Import the base URL

@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
) : Authenticator {

    // Create a separate API service specifically for token refresh
    private val refreshApiService: AuthApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AuthApiService::class.java)

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite loops if the refresh token call also fails
        if (response.request.url.encodedPath.contains("auth/jwt/refresh/")) {
            return null
        }

        // We only want to intercept 401 Unauthorized responses
        if (response.code == 401) {
            val refreshToken = sessionManager.fetchRefreshToken()
            if (refreshToken != null) {
                // Use the separate API service for the synchronous refresh call
                val refreshResponse = refreshApiService.refreshToken(RefreshRequest(refreshToken)).execute()
                if (refreshResponse.isSuccessful) {
                    val newTokens = refreshResponse.body()
                    Log.d("TokenAuthenticator", "New tokens received: $newTokens")
                    if (newTokens != null) {
                        // Save the new tokens
                        sessionManager.saveAuthToken(newTokens.accessToken, newTokens.refreshToken)

                        // Retry the original request with the new access token
                        return response.request.newBuilder()
                            .header("Authorization", "Bearer ${newTokens.accessToken}")
                            .build()
                    }
                }
            }
            // If refresh token is null, or the refresh call failed, force logout
            sessionManager.clearAuthToken()
        }
        return null
    }
}