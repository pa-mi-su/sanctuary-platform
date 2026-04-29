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
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

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

    @PUT("me/preferences")
    suspend fun updateMePreferences(@Body request: UserPreferencesUpdateRequest): UserProfileResponse

    @GET("me/favorites")
    suspend fun listFavorites(): List<UserFavoriteResponse>

    @PUT("me/favorites/{itemType}/{itemId}")
    suspend fun saveFavorite(
        @retrofit2.http.Path("itemType") itemType: String,
        @retrofit2.http.Path("itemId") itemId: String
    )

    @DELETE("me/favorites/{itemType}/{itemId}")
    suspend fun deleteFavorite(
        @retrofit2.http.Path("itemType") itemType: String,
        @retrofit2.http.Path("itemId") itemId: String
    )

    @GET("me/novena-commitments")
    suspend fun listNovenaCommitments(): List<UserNovenaCommitmentResponse>

    @PUT("me/novena-commitments/{novenaId}")
    suspend fun saveNovenaCommitment(
        @retrofit2.http.Path("novenaId") novenaId: String,
        @Body request: UserNovenaCommitmentRequest
    ): UserNovenaCommitmentResponse

    @DELETE("me/novena-commitments/{novenaId}")
    suspend fun deleteNovenaCommitment(
        @retrofit2.http.Path("novenaId") novenaId: String
    )

    @GET("content/saints/search")
    suspend fun listSaints(
        @Query("lang") lang: String = "en",
        @Query("query") query: String = ""
    ): List<SaintSummaryResponse>

    @GET("content/saints")
    suspend fun listSaintsByDay(
        @Query("month") month: Int,
        @Query("day") day: Int,
        @Query("lang") lang: String = "en"
    ): List<SaintSummaryResponse>

    @GET("content/saints/range")
    suspend fun listSaintsInRange(
        @Query("start") start: String,
        @Query("end") end: String,
        @Query("lang") lang: String = "en"
    ): List<SaintDateGroupResponse>

    @GET("content/saints/{slug}")
    suspend fun getSaintDetail(
        @retrofit2.http.Path("slug") slug: String,
        @Query("lang") lang: String = "en"
    ): SaintDetailResponse

    @GET("content/prayers")
    suspend fun listPrayers(
        @Query("lang") lang: String = "en",
        @Query("query") query: String = ""
    ): List<PrayerSummaryResponse>

    @GET("content/prayers/{slug}")
    suspend fun getPrayerDetail(
        @retrofit2.http.Path("slug") slug: String,
        @Query("lang") lang: String = "en"
    ): PrayerDetailResponse

    @GET("content/novenas")
    suspend fun listNovenas(
        @Query("lang") lang: String = "en",
        @Query("query") query: String = ""
    ): List<NovenaSummaryResponse>

    @GET("content/novenas/intentions")
    suspend fun listNovenasByIntentions(
        @Query("lang") lang: String = "en",
        @Query("query") query: String = ""
    ): List<NovenaSummaryResponse>

    @GET("content/novenas/calendar")
    suspend fun listNovenasCalendarRange(
        @Query("start") start: String,
        @Query("end") end: String,
        @Query("lang") lang: String = "en"
    ): List<NovenaCalendarDateResponse>

    @GET("content/novenas/{slug}")
    suspend fun getNovenaDetail(
        @retrofit2.http.Path("slug") slug: String,
        @Query("lang") lang: String = "en"
    ): NovenaDetailResponse

    @GET("calendar/range")
    suspend fun listLiturgicalRange(
        @Query("start") start: String,
        @Query("end") end: String
    ): List<LiturgicalDayResponse>
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
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
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
