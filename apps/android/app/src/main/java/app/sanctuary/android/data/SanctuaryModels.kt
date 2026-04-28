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

data class NovenaSummaryResponse(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val durationDays: Int,
    val intentions: List<String>?,
    val imageUrl: String?
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

data class SaintSummary(
    val id: String,
    val slug: String,
    val name: String,
    val feastLabel: String,
    val summary: String?
)

data class NovenaSummary(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val durationDays: Int,
    val intentions: List<String>
)
