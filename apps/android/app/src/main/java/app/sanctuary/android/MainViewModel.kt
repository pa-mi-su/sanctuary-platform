package app.sanctuary.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.sanctuary.android.data.AuthRegistrationResponse
import app.sanctuary.android.data.IntentionSearchResult
import app.sanctuary.android.data.LiturgicalDay
import app.sanctuary.android.data.NovenaCalendarDate
import app.sanctuary.android.data.NovenaDayDetail
import app.sanctuary.android.data.NovenaDetail
import app.sanctuary.android.data.NovenaSummary
import app.sanctuary.android.data.PrayerDetail
import app.sanctuary.android.data.PrayerSummary
import app.sanctuary.android.data.SaintDateGroup
import app.sanctuary.android.data.SaintDetail
import app.sanctuary.android.data.SaintSummary
import app.sanctuary.android.data.SanctuaryApiFactory
import app.sanctuary.android.data.SessionBootstrapResult
import app.sanctuary.android.data.SessionRepository
import app.sanctuary.android.data.StoredSession
import app.sanctuary.android.data.CommitmentStatus
import app.sanctuary.android.data.FavoriteItemType
import app.sanctuary.android.data.UserFavorite
import app.sanctuary.android.data.UserNovenaCommitment
import app.sanctuary.android.data.UserProfile
import app.sanctuary.android.ui.AppLanguage
import app.sanctuary.android.ui.SanctuaryStrings
import java.text.Normalizer
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SessionStatus {
    SignedOut,
    Loading,
    AwaitingConfirmation,
    Authenticated,
    Failed
}

data class SessionUiState(
    val status: SessionStatus = SessionStatus.Loading,
    val isBootstrapping: Boolean = false,
    val isSavingReminderPreferences: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val session: StoredSession? = null,
    val profile: UserProfile? = null,
    val pendingConfirmationEmail: String? = null,
    val pendingPasswordResetEmail: String? = null,
    val message: String? = null,
    val isErrorMessage: Boolean = false
)

data class ContentListUiState<T>(
    val allItems: List<T> = emptyList(),
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val query: String = "",
    val error: String? = null
)

private data class SearchDocument(
    val itemId: String,
    val primaryText: String,
    val secondaryText: String = "",
    val auxiliaryText: String = ""
) {
    val normalizedPrimary: String = normalizeSearchText(primaryText)
    val normalizedSecondary: String = normalizeSearchText(secondaryText)
    val normalizedAuxiliary: String = normalizeSearchText(auxiliaryText)
    val primaryTokens: List<String> = normalizedPrimary.searchTokens()
    val secondaryTokens: List<String> = normalizedSecondary.searchTokens()
    val auxiliaryTokens: List<String> = normalizedAuxiliary.searchTokens()
}

data class IntentionSearchUiState(
    val result: IntentionSearchResult = IntentionSearchResult(novenas = emptyList(), saints = emptyList()),
    val isLoading: Boolean = false,
    val query: String = "",
    val error: String? = null
)

