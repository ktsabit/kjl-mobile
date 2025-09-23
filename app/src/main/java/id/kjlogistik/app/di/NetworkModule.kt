package id.kjlogistik.app.di

import id.kjlogistik.app.data.api.AuthApiService
import id.kjlogistik.app.data.repository.AuthRepository // NEW: Import AuthRepository
import id.kjlogistik.app.data.session.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import id.kjlogistik.app.data.api.TokenAuthenticator
import com.chuckerteam.chucker.api.ChuckerInterceptor
import okhttp3.Interceptor
import javax.inject.Inject

// IMPORTANT: Replace with your actual Cloudflared tunnel URL.
// This is critical for your physical device to connect.
// Example: "https://my-warehouse-api.trycloudflare.com/"
const val BASE_URL = "https://api.kjlogistik.id/" // <--- UPDATE THIS!
//const val BASE_URL = "https://dev-api.kaisan.dev/" // <--- UPDATE THIS!

// --- NEW DYNAMIC INTERCEPTOR CLASS ---
// This interceptor acts as a gatekeeper for the real Chucker interceptor.
class DynamicChuckerInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    private val chuckerInterceptor: ChuckerInterceptor
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        // Check our flag before deciding whether to use Chucker
        return if (sessionManager.isChuckerEnabled()) {
            // If the flag is true, let the real Chucker interceptor do its job.
            chuckerInterceptor.intercept(chain)
        } else {
            // Otherwise, just continue the request without Chucker.
            chain.proceed(chain.request())
        }
    }
}


@Module
@InstallIn(SingletonComponent::class) // This module's dependencies live as long as the application
object NetworkModule {

    // Provider for the real Chucker Interceptor
    @Provides
    @Singleton
    fun provideChuckerInterceptor(@ApplicationContext context: Context): ChuckerInterceptor {
        return ChuckerInterceptor.Builder(context)
            .maxContentLength(250_000L)
            .alwaysReadResponseBody(true)
            .build()
    }

    @Singleton
    @Provides
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        sessionManager: SessionManager, // Inject SessionManager
        tokenAuthenticator: TokenAuthenticator,
        chuckerInterceptor: ChuckerInterceptor // Inject the real Chucker Interceptor

    ): OkHttpClient {

        // Create an instance of our new dynamic interceptor
        val dynamicChucker = DynamicChuckerInterceptor(sessionManager, chuckerInterceptor)

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(dynamicChucker)
            .build()
    }

    @Singleton
    @Provides
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Singleton
    @Provides
    fun provideAuthRepository(
        authApiService: AuthApiService,
        sessionManager: SessionManager, // NEW: Inject SessionManager
        @ApplicationContext context: Context
    ): AuthRepository {
        return AuthRepository(authApiService, sessionManager, context) // Pass SessionManager
    }

    @Singleton
    @Provides
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }
}


