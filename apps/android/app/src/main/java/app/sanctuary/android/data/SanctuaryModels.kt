package app.sanctuary.android.data

data class AuthRegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String
)

data class AuthLoginRequest(
    val email: String,
    val password: String
)

data class AuthRefreshRequest(
    val refreshToken: String
)

data class AuthConfirmRequest(
    val email: String,
    val code: String
)

data class AuthResendRequest(
    val email: String
)

data class AuthForgotPasswordRequest(
    val email: String
)

data class AuthResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)

data class AuthRegistrationResponse(
    val email: String,
    val displayName: String,
    val confirmationRequired: Boolean
)

data class AuthStatusResponse(
    val message: String
)

data class AuthSessionResponse(
    val accessToken: String,
    val idToken: String,
    val refreshToken: String?,
    val tokenType: String,
    val expiresIn: Int,
    val email: String,
    val displayName: String
)

data class UserProfileResponse(
    val userId: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val displayName: String?,
    val preferredLanguage: String?,
    val avatarUrl: String?,
    val timeZoneId: String?,
    val novenaRemindersEnabled: Boolean,
    val feastRemindersEnabled: Boolean,
    val emailUpdatesEnabled: Boolean,
    val onboardingCompleted: Boolean,
    val favoriteSaintCount: Int,
    val favoriteNovenaCount: Int,
    val favoritePrayerCount: Int,
    val activeNovenaCount: Int,
    val completedNovenaCount: Int
)

data class UserFavoriteResponse(
    val itemType: String,
    val itemId: String,
    val createdAt: String
)

data class UserNovenaCommitmentResponse(
    val novenaId: String,
    val startedAt: String,
    val currentDay: Int,
    val completedDays: List<Int>,
    val reminderEnabled: Boolean,
    val reminderMorningHour: Int?,
    val reminderEveningHour: Int?,
    val reminderTimeZoneId: String,
    val status: String,
    val updatedAt: String
)

data class UserNovenaCommitmentRequest(
    val startedAt: String,
    val currentDay: Int,
    val completedDays: List<Int>,
    val reminderEnabled: Boolean,
    val reminderMorningHour: Int?,
    val reminderEveningHour: Int?,
    val reminderTimeZoneId: String,
    val status: String
)

data class SaintSummaryResponse(
    val id: String,
    val slug: String,
    val name: String,
    val feastMonth: Int,
    val feastDay: Int,
    val feastLabel: String,
    val summary: String?,
    val imageUrl: String?
)

data class SaintDetailResponse(
    val id: String,
    val slug: String,
    val name: String,
    val feastMonth: Int,
    val feastDay: Int,
    val feastLabel: String,
    val summary: String?,
    val biography: String?,
    val imageUrl: String?,
    val sources: List<SaintSourceResponse>?
)

data class SaintSourceResponse(
    val text: String?,
    val url: String?
)

data class SaintDateGroupResponse(
    val date: String,
    val saints: List<SaintSummaryResponse>
)

data class PrayerSummaryResponse(
    val id: String,
    val slug: String,
    val title: String,
    val bodyPreview: String,
    val category: String,
    val imageUrl: String?
)

data class PrayerDetailResponse(
    val id: String,
    val slug: String,
    val title: String,
    val alternateTitle: String?,
    val body: String,
    val note: String?,
    val category: String,
    val imageUrl: String?,
    val sourceTitle: String?,
    val sourceType: String?,
    val tags: List<String>?
)

data class NovenaSummaryResponse(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val durationDays: Int,
    val intentions: List<String>?,
    val imageUrl: String?
)

data class NovenaDetailResponse(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val durationDays: Int,
    val imageUrl: String?,
    val tags: List<String>?,
    val intentions: List<String>?,
    val days: List<NovenaDayDetailResponse>?
)

