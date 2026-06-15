package app.sanctuary.android.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.time.Instant
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
    private val languageKey = "preferred_language"

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

    fun currentLanguage(): String = preferences.getString(languageKey, null)?.ifBlank { null } ?: "en"

    suspend fun updatePreferredLanguage(language: String): UserProfile? = withContext(Dispatchers.IO) {
        persistLanguage(language)
        val session = loadSession() ?: return@withContext null
        val currentProfile = authenticatedCall(session) { it.me() }.toUserProfile(loadSession() ?: session)
        val updatedProfile = authenticatedCall {
            it.updateMePreferences(
                UserPreferencesUpdateRequest(
                    preferredLanguage = language,
                    timeZoneId = currentProfile.timeZoneId ?: java.util.TimeZone.getDefault().id,
                    novenaRemindersEnabled = currentProfile.novenaRemindersEnabled,
                    feastRemindersEnabled = currentProfile.feastRemindersEnabled,
                    emailUpdatesEnabled = currentProfile.emailUpdatesEnabled,
                    onboardingCompleted = currentProfile.onboardingCompleted
                )
            )
        }
        updatedProfile.toUserProfile(session).also {
            persistLanguage(it.preferredLanguage ?: language)
        }
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
        runApiCall { api.listSaints(lang = currentLanguage(), query = query.trim()) }
            .map {
                SaintSummary(
                    id = it.id,
                    slug = it.slug,
                    name = it.name,
                    feastLabel = it.feastLabel,
                    summary = it.summary,
                    imageUrl = it.imageUrl
                )
            }
    }

    suspend fun fetchSaintDetail(slug: String): SaintDetail = withContext(Dispatchers.IO) {
        runApiCall { api.getSaintDetail(slug = slug, lang = currentLanguage()) }
            .let {
                SaintDetail(
                    id = it.id,
                    slug = it.slug,
                    name = it.name,
                    feastLabel = it.feastLabel,
                    summary = it.summary,
                    biography = it.biography,
                    imageUrl = it.imageUrl,
                    intentions = it.intentions.orEmpty(),
                    sources = it.sources.orEmpty().map { source ->
                        SaintSource(
                            title = source.text ?: source.url ?: "Source",
                            url = source.url
                        )
                    }
                )
            }
    }

    suspend fun listSaintsByFeastDay(month: Int, day: Int): List<SaintSummary> = withContext(Dispatchers.IO) {
        runApiCall { api.listSaintsByDay(month = month, day = day, lang = currentLanguage()) }
            .map { it.toDomain() }
    }

    suspend fun listPrayers(query: String): List<PrayerSummary> = withContext(Dispatchers.IO) {
        runApiCall { api.listPrayers(lang = currentLanguage(), query = query.trim(), excludeCategory = "rosary") }
            .filterNot { it.category.equals("rosary", ignoreCase = true) }
            .map { prayer ->
                PrayerSummary(
                    id = prayer.id,
                    slug = prayer.slug,
                    title = prayer.title,
                    bodyPreview = prayer.bodyPreview,
                    category = prayer.category,
                    imageUrl = prayer.imageUrl
                )
            }
    }

    suspend fun listRosaries(query: String): List<PrayerSummary> = withContext(Dispatchers.IO) {
        runApiCall { api.listPrayers(lang = currentLanguage(), query = query.trim(), category = "rosary") }
            .filter { it.category.equals("rosary", ignoreCase = true) }
            .map { prayer ->
                PrayerSummary(
                    id = prayer.id,
                    slug = prayer.slug,
                    title = prayer.title,
                    bodyPreview = prayer.bodyPreview,
                    category = prayer.category,
                    imageUrl = prayer.imageUrl
                )
            }
    }

    suspend fun fetchPrayerDetail(slug: String): PrayerDetail = withContext(Dispatchers.IO) {
        runApiCall { api.getPrayerDetail(slug = slug, lang = currentLanguage()) }
            .let { prayer ->
                PrayerDetail(
                    id = prayer.id,
                    slug = prayer.slug,
                    title = prayer.title,
                    alternateTitle = prayer.alternateTitle,
                    body = prayer.body,
                    note = prayer.note,
                    category = prayer.category,
                    imageUrl = prayer.imageUrl,
                    sourceTitle = prayer.sourceTitle,
                    sourceType = prayer.sourceType,
                    tags = prayer.tags.orEmpty()
                )
            }
    }

    suspend fun listSaintsInRange(start: String, end: String): List<SaintDateGroup> = withContext(Dispatchers.IO) {
        runApiCall { api.listSaintsInRange(start = start, end = end, lang = currentLanguage()) }
            .map { group ->
                SaintDateGroup(
                    date = group.date,
                    saints = group.saints.map { it.toDomain() }
                )
            }
    }

    suspend fun listNovenas(query: String): List<NovenaSummary> = withContext(Dispatchers.IO) {
        runApiCall { api.listNovenas(lang = currentLanguage(), query = query.trim()) }
            .map {
                NovenaSummary(
                    id = it.id,
                    slug = it.slug,
                    title = it.title,
                    description = it.description,
                    durationDays = it.durationDays,
                    intentions = it.intentions.orEmpty(),
                    imageUrl = it.imageUrl
                )
            }
    }

    suspend fun fetchNovenaDetail(slug: String): NovenaDetail = withContext(Dispatchers.IO) {
        runApiCall { api.getNovenaDetail(slug = slug, lang = currentLanguage()) }
            .let {
                NovenaDetail(
                    id = it.id,
                    slug = it.slug,
                    title = it.title,
                    description = it.description,
                    durationDays = it.durationDays,
                    imageUrl = it.imageUrl,
                    tags = it.tags.orEmpty(),
                    intentions = it.intentions.orEmpty(),
                    days = it.days.orEmpty().map { day ->
                        NovenaDayDetail(
                            dayNumber = day.dayNumber,
                            title = day.title,
                            openingPrayer = day.openingPrayer,
                            meditation = day.meditation,
                            closingPrayer = day.closingPrayer,
                            scripture = day.scripture,
                            prayer = day.prayer,
                            reflection = day.reflection,
                            body = day.body
                        )
                    }
                )
            }
    }

    suspend fun listNovenaCommitments(): List<UserNovenaCommitment> = withContext(Dispatchers.IO) {
        authenticatedCall { it.listNovenaCommitments() }
            .mapNotNull { response ->
                val status = when (response.status.lowercase()) {
                    "active" -> CommitmentStatus.Active
                    "completed" -> CommitmentStatus.Completed
                    else -> null
                } ?: return@mapNotNull null

                UserNovenaCommitment(
                    novenaId = response.novenaId,
                    startedAt = response.startedAt,
                    currentDay = response.currentDay,
                    completedDays = response.completedDays.sorted(),
                    reminder = ReminderConfig(
                        enabled = response.reminderEnabled,
                        morningHour = response.reminderMorningHour,
                        eveningHour = response.reminderEveningHour,
                        timeZoneId = response.reminderTimeZoneId
                    ),
                    status = status,
                    updatedAt = response.updatedAt
                )
            }
    }

    suspend fun listFavorites(): List<UserFavorite> = withContext(Dispatchers.IO) {
        authenticatedCall { it.listFavorites() }
            .mapNotNull { response ->
                val itemType = when (response.itemType.lowercase()) {
                    "saint" -> FavoriteItemType.Saint
                    "novena" -> FavoriteItemType.Novena
                    "prayer" -> FavoriteItemType.Prayer
                    else -> null
                } ?: return@mapNotNull null

                UserFavorite(
                    itemType = itemType,
                    itemId = response.itemId,
                    createdAt = response.createdAt
                )
            }
    }

    suspend fun updateReminderPreferences(
        novenaEnabled: Boolean,
        dailyEnabled: Boolean
    ): UserProfile = withContext(Dispatchers.IO) {
        val session = loadSession() ?: throw SanctuaryApiException("Please sign in to continue.")
        val currentProfile = authenticatedCall(session) { it.me() }.toUserProfile(loadSession() ?: session)
        val updatedProfile = authenticatedCall {
            it.updateMePreferences(
                UserPreferencesUpdateRequest(
                    preferredLanguage = currentProfile.preferredLanguage ?: "en",
                    timeZoneId = currentProfile.timeZoneId ?: java.util.TimeZone.getDefault().id,
                    novenaRemindersEnabled = novenaEnabled,
                    feastRemindersEnabled = dailyEnabled,
                    emailUpdatesEnabled = currentProfile.emailUpdatesEnabled,
                    onboardingCompleted = currentProfile.onboardingCompleted
                )
            )
        }
        updatedProfile.toUserProfile(session)
    }

    suspend fun toggleFavorite(
        itemType: FavoriteItemType,
        itemId: String,
        enabled: Boolean
    ) = withContext(Dispatchers.IO) {
        if (enabled) {
            authenticatedCall { it.saveFavorite(itemType.name.lowercase(), itemId) }
        } else {
            authenticatedCall { it.deleteFavorite(itemType.name.lowercase(), itemId) }
        }
    }

    suspend fun startNovena(novenaId: String): UserNovenaCommitment = withContext(Dispatchers.IO) {
        val now = Instant.now().toString()
        saveNovenaCommitment(
            novenaId = novenaId,
            request = UserNovenaCommitmentRequest(
                startedAt = now,
                currentDay = 1,
                completedDays = emptyList(),
                reminderEnabled = false,
                reminderMorningHour = null,
                reminderEveningHour = null,
                reminderTimeZoneId = java.util.TimeZone.getDefault().id,
                status = "active"
            )
        )
    }

    suspend fun stopNovena(novenaId: String) = withContext(Dispatchers.IO) {
        authenticatedCall { it.deleteNovenaCommitment(novenaId) }
    }

    suspend fun completeCurrentNovenaDay(
        novenaId: String,
        totalDays: Int
    ): UserNovenaCommitment = withContext(Dispatchers.IO) {
        val existing = listNovenaCommitments()
            .firstOrNull { it.novenaId == novenaId && it.status == CommitmentStatus.Active }
            ?: throw SanctuaryApiException("No active novena was found to update.")

        val dayToComplete = existing.currentDay
        val completedDays = (existing.completedDays + dayToComplete).toSet().sorted()
        val reachedEnd = dayToComplete >= maxOf(1, totalDays)

        saveNovenaCommitment(
            novenaId = novenaId,
            request = UserNovenaCommitmentRequest(
                startedAt = existing.startedAt,
                currentDay = if (reachedEnd) maxOf(1, totalDays) else maxOf(existing.currentDay, dayToComplete + 1),
                completedDays = if (reachedEnd) (1..maxOf(1, totalDays)).toList() else completedDays,
                reminderEnabled = existing.reminder.enabled,
                reminderMorningHour = existing.reminder.morningHour,
                reminderEveningHour = existing.reminder.eveningHour,
                reminderTimeZoneId = existing.reminder.timeZoneId,
                status = if (reachedEnd) "completed" else "active"
            )
        )
    }

    suspend fun listNovenasByIntentions(query: String): List<NovenaSummary> = withContext(Dispatchers.IO) {
        runApiCall { api.listNovenasByIntentions(lang = currentLanguage(), query = query.trim()) }
            .map { it.toDomain() }
    }

    suspend fun searchIntentions(query: String): IntentionSearchResult = withContext(Dispatchers.IO) {
        runApiCall { api.searchIntentions(lang = currentLanguage(), query = query.trim()) }
            .toDomain()
    }

    suspend fun listNovenaCalendarRange(start: String, end: String): List<NovenaCalendarDate> = withContext(Dispatchers.IO) {
        runApiCall { api.listNovenasCalendarRange(start = start, end = end, lang = currentLanguage()) }
            .map { entry ->
                NovenaCalendarDate(
                    date = entry.date,
                    novenas = entry.novenas.map { it.toDomain() },
                    startingNovena = entry.startingNovena?.toDomain()
                )
            }
    }

    suspend fun listLiturgicalRange(start: String, end: String): List<LiturgicalDay> = withContext(Dispatchers.IO) {
        runApiCall { api.listLiturgicalRange(start = start, end = end) }
            .map { day ->
                LiturgicalDay(
                    date = day.date,
                    season = day.season,
                    primaryRank = day.primaryRank,
                    observances = day.observances,
                    readingsUrl = day.readingsUrl,
                    rankType = day.rankType
                )
            }
    }

    suspend fun fetchAskSanctuaryStatus(): AskSanctuaryStatusResponse = withContext(Dispatchers.IO) {
        authenticatedCall { it.getAskSanctuaryStatus() }
    }

    suspend fun acceptAskSanctuaryDisclaimer(): AskSanctuaryStatusResponse = withContext(Dispatchers.IO) {
        authenticatedCall { it.acceptAskSanctuaryDisclaimer() }
    }

    suspend fun askSanctuary(message: String, locale: String): AskSanctuaryResponse = withContext(Dispatchers.IO) {
        authenticatedCall {
            it.askSanctuary(AskSanctuaryRequest(message = message.trim(), locale = locale))
        }
    }

    fun currentSession(): StoredSession? = loadSession()

    fun logout() {
        clearSession()
    }

    suspend fun deleteAccount() = withContext(Dispatchers.IO) {
        try {
            authenticatedCall { it.deleteMe() }
        } catch (exception: SanctuaryApiException) {
            if (!exception.isSessionRejected()) {
                throw exception
            }
        }
        clearSession()
    }

    private suspend fun saveNovenaCommitment(
        novenaId: String,
        request: UserNovenaCommitmentRequest
    ): UserNovenaCommitment {
        return authenticatedCall { it.saveNovenaCommitment(novenaId, request) }
            .let { response ->
                UserNovenaCommitment(
                    novenaId = response.novenaId,
                    startedAt = response.startedAt,
                    currentDay = response.currentDay,
                    completedDays = response.completedDays.sorted(),
                    reminder = ReminderConfig(
                        enabled = response.reminderEnabled,
                        morningHour = response.reminderMorningHour,
                        eveningHour = response.reminderEveningHour,
                        timeZoneId = response.reminderTimeZoneId
                    ),
                    status = when (response.status.lowercase()) {
                        "completed" -> CommitmentStatus.Completed
                        else -> CommitmentStatus.Active
                    },
                    updatedAt = response.updatedAt
                )
            }
    }

    private fun loadSession(): StoredSession? {
        val raw = preferences.getString(sessionKey, null) ?: return null
        return ApiJson.decodeSession(raw)
    }

    private fun persistSession(session: StoredSession) {
        preferences.edit().putString(sessionKey, ApiJson.encodeSession(session)).apply()
    }

    private fun persistLanguage(language: String) {
        preferences.edit().putString(languageKey, language).apply()
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
            val resolvedSession = loadSession() ?: session
            val profile = authenticatedCall(resolvedSession) { it.me() }.toUserProfile(loadSession() ?: resolvedSession)
            persistLanguage(profile.preferredLanguage ?: currentLanguage())
            SessionBootstrapResult.authenticated(loadSession() ?: resolvedSession, profile)
        } catch (exception: Exception) {
            if (!session.refreshToken.isNullOrBlank()) {
                refreshSession(session.refreshToken, session)
            } else if (exception is SanctuaryApiException && exception.isSessionRejected()) {
                clearSession()
                SessionBootstrapResult.signedOut()
            } else {
                clearSession()
                SessionBootstrapResult.failed(exception.message ?: "Please sign in to continue.")
            }
        }
    }

    private fun authenticatedApi(session: StoredSession? = loadSession()): SanctuaryApiService {
        val activeSession = session ?: throw SanctuaryApiException("Please sign in to continue.")
        return SanctuaryApiFactory.create {
            activeSession.idToken.ifBlank { activeSession.accessToken }
        }
    }

    private suspend fun <T> authenticatedCall(
        session: StoredSession? = loadSession(),
        block: suspend (SanctuaryApiService) -> T
    ): T {
        var activeSession = session ?: throw SanctuaryApiException("Please sign in to continue.")
        if (activeSession.expiresAtMillis <= System.currentTimeMillis() + 60_000L) {
            val refreshToken = activeSession.refreshToken
            activeSession = if (refreshToken.isNullOrBlank()) {
                throw SanctuaryApiException("Please sign in to continue.", 401)
            } else {
                refreshStoredSession(refreshToken, activeSession)
                    ?: throw SanctuaryApiException("Please sign in to continue.", 401)
            }
        }

        return try {
            runApiCall { block(authenticatedApi(activeSession)) }
        } catch (exception: SanctuaryApiException) {
            val refreshToken = activeSession.refreshToken
            if (!exception.isSessionRejected() || refreshToken.isNullOrBlank()) {
                throw exception
            }

            val refreshed = refreshStoredSession(refreshToken, activeSession)
                ?: throw exception

            runApiCall { block(authenticatedApi(refreshed)) }
        }
    }

    private suspend fun refreshStoredSession(
        refreshToken: String,
        previous: StoredSession
    ): StoredSession? {
        return try {
            val response = runApiCall {
                api.refresh(AuthRefreshRequest(refreshToken = refreshToken))
            }
            response.toStoredSession(previous.refreshToken).also(::persistSession)
        } catch (_: Exception) {
            clearSession()
            null
        }
    }
}

private fun SaintSummaryResponse.toDomain(): SaintSummary = SaintSummary(
    id = id,
    slug = slug,
    name = name,
    feastLabel = feastLabel,
    summary = summary,
    imageUrl = imageUrl,
    intentions = intentions.orEmpty()
)

private fun NovenaSummaryResponse.toDomain(): NovenaSummary = NovenaSummary(
    id = id,
    slug = slug,
    title = title,
    description = description,
    durationDays = durationDays,
    intentions = intentions.orEmpty(),
    imageUrl = imageUrl
)

private fun IntentionSearchResultResponse.toDomain(): IntentionSearchResult = IntentionSearchResult(
    novenas = novenas.map { it.toDomain() },
    saints = saints.map { it.toDomain() }
)

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

private fun SanctuaryApiException.isSessionRejected(): Boolean {
    return statusCode == 401 || statusCode == 403 || statusCode == 404 || statusCode == 410
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
