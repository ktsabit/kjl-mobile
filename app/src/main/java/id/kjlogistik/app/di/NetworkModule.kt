package id.kjlogistik.app.di

import id.kjlogistik.app.data.api.AuthApiService
import id.kjlogistik.app.data.repository.AuthRepository
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
import dagger.hilt.android.qualifiers.ApplicationContext // NEW
import android.content.Context // NEW

// IMPORTANT: Replace with your actual Cloudflared tunnel URL.
// This is critical for your physical device to connect.
// Example: "https://my-warehouse-api.trycloudflare.com/"
const val BASE_URL = "https://dev-api.kaisan.dev/" // <--- UPDATE THIS!

@Module
@InstallIn(SingletonComponent::class) // This module's dependencies live as long as the application
object NetworkModule {

    @Singleton
    @Provides
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor // Inject logging interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Singleton
    @Provides
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor { // NEW: Provide logging interceptor separately
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Log request and response bodies
        }
    }

    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL) // Use the constant defined above
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
    fun provideAuthRepository(authApiService: AuthApiService): AuthRepository {
        return AuthRepository(authApiService)
    }

    // NEW: Provide SessionManager
    @Singleton
    @Provides
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }
}