package app.sanctuary.android.data

import app.sanctuary.android.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.IOException

interface SanctuaryApiService {
    @POST("auth/register")
    suspend fun register(@Body request: AuthRegisterRequest): AuthRegistrationResponse

    @POST("auth/confirm")
    suspend fun confirm(@Body request: AuthConfirmRequest): AuthStatusResponse

    @POST("auth/resend-confirmation")
    suspend fun resendConfirmation(@Body request: AuthResendRequest): AuthStatusResponse

    @POST("auth/login")
    suspend fun login(@Body request: AuthLoginRequest): AuthSessionResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: AuthRefreshRequest): AuthSessionResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: AuthForgotPasswordRequest): AuthStatusResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: AuthResetPasswordRequest): AuthStatusResponse

    @GET("me")
    suspend fun me(): UserProfileResponse

    @GET("content/saints/search")
    suspend fun listSaints(
        @Query("lang") lang: String = "en",
        @Query("query") query: String = ""
    ): List<SaintSummaryResponse>

    @GET("content/novenas")
    suspend fun listNovenas(
        @Query("lang") lang: String = "en",
        @Query("query") query: String = ""
    ): List<NovenaSummaryResponse>
}

class AuthHeaderInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }

        return chain.proceed(request)
    }
}

object SanctuaryApiFactory {
    fun create(tokenProvider: () -> String?): SanctuaryApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.ENVIRONMENT == "prod") {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor(tokenProvider))
            .addInterceptor(logging)
            .build()

        val baseUrl = BuildConfig.API_BASE_URL.trim().let {
            if (it.endsWith("/")) it else "$it/"
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SanctuaryApiService::class.java)
    }
}

class SanctuaryApiException(message: String) : IOException(message)

suspend fun <T> runApiCall(block: suspend () -> T): T {
    return try {
        block()
    } catch (exception: HttpException) {
        val message = exception.response()?.errorBody()?.string()
        val fallback = "Sanctuary could not complete that request right now."
        throw SanctuaryApiException(
            ApiJson.parseErrorMessage(message) ?: fallback
        )
    } catch (exception: IOException) {
        throw SanctuaryApiException(exception.message ?: "Sanctuary could not complete that request right now.")
    }
}