data class ContentDetailUiState<T>(
    val item: T? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class NovenaProgressUiState(
    val commitments: List<UserNovenaCommitment> = emptyList(),
    val favorites: List<UserFavorite> = emptyList(),
    val saintNames: Map<String, String> = emptyMap(),
    val saintSlugs: Map<String, String> = emptyMap(),
    val novenaTitles: Map<String, String> = emptyMap(),
    val novenaDurations: Map<String, Int> = emptyMap(),
    val prayerTitles: Map<String, String> = emptyMap(),
    val prayerSlugs: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val reminderScheduler = AndroidReminderScheduler(application.applicationContext)
    private val repository = SessionRepository(
        context = application.applicationContext,
        api = SanctuaryApiFactory.create {
            _session.value.session?.idToken ?: _session.value.session?.accessToken
        }
    )
    private val pendingFavoriteToggles = mutableSetOf<String>()

    private val _session = MutableStateFlow(
        SessionUiState(
            status = SessionStatus.Loading,
            isBootstrapping = true
        )
    )
    val session: StateFlow<SessionUiState> = _session.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.fromCode(repository.currentLanguage()))
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private fun l10n() = SanctuaryStrings(_appLanguage.value)

    private val _saints = MutableStateFlow(ContentListUiState<SaintSummary>())
    val saints: StateFlow<ContentListUiState<SaintSummary>> = _saints.asStateFlow()

    private val _novenas = MutableStateFlow(ContentListUiState<NovenaSummary>())
    val novenas: StateFlow<ContentListUiState<NovenaSummary>> = _novenas.asStateFlow()

    private val _intentions = MutableStateFlow(IntentionSearchUiState())
    val intentions: StateFlow<IntentionSearchUiState> = _intentions.asStateFlow()

    private val _prayers = MutableStateFlow(ContentListUiState<PrayerSummary>())
    val prayers: StateFlow<ContentListUiState<PrayerSummary>> = _prayers.asStateFlow()

    private val _rosaries = MutableStateFlow(ContentListUiState<PrayerSummary>())
    val rosaries: StateFlow<ContentListUiState<PrayerSummary>> = _rosaries.asStateFlow()

    private val _saintDetail = MutableStateFlow(ContentDetailUiState<SaintDetail>())
    val saintDetail: StateFlow<ContentDetailUiState<SaintDetail>> = _saintDetail.asStateFlow()

    private val _novenaDetail = MutableStateFlow(ContentDetailUiState<NovenaDetail>())
    val novenaDetail: StateFlow<ContentDetailUiState<NovenaDetail>> = _novenaDetail.asStateFlow()

    private val _prayerDetail = MutableStateFlow(ContentDetailUiState<PrayerDetail>())
    val prayerDetail: StateFlow<ContentDetailUiState<PrayerDetail>> = _prayerDetail.asStateFlow()

    private val _novenaProgress = MutableStateFlow(NovenaProgressUiState())
    val novenaProgress: StateFlow<NovenaProgressUiState> = _novenaProgress.asStateFlow()

    init {
        bootstrap()
    }

    fun bootstrap() {
        viewModelScope.launch {
            _session.update {
                it.copy(
                    status = SessionStatus.Loading,
                    isBootstrapping = true,
                    message = null,
                    isErrorMessage = false
                )
            }
            val result = runCatching {
                withTimeoutOrNull(10_000) { repository.bootstrap() }
                    ?: SessionBootstrapResult.signedOut()
            }.getOrElse { failure ->
                SessionBootstrapResult.signedOut()
            }
            if (result.authenticated) {
                _appLanguage.value = AppLanguage.fromCode(result.profile?.preferredLanguage ?: repository.currentLanguage())
                _session.value = SessionUiState(
                    status = SessionStatus.Authenticated,
                    isBootstrapping = false,
                    isSavingReminderPreferences = false,
                    session = result.session,
                    profile = result.profile
                )
                syncReminderScheduler(
                    profile = result.profile,
                    activeCommitmentCount = 0
                )
                loadInitialContent()
                refreshNovenaProgress()
            } else {
                reminderScheduler.cancelAll()
                _appLanguage.value = AppLanguage.fromCode(repository.currentLanguage())
                _session.value = SessionUiState(
                    status = SessionStatus.SignedOut,
                    isBootstrapping = false,
                    isSavingReminderPreferences = false
                )
                loadInitialContent()
                _novenaProgress.value = NovenaProgressUiState()
            }
        }
    }

    fun register(firstName: String, lastName: String, email: String, password: String) {
        viewModelScope.launch {
            setBusy()
            runCatching {
                withTimeoutOrNull(15_000) {
                    repository.register(firstName, lastName, email, password)
                } ?: throw IllegalStateException(l10n().t("status.registerTimeout"))
            }.onSuccess { response: AuthRegistrationResponse ->
                _session.value = SessionUiState(
                    status = SessionStatus.AwaitingConfirmation,
                    pendingConfirmationEmail = response.email,
                    message = "${l10n().t("auth.confirmationSentPrefix")} ${response.email}.",
                    isErrorMessage = false
                )
            }.onFailure { failure ->
                _session.value = SessionUiState(
                    status = SessionStatus.Failed,
                    pendingConfirmationEmail = email,
                    message = failure.message,
                    isErrorMessage = true
                )
            }
        }
    }

    fun confirmRegistration(code: String, password: String? = null) {
        val email = _session.value.pendingConfirmationEmail ?: return
        viewModelScope.launch {
            setBusy()
            var confirmationMessage: String? = null
            runCatching {
                val confirmation = withTimeoutOrNull(15_000) {
                    repository.confirm(email = email, code = code)
                } ?: throw IllegalStateException(l10n().t("status.confirmTimeout"))
                confirmationMessage = confirmation.message

                if (!password.isNullOrEmpty()) {
                    withTimeoutOrNull(15_000) {
                        repository.login(email, password)
                    } ?: throw IllegalStateException(l10n().t("status.loginTimeout"))
                } else {
                    null
                }
            }.onSuccess { loginResult ->
                if (loginResult?.authenticated == true) {
                    applyAuthenticatedSession(loginResult)
                } else {
                    _session.value = _session.value.copy(
                        status = SessionStatus.SignedOut,
                        pendingConfirmationEmail = email,
                        message = confirmationMessage,
                        isErrorMessage = false
                    )
                }
            }.onFailure { failure ->
                _session.value = _session.value.copy(
                    status = SessionStatus.Failed,
                    message = failure.message,
                    isErrorMessage = true
                )
            }
        }
    }

    fun resendConfirmation() {
        val email = _session.value.pendingConfirmationEmail ?: return
        viewModelScope.launch {
            setBusy()
            runCatching {
                withTimeoutOrNull(15_000) {
                    repository.resendConfirmation(email)
                } ?: throw IllegalStateException(l10n().t("status.resendTimeout"))
            }
                .onSuccess { response ->
                    _session.value = _session.value.copy(
                        status = SessionStatus.AwaitingConfirmation,
                        message = response.message,
                        isErrorMessage = false
                    )
                }.onFailure { failure ->
                    _session.value = _session.value.copy(
                        status = SessionStatus.Failed,
                        message = failure.message,
                        isErrorMessage = true
                    )
                }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            setBusy()
            runCatching {
                withTimeoutOrNull(15_000) {
                    repository.forgotPassword(email)
                } ?: throw IllegalStateException(l10n().t("status.forgotTimeout"))
            }
                .onSuccess { response ->
                    _session.value = SessionUiState(
                        status = SessionStatus.SignedOut,
                        pendingPasswordResetEmail = email,
                        message = response.message,
                        isErrorMessage = false
                    )
                }.onFailure { failure ->
                    _session.value = SessionUiState(
                        status = SessionStatus.Failed,
                        pendingPasswordResetEmail = email,
                        message = failure.message,
                        isErrorMessage = true
                    )
                }
        }
    }

    fun resetPassword(email: String, code: String, newPassword: String) {
        viewModelScope.launch {
            setBusy()
            runCatching {
                withTimeoutOrNull(15_000) {
                    repository.resetPassword(email, code, newPassword)
                } ?: throw IllegalStateException(l10n().t("status.resetTimeout"))
            }
                .onSuccess { result ->
                    if (result.authenticated) {
                        applyAuthenticatedSession(result)
                    } else {
                        _session.value = SessionUiState(
                            status = SessionStatus.Failed,
                            pendingPasswordResetEmail = email,
                            message = result.errorMessage ?: l10n().t("status.loginFailed"),
                            isErrorMessage = true
                        )
                    }
                }.onFailure { failure ->
                    _session.value = SessionUiState(
                        status = SessionStatus.Failed,
                        pendingPasswordResetEmail = email,
                        message = failure.message,
                        isErrorMessage = true
                    )
                }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            setBusy()
            runCatching {
                withTimeoutOrNull(15_000) {
                    repository.login(email, password)
                } ?: throw IllegalStateException(l10n().t("status.loginTimeout"))
            }
                .onSuccess { result ->
                    if (result.authenticated) {
                        applyAuthenticatedSession(result)
                    } else {
                        reminderScheduler.cancelAll()
                        _session.value = SessionUiState(
                            status = SessionStatus.Failed,
                            message = result.errorMessage ?: l10n().t("status.loginFailed"),
                            isErrorMessage = true
                        )
                    }
                }.onFailure { failure ->
                    _session.value = SessionUiState(
                        status = SessionStatus.Failed,
                        message = failure.message,
                        isErrorMessage = true
                    )
                }
        }
    }

    fun logout() {
        repository.logout()
        reminderScheduler.cancelAll()
        _session.value = SessionUiState(status = SessionStatus.SignedOut, isSavingReminderPreferences = false)
        _novenaProgress.value = NovenaProgressUiState()
        loadInitialContent()
    }

    fun deleteAccount() {
        val current = _session.value
        if (current.status != SessionStatus.Authenticated || current.isDeletingAccount) return

        viewModelScope.launch {
            _session.update {
                it.copy(
                    isDeletingAccount = true,
                    message = null,
                    isErrorMessage = false
                )
            }

            runCatching {
                withTimeoutOrNull(20_000) {
                    repository.deleteAccount()
                } ?: throw IllegalStateException(l10n().t("status.deleteAccountTimeout"))
            }.onSuccess {
                reminderScheduler.cancelAll()
                _session.value = SessionUiState(status = SessionStatus.SignedOut)
                _novenaProgress.value = NovenaProgressUiState()
                loadInitialContent()
            }.onFailure { failure ->
                _session.update {
                    it.copy(
                        status = SessionStatus.Authenticated,
                        isDeletingAccount = false,
                        message = failure.message ?: l10n().t("status.deleteAccountFailed"),
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    private fun applyAuthenticatedSession(result: SessionBootstrapResult) {
        _session.value = SessionUiState(
            status = SessionStatus.Authenticated,
            isBootstrapping = false,
            isSavingReminderPreferences = false,
            session = result.session,
            profile = result.profile
        )
        syncReminderScheduler(
            profile = result.profile,
            activeCommitmentCount = 0
        )
        loadInitialContent()
        refreshNovenaProgress()
    }

    fun updateLanguage(language: AppLanguage) {
        if (_appLanguage.value == language) return
        viewModelScope.launch {
            _appLanguage.value = language
            runCatching { repository.updatePreferredLanguage(language.code) }
                .onSuccess { updatedProfile ->
                    if (updatedProfile != null) {
                        syncReminderScheduler(
                            profile = updatedProfile,
                            activeCommitmentCount = _novenaProgress.value.commitments.count { it.status == CommitmentStatus.Active }
                        )
                        _session.update {
                            it.copy(
                                status = SessionStatus.Authenticated,
                                profile = updatedProfile,
                                message = null,
                                isErrorMessage = false
                            )
                        }
                    }
                }
            loadInitialContent()
            refreshNovenaProgress()
        }
    }

    fun updateReminderPreferences(novenaEnabled: Boolean, dailyEnabled: Boolean) {
        val previousProfile = _session.value.profile ?: return
        if (_session.value.status != SessionStatus.Authenticated) return

        viewModelScope.launch {
            _session.update {
                it.copy(
                    isSavingReminderPreferences = true,
                    profile = previousProfile.copy(
                        novenaRemindersEnabled = novenaEnabled,
                        feastRemindersEnabled = dailyEnabled
                    ),
                    message = null,
                    isErrorMessage = false
                )
            }

            runCatching {
                repository.updateReminderPreferences(
                    novenaEnabled = novenaEnabled,
                    dailyEnabled = dailyEnabled
                )
            }.onSuccess { updatedProfile ->
                syncReminderScheduler(
                    profile = updatedProfile,
                    activeCommitmentCount = _novenaProgress.value.commitments.count { it.status == CommitmentStatus.Active }
                )
                _session.update {
                    it.copy(
                        status = SessionStatus.Authenticated,
                        isSavingReminderPreferences = false,
                        profile = updatedProfile,
                        message = if (novenaEnabled || dailyEnabled) {
                            l10n().t("status.remindersUpdated")
                        } else {
                            l10n().t("status.remindersOff")
                        },
                        isErrorMessage = false
                    )
                }
            }.onFailure { failure ->
                _session.update {
                    it.copy(
                        status = SessionStatus.Authenticated,
                        isSavingReminderPreferences = false,
                        profile = previousProfile,
                        message = failure.message,
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun updateSaintQuery(query: String) {
        _saints.update { it.copy(query = query) }
        filterSaints()
    }

    fun updateNovenaQuery(query: String) {
        _novenas.update { it.copy(query = query) }
        filterNovenas()
    }

    fun updatePrayerQuery(query: String) {
        _prayers.update { it.copy(query = query) }
        filterPrayers()
    }

    fun updateRosaryQuery(query: String) {
        _rosaries.update { it.copy(query = query) }
        filterRosaries()
    }

    fun updateIntentionsQuery(query: String) {
        _intentions.update { it.copy(query = query) }
    }

    fun loadSaints() {
        if (_saints.value.allItems.isNotEmpty()) {
            filterSaints()
            return
        }
        viewModelScope.launch {
            _saints.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.listSaints("")
            }.onSuccess { items ->
                _saints.value = _saints.value.copy(allItems = items, isLoading = false, error = null)
                filterSaints()
            }.onFailure { failure ->
                _saints.value = _saints.value.copy(isLoading = false, error = failure.message)
            }
        }
    }

    fun loadNovenas() {
        if (_novenas.value.allItems.isNotEmpty()) {
            filterNovenas()
            return
        }
        viewModelScope.launch {
            _novenas.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.listNovenas("")
            }.onSuccess { items ->
                _novenas.value = _novenas.value.copy(allItems = items, isLoading = false, error = null)
                filterNovenas()
            }.onFailure { failure ->
                _novenas.value = _novenas.value.copy(isLoading = false, error = failure.message)
            }
        }
    }

    fun loadPrayers() {
        if (_prayers.value.allItems.isNotEmpty()) {
            filterPrayers()
            return
        }
        viewModelScope.launch {
            _prayers.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.listPrayers("")
            }.onSuccess { items ->
                _prayers.value = _prayers.value.copy(allItems = items, isLoading = false, error = null)
                filterPrayers()
            }.onFailure { failure ->
                _prayers.value = _prayers.value.copy(isLoading = false, error = failure.message)
            }
        }
    }

    fun loadRosaries() {
        if (_rosaries.value.allItems.isNotEmpty()) {
            filterRosaries()
            return
        }
        viewModelScope.launch {
            _rosaries.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.listRosaries("")
            }.onSuccess { items ->
                _rosaries.value = _rosaries.value.copy(allItems = items, isLoading = false, error = null)
                filterRosaries()
            }.onFailure { failure ->
                _rosaries.value = _rosaries.value.copy(isLoading = false, error = failure.message)
            }
        }
    }

    fun loadIntentions() {
        viewModelScope.launch {
            _intentions.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.searchIntentions(_intentions.value.query)
            }.onSuccess { result ->
                _intentions.value = _intentions.value.copy(result = result, isLoading = false, error = null)
            }.onFailure { failure ->
                _intentions.value = _intentions.value.copy(isLoading = false, error = failure.message)
            }
        }
    }

    private fun filterSaints() {
        _saints.update { state ->
            state.copy(
                items = rankSearchResults(
                    query = state.query,
                    items = state.allItems,
                    document = { saint ->
                        SearchDocument(
                            itemId = saint.id,
                            primaryText = saint.name,
                            secondaryText = saint.slug,
                            auxiliaryText = listOfNotNull(saint.summary).joinToString(" ")
                        )
                    }
                )
            )
        }
    }

    private fun filterNovenas() {
        _novenas.update { state ->
            state.copy(
                items = rankSearchResults(
                    query = state.query,
                    items = state.allItems,
                    document = { novena ->
                        SearchDocument(
                            itemId = novena.id,
                            primaryText = novena.title,
                            secondaryText = novena.slug,
                            auxiliaryText = novena.description
                        )
                    }
                )
            )
        }
    }

    private fun filterPrayers() {
        _prayers.update { state ->
            state.copy(
                items = rankSearchResults(
                    query = state.query,
                    items = state.allItems,
                    document = { prayer ->
                        SearchDocument(
                            itemId = prayer.id,
                            primaryText = prayer.title,
                            secondaryText = "${prayer.category} ${prayer.slug}",
                            auxiliaryText = prayer.bodyPreview
                        )
                    }
                )
            )
        }
    }

    private fun filterRosaries() {
        _rosaries.update { state ->
            state.copy(
                items = rankSearchResults(
                    query = state.query,
                    items = state.allItems,
                    document = { prayer ->
                        SearchDocument(
                            itemId = prayer.id,
                            primaryText = prayer.title,
                            secondaryText = "${prayer.category} ${prayer.slug}",
                            auxiliaryText = prayer.bodyPreview
                        )
                    }
                )
            )
        }
    }

    fun openSaint(slug: String) {
        viewModelScope.launch {
            _saintDetail.value = ContentDetailUiState(isLoading = true)
            runCatching {
                repository.fetchSaintDetail(slug)
            }.onSuccess { detail ->
                _saintDetail.value = ContentDetailUiState(item = detail)
            }.onFailure { failure ->
                _saintDetail.value = ContentDetailUiState(error = failure.message)
            }
        }
    }

    fun closeSaintDetail() {
        _saintDetail.value = ContentDetailUiState()
    }

    fun openNovena(slug: String) {
        viewModelScope.launch {
            _novenaDetail.value = ContentDetailUiState(isLoading = true)
            runCatching {
                repository.fetchNovenaDetail(slug)
            }.onSuccess { detail ->
                _novenaDetail.value = ContentDetailUiState(item = detail)
            }.onFailure { failure ->
                _novenaDetail.value = ContentDetailUiState(error = failure.message)
            }
        }
    }

    fun closeNovenaDetail() {
        _novenaDetail.value = ContentDetailUiState()
    }

    fun refreshNovenaProgress() {
        if (_session.value.status != SessionStatus.Authenticated) {
            reminderScheduler.cancelAll()
            _novenaProgress.value = NovenaProgressUiState()
            return
        }

        viewModelScope.launch {
            _novenaProgress.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.listNovenaCommitments() }
                .onSuccess { commitments ->
                    runCatching { repository.listFavorites() }
                        .onSuccess { favorites ->
                            val novenaIds = (commitments.map { it.novenaId } + favorites.filter { it.itemType == FavoriteItemType.Novena }.map { it.itemId })
                                .distinct()
                            val saintIds = favorites.filter { it.itemType == FavoriteItemType.Saint }.map { it.itemId }.distinct()
                            val prayerIds = favorites.filter { it.itemType == FavoriteItemType.Prayer }.map { it.itemId }.distinct()

                            val novenaDetails = novenaIds.associateWith { id ->
                                runCatching { repository.fetchNovenaDetail(id) }.getOrNull()
                            }
                            val prayerDetails = prayerIds.associateWith { id ->
                                runCatching { repository.fetchPrayerDetail(id) }.getOrNull()
                            }
                            val allSaints = runCatching { repository.listSaints("") }.getOrNull().orEmpty()
                            val saintDetails = saintIds.associateWith { id ->
                                runCatching { repository.fetchSaintDetail(id) }.getOrNull()
                            }
                            val saintNames = saintIds.associateWith { id ->
                                saintDetails[id]?.name
                                    ?: allSaints.firstOrNull { it.id == id || it.slug == id }?.name
                                    ?: formatFavoriteSaintLabel(id)
                            }
                            val saintSlugs = saintIds.associateWith { id ->
                                saintDetails[id]?.slug
                                    ?: allSaints.firstOrNull { it.id == id || it.slug == id }?.slug
                                    ?: id
                            }
                            val activeCommitmentCount = commitments.count { it.status == CommitmentStatus.Active }
                            syncReminderScheduler(
                                profile = _session.value.profile,
                                activeCommitmentCount = activeCommitmentCount
                            )

                            _novenaProgress.value = NovenaProgressUiState(
                                commitments = commitments,
                                favorites = favorites,
                                saintNames = saintNames,
                                saintSlugs = saintSlugs,
                                novenaTitles = novenaDetails.mapNotNull { (id, detail) -> detail?.let { id to it.title } }.toMap(),
                                novenaDurations = novenaDetails.mapNotNull { (id, detail) -> detail?.let { id to it.durationDays } }.toMap(),
                                prayerTitles = prayerDetails.mapNotNull { (id, detail) -> detail?.let { id to it.title } }.toMap(),
                                prayerSlugs = prayerDetails.mapNotNull { (id, detail) -> detail?.let { id to it.slug } }.toMap(),
                                isLoading = false
                            )
                        }.onFailure { failure ->
                            _novenaProgress.value = NovenaProgressUiState(
                                commitments = commitments,
                                favorites = emptyList(),
                                saintNames = emptyMap(),
                                saintSlugs = emptyMap(),
                                novenaTitles = emptyMap(),
                                novenaDurations = emptyMap(),
                                prayerTitles = emptyMap(),
                                prayerSlugs = emptyMap(),
                                isLoading = false,
                                error = failure.message
                            )
                        }
                }.onFailure { failure ->
                    _novenaProgress.value = NovenaProgressUiState(
                        commitments = emptyList(),
                        favorites = emptyList(),
                        saintNames = emptyMap(),
                        saintSlugs = emptyMap(),
                        novenaTitles = emptyMap(),
                        novenaDurations = emptyMap(),
                        prayerTitles = emptyMap(),
                        prayerSlugs = emptyMap(),
                        isLoading = false,
                        error = failure.message
                    )
                }
        }
    }

    fun startNovena(novenaId: String) {
        viewModelScope.launch {
            runCatching { repository.startNovena(novenaId) }
                .onSuccess {
                    refreshNovenaProgress()
                }.onFailure { failure ->
                    _novenaProgress.update { it.copy(error = failure.message) }
                }
        }
    }

    fun stopNovena(novenaId: String) {
        viewModelScope.launch {
            runCatching { repository.stopNovena(novenaId) }
                .onSuccess {
                    refreshNovenaProgress()
                }.onFailure { failure ->
                    _novenaProgress.update { it.copy(error = failure.message) }
                }
        }
    }

    fun completeNovenaDay(novenaId: String, totalDays: Int) {
        viewModelScope.launch {
            runCatching { repository.completeCurrentNovenaDay(novenaId, totalDays) }
                .onSuccess {
                    refreshNovenaProgress()
                }.onFailure { failure ->
                    _novenaProgress.update { it.copy(error = failure.message) }
                }
        }
    }

    fun toggleFavorite(itemType: FavoriteItemType, itemId: String) {
        if (_session.value.status != SessionStatus.Authenticated) return
        val mutationKey = "${itemType.name}:$itemId"
        if (!pendingFavoriteToggles.add(mutationKey)) {
            return
        }
        val previousProgress = _novenaProgress.value
        val previousProfile = _session.value.profile
        val currentlyFavorite = previousProgress.favorites.any { it.itemType == itemType && it.itemId == itemId }
        val nextFavorite = !currentlyFavorite

        _novenaProgress.update { progress ->
            val favorites = if (nextFavorite) {
                progress.favorites + UserFavorite(itemType, itemId, Instant.now().toString())
            } else {
                progress.favorites.filterNot { it.itemType == itemType && it.itemId == itemId }
            }
            progress.copy(favorites = favorites, error = null)
        }
        updateFavoriteCount(itemType, if (nextFavorite) 1 else -1)

        viewModelScope.launch {
            runCatching {
                repository.toggleFavorite(
                    itemType = itemType,
                    itemId = itemId,
                    enabled = nextFavorite
                )
            }.onSuccess {
                pendingFavoriteToggles.remove(mutationKey)
            }.onFailure { failure ->
                pendingFavoriteToggles.remove(mutationKey)
                _novenaProgress.value = previousProgress.copy(error = failure.message)
                _session.update { it.copy(profile = previousProfile) }
            }
        }
    }

    private fun updateFavoriteCount(itemType: FavoriteItemType, delta: Int) {
        _session.update { sessionState ->
            val profile = sessionState.profile ?: return@update sessionState
            val updatedProfile = when (itemType) {
                FavoriteItemType.Saint -> profile.copy(favoriteSaintCount = (profile.favoriteSaintCount + delta).coerceAtLeast(0))
                FavoriteItemType.Novena -> profile.copy(favoriteNovenaCount = (profile.favoriteNovenaCount + delta).coerceAtLeast(0))
                FavoriteItemType.Prayer -> profile.copy(favoritePrayerCount = (profile.favoritePrayerCount + delta).coerceAtLeast(0))
            }
            sessionState.copy(profile = updatedProfile)
        }
    }

    fun openPrayer(slug: String) {
        viewModelScope.launch {
            _prayerDetail.value = ContentDetailUiState(isLoading = true)
            runCatching {
                repository.fetchPrayerDetail(slug)
            }.onSuccess { detail ->
                _prayerDetail.value = ContentDetailUiState(item = detail)
            }.onFailure { failure ->
                _prayerDetail.value = ContentDetailUiState(error = failure.message)
            }
        }
    }

    fun closePrayerDetail() {
        _prayerDetail.value = ContentDetailUiState()
    }

    private fun loadInitialContent() {
        loadSaints()
        loadNovenas()
        loadPrayers()
        loadRosaries()
        loadIntentions()
    }

    private fun syncReminderScheduler(profile: UserProfile?, activeCommitmentCount: Int) {
        if (profile == null) {
            reminderScheduler.cancelAll()
            return
        }
        reminderScheduler.syncDigestReminder(
            activeCommitmentCount = activeCommitmentCount,
            novenaEnabled = profile.novenaRemindersEnabled,
            generalDailyEnabled = profile.feastRemindersEnabled
        )
    }

    private fun setBusy() {
        _session.update {
            it.copy(
                status = SessionStatus.Loading,
                isBootstrapping = false,
                isSavingReminderPreferences = false,
                message = null,
                isErrorMessage = false
            )
        }
    }

    suspend fun fetchSaintsByFeastDay(month: Int, day: Int): List<SaintSummary> {
        return repository.listSaintsByFeastDay(month, day)
    }

    suspend fun fetchSaintsInRange(start: String, end: String): List<SaintDateGroup> {
        return repository.listSaintsInRange(start, end)
    }

    suspend fun fetchNovenasInRange(start: String, end: String): List<NovenaCalendarDate> {
        return repository.listNovenaCalendarRange(start, end)
    }

    suspend fun fetchLiturgicalRange(start: String, end: String): List<LiturgicalDay> {
        return repository.listLiturgicalRange(start, end)
    }

    private fun formatFavoriteSaintLabel(id: String): String {
        val trimmed = id.trim()
        if (trimmed.isBlank()) return id
        val rawTokens = trimmed.split("_")
        val nameTokens = if (rawTokens.size >= 4 && rawTokens[2].equals("saint", ignoreCase = true)) {
            listOf("Saint") + rawTokens.drop(3)
        } else {
            trimmed.replace("-", " ").replace("_", " ").split(" ")
        }

        return nameTokens
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.lowercase().replaceFirstChar { char -> char.uppercase() }
            }
    }
}

private fun <T> rankSearchResults(
    query: String,
    items: List<T>,
    document: (T) -> SearchDocument
): List<T> {
    val normalizedQuery = normalizeSearchText(query)
    if (normalizedQuery.isBlank()) return items

    val queryTokens = normalizedQuery.searchTokens()
    if (queryTokens.isEmpty()) return items

    return items
        .mapNotNull { item ->
            val doc = document(item)
            val score = scoreSearchDocument(doc, normalizedQuery, queryTokens) ?: return@mapNotNull null
            item to score
        }
        .sortedWith(
            compareByDescending<Pair<T, Int>> { it.second }
                .thenBy { document(it.first).itemId }
        )
        .map { it.first }
}

private fun scoreSearchDocument(
    document: SearchDocument,
    query: String,
    queryTokens: List<String>
): Int? {
    if (!queryTokens.all { token -> tokenMatchesDocument(token, document) }) return null

    var score = 0
    score += when {
        document.normalizedPrimary == query -> 500
        document.normalizedPrimary.contains(query) -> 220
        else -> 0
    }
    if (document.normalizedSecondary.contains(query)) score += 90
    if (document.normalizedAuxiliary.contains(query)) score += 45

    queryTokens.forEach { token ->
        score += tokenScore(token, document.primaryTokens, exact = 80, prefix = 50)
        score += tokenScore(token, document.secondaryTokens, exact = 24, prefix = 12)
        score += tokenScore(token, document.auxiliaryTokens, exact = 10, prefix = 5)
    }

    if (queryTokens.size > 1 && phrasePrefixMatches(queryTokens, document.primaryTokens)) {
        score += 140
    }
    return score
}

private fun tokenMatchesDocument(token: String, document: SearchDocument): Boolean {
    return tokenScore(token, document.primaryTokens, exact = 1, prefix = 1) > 0 ||
        tokenScore(token, document.secondaryTokens, exact = 1, prefix = 1) > 0 ||
        tokenScore(token, document.auxiliaryTokens, exact = 1, prefix = 1) > 0
}

private fun tokenScore(token: String, tokens: List<String>, exact: Int, prefix: Int): Int {
    if (tokens.contains(token)) return exact
    if (tokens.any { it.startsWith(token) }) return prefix
    return 0
}

private fun phrasePrefixMatches(queryTokens: List<String>, tokens: List<String>): Boolean {
    if (queryTokens.size > tokens.size) return false
    return (0..(tokens.size - queryTokens.size)).any { start ->
        queryTokens.indices.all { offset ->
            tokens[start + offset].startsWith(queryTokens[offset])
        }
    }
}

private fun normalizeSearchText(value: String): String {
    val spaced = value.replace("_", " ").replace("-", " ")
    val withoutMarks = Normalizer.normalize(spaced, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
    return withoutMarks
        .lowercase(Locale.getDefault())
        .replace("[^a-z0-9]+".toRegex(), " ")
        .trim()
}

private fun String.searchTokens(): List<String> {
    return split(" ").filter { it.isNotBlank() }
}
