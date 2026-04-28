package app.sanctuary.android.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionRepository(
    context: Context,
    private val api: SanctuaryApiService
) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "sanctuary_session",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val sessionKey = "primary_session"

    suspend fun bootstrap(): SessionBootstrapResult = withContext(Dispatchers.IO) {
        val stored = loadSession()
            ?: return@withContext SessionBootstrapResult.signedOut()

        if (stored.expiresAtMillis <= System.currentTimeMillis()) {
            val refreshToken = stored.refreshToken
            if (!refreshToken.isNullOrBlank()) {
                return@withContext refreshSession(refreshToken, stored)
            }

            clearSession()
            return@withContext SessionBootstrapResult.signedOut()
        }

        loadProfile(stored)
    }

    suspend fun login(email: String, password: String): SessionBootstrapResult = withContext(Dispatchers.IO) {
        val response = runApiCall {
            api.login(AuthLoginRequest(email = email, password = password))
        }
        val session = response.toStoredSession()
        persistSession(session)
        loadProfile(session)
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): AuthRegistrationResponse = withContext(Dispatchers.IO) {
        runApiCall {
            api.register(
                AuthRegisterRequest(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    password = password
                )
            )
        }
    }

    suspend fun confirm(email: String, code: String): AuthStatusResponse = withContext(Dispatchers.IO) {
        runApiCall {
            api.confirm(AuthConfirmRequest(email = email, code = code))
        }
    }

    suspend fun resendConfirmation(email: String): AuthStatusResponse = withContext(Dispatchers.IO) {
        runApiCall {
            api.resendConfirmation(AuthResendRequest(email = email))
        }
    }

    suspend fun forgotPassword(email: String): AuthStatusResponse = withContext(Dispatchers.IO) {
        runApiCall {
            api.forgotPassword(AuthForgotPasswordRequest(email = email))
        }
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String): AuthStatusResponse =
        withContext(Dispatchers.IO) {
            runApiCall {
                api.resetPassword(
                    AuthResetPasswordRequest(
                        email = email,
                        code = code,
                        newPassword = newPassword
                    )
                )
            }
        }

    suspend fun listSaints(query: String): List<SaintSummary> = withContext(Dispatchers.IO) {
        runApiCall { api.listSaints(query = query.trim()) }
            .map {
                SaintSummary(
                    id = it.id,
                    slug = it.slug,
                    name = it.name,
                    feastLabel = it.feastLabel,
                    summary = it.summary
                )
            }
    }

    suspend fun listNovenas(query: String): List<NovenaSummary> = withContext(Dispatchers.IO) {
        runApiCall { api.listNovenas(query = query.trim()) }
            .map {
                NovenaSummary(
                    id = it.id,
                    slug = it.slug,
                    title = it.title,
                    description = it.description,
                    durationDays = it.durationDays,
                    intentions = it.intentions.orEmpty()
                )
            }
    }

    fun currentSession(): StoredSession? = loadSession()

    fun logout() {
        clearSession()
    }

    private fun loadSession(): StoredSession? {
        val raw = preferences.getString(sessionKey, null) ?: return null
        return ApiJson.decodeSession(raw)
    }

    private fun persistSession(session: StoredSession) {
        preferences.edit().putString(sessionKey, ApiJson.encodeSession(session)).apply()
    }

    private fun clearSession() {
        preferences.edit().remove(sessionKey).apply()
    }

    private suspend fun refreshSession(
        refreshToken: String,
        previous: StoredSession
    ): SessionBootstrapResult {
        return try {
            val response = runApiCall {
                api.refresh(AuthRefreshRequest(refreshToken = refreshToken))
            }
            val session = response.toStoredSession(previous.refreshToken)
            persistSession(session)
            loadProfile(session)
        } catch (_: Exception) {
            clearSession()
            SessionBootstrapResult.signedOut()
        }
    }

    private suspend fun loadProfile(session: StoredSession): SessionBootstrapResult {
        return try {
            val profile = runApiCall { api.me() }.toUserProfile(session)
            SessionBootstrapResult.authenticated(session, profile)
        } catch (exception: Exception) {
            if (!session.refreshToken.isNullOrBlank()) {
                refreshSession(session.refreshToken, session)
            } else {
                clearSession()
                SessionBootstrapResult.failed(exception.message ?: "Please sign in to continue.")
            }
        }
    }
}

data class SessionBootstrapResult(
    val session: StoredSession?,
    val profile: UserProfile?,
    val errorMessage: String?,
    val authenticated: Boolean
) {
    companion object {
        fun authenticated(session: StoredSession, profile: UserProfile) = SessionBootstrapResult(
            session = session,
            profile = profile,
            errorMessage = null,
            authenticated = true
        )

        fun signedOut() = SessionBootstrapResult(
            session = null,
            profile = null,
            errorMessage = null,
            authenticated = false
        )

        fun failed(message: String) = SessionBootstrapResult(
            session = null,
            profile = null,
            errorMessage = message,
            authenticated = false
        )
    }
}

private fun AuthSessionResponse.toStoredSession(fallbackRefreshToken: String? = null): StoredSession {
    return StoredSession(
        accessToken = accessToken,
        idToken = idToken,
        refreshToken = refreshToken ?: fallbackRefreshToken,
        tokenType = tokenType,
        expiresAtMillis = System.currentTimeMillis() + (expiresIn * 1000L),
        email = email,
        displayName = displayName
    )
}

private fun UserProfileResponse.toUserProfile(session: StoredSession): UserProfile {
    return UserProfile(
        userId = userId,
        email = email,
        firstName = firstName,
        lastName = lastName,
        displayName = displayName ?: session.displayName,
        preferredLanguage = preferredLanguage,
        avatarUrl = avatarUrl,
        timeZoneId = timeZoneId,
        novenaRemindersEnabled = novenaRemindersEnabled,
        feastRemindersEnabled = feastRemindersEnabled,
        emailUpdatesEnabled = emailUpdatesEnabled,
        onboardingCompleted = onboardingCompleted,
        favoriteSaintCount = favoriteSaintCount,
        favoriteNovenaCount = favoriteNovenaCount,
        favoritePrayerCount = favoritePrayerCount,
        activeNovenaCount = activeNovenaCount,
        completedNovenaCount = completedNovenaCount
    )
}