data class NovenaDayDetailResponse(
    val dayNumber: Int,
    val title: String?,
    val openingPrayer: String?,
    val meditation: String?,
    val closingPrayer: String?,
    val scripture: String? = null,
    val prayer: String? = null,
    val reflection: String? = null,
    val body: String? = null
)

data class NovenaCalendarDateResponse(
    val date: String,
    val novenas: List<NovenaSummaryResponse>,
    val startingNovena: NovenaSummaryResponse?
)

data class LiturgicalDayResponse(
    val date: String,
    val season: String,
    val primaryRank: String,
    val observances: List<String>,
    val readingsUrl: String?,
    val rankType: String
)

data class ApiErrorEnvelope(
    val message: String?
)

data class StoredSession(
    val accessToken: String,
    val idToken: String,
    val refreshToken: String?,
    val tokenType: String,
    val expiresAtMillis: Long,
    val email: String,
    val displayName: String
)

data class UserProfile(
    val userId: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val displayName: String,
    val preferredLanguage: String?,
    val avatarUrl: String?,
    val timeZoneId: String?,
    val novenaRemindersEnabled: Boolean,
    val feastRemindersEnabled: Boolean,
    val emailUpdatesEnabled: Boolean,
    val onboardingCompleted: Boolean,
    val favoriteSaintCount: Int,
    val favoriteNovenaCount: Int,
    val favoritePrayerCount: Int,
    val activeNovenaCount: Int,
    val completedNovenaCount: Int
)

enum class CommitmentStatus {
    Active,
    Completed
}

enum class FavoriteItemType {
    Saint,
    Novena,
    Prayer
}

data class ReminderConfig(
    val enabled: Boolean,
    val morningHour: Int?,
    val eveningHour: Int?,
    val timeZoneId: String
)

data class UserNovenaCommitment(
    val novenaId: String,
    val startedAt: String,
    val currentDay: Int,
    val completedDays: List<Int>,
    val reminder: ReminderConfig,
    val status: CommitmentStatus,
    val updatedAt: String
)

data class UserFavorite(
    val itemType: FavoriteItemType,
    val itemId: String,
    val createdAt: String
)

data class SaintSummary(
    val id: String,
    val slug: String,
    val name: String,
    val feastLabel: String,
    val summary: String?,
    val imageUrl: String?
)

data class SaintDetail(
    val id: String,
    val slug: String,
    val name: String,
    val feastLabel: String,
    val summary: String?,
    val biography: String?,
    val imageUrl: String?,
    val sources: List<SaintSource>
)

data class SaintSource(
    val title: String,
    val url: String?
)

data class SaintDateGroup(
    val date: String,
    val saints: List<SaintSummary>
)

data class PrayerSummary(
    val id: String,
    val slug: String,
    val title: String,
    val bodyPreview: String,
    val category: String,
    val imageUrl: String?
)

data class PrayerDetail(
    val id: String,
    val slug: String,
    val title: String,
    val alternateTitle: String?,
    val body: String,
    val note: String?,
    val category: String,
    val imageUrl: String?,
    val sourceTitle: String?,
    val sourceType: String?,
    val tags: List<String>
)

data class NovenaSummary(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val durationDays: Int,
    val intentions: List<String>,
    val imageUrl: String?
)

data class NovenaDetail(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val durationDays: Int,
    val imageUrl: String?,
    val tags: List<String>,
    val intentions: List<String>,
    val days: List<NovenaDayDetail>
)

data class NovenaDayDetail(
    val dayNumber: Int,
    val title: String?,
    val openingPrayer: String?,
    val meditation: String?,
    val closingPrayer: String?,
    val scripture: String?,
    val prayer: String?,
    val reflection: String?,
    val body: String?
)

data class NovenaCalendarDate(
    val date: String,
    val novenas: List<NovenaSummary>,
    val startingNovena: NovenaSummary?
)

data class LiturgicalDay(
    val date: String,
    val season: String,
    val primaryRank: String,
    val observances: List<String>,
    val readingsUrl: String?,
    val rankType: String
)
