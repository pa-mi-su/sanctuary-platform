package app.sanctuary.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.sanctuary.android.data.AuthRegistrationResponse
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
import app.sanctuary.android.data.FavoriteItemType
import app.sanctuary.android.data.UserFavorite
import app.sanctuary.android.data.UserNovenaCommitment
import app.sanctuary.android.data.UserProfile
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
    val session: StoredSession? = null,
    val profile: UserProfile? = null,
    val pendingConfirmationEmail: String? = null,
    val pendingPasswordResetEmail: String? = null,
    val message: String? = null,
    val isErrorMessage: Boolean = false
)

data class ContentListUiState<T>(
    val items: List<T> = emptyList(),
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
    val isLoading: Boolean = false,
    val error: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SessionRepository(
        context = application.applicationContext,
        api = SanctuaryApiFactory.create { _session.value.session?.accessToken }
    )

    private val _session = MutableStateFlow(SessionUiState())
    val session: StateFlow<SessionUiState> = _session.asStateFlow()

    private val _saints = MutableStateFlow(ContentListUiState<SaintSummary>())
    val saints: StateFlow<ContentListUiState<SaintSummary>> = _saints.asStateFlow()

    private val _novenas = MutableStateFlow(ContentListUiState<NovenaSummary>())
    val novenas: StateFlow<ContentListUiState<NovenaSummary>> = _novenas.asStateFlow()

    private val _intentions = MutableStateFlow(ContentListUiState<NovenaSummary>())
    val intentions: StateFlow<ContentListUiState<NovenaSummary>> = _intentions.asStateFlow()

    private val _prayers = MutableStateFlow(ContentListUiState<PrayerSummary>())
    val prayers: StateFlow<ContentListUiState<PrayerSummary>> = _prayers.asStateFlow()

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
            _session.update { it.copy(status = SessionStatus.Loading, message = null, isErrorMessage = false) }
            val result = runCatching {
                withTimeoutOrNull(10_000) { repository.bootstrap() }
                    ?: SessionBootstrapResult.signedOut()
            }.getOrElse { failure ->
                SessionBootstrapResult.signedOut()
            }
            if (result.authenticated) {
                _session.value = SessionUiState(
                    status = SessionStatus.Authenticated,
                    session = result.session,
                    profile = result.profile
                )
                loadInitialContent()
                refreshNovenaProgress()
            } else {
                _session.value = SessionUiState(
                    status = SessionStatus.SignedOut
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
                repository.register(firstName, lastName, email, password)
            }.onSuccess { response: AuthRegistrationResponse ->
                _session.value = SessionUiState(
                    status = SessionStatus.AwaitingConfirmation,
                    pendingConfirmationEmail = response.email,
                    message = "We sent a confirmation code to ${response.email}.",
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

    fun confirmRegistration(code: String) {
        val email = _session.value.pendingConfirmationEmail ?: return
        viewModelScope.launch {
            setBusy()
            runCatching {
                repository.confirm(email = email, code = code)
            }.onSuccess { response ->
                _session.value = _session.value.copy(
                    status = SessionStatus.SignedOut,
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

    fun resendConfirmation() {
        val email = _session.value.pendingConfirmationEmail ?: return
        viewModelScope.launch {
            setBusy()
            runCatching { repository.resendConfirmation(email) }
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
            runCatching { repository.forgotPassword(email) }
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
            runCatching { repository.resetPassword(email, code, newPassword) }
                .onSuccess { response ->
                    _session.value = SessionUiState(
                        status = SessionStatus.SignedOut,
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

    fun login(email: String, password: String) {
        viewModelScope.launch {
            setBusy()
            runCatching { repository.login(email, password) }
                .onSuccess { result ->
                    _session.value = SessionUiState(
                        status = SessionStatus.Authenticated,
                        session = result.session,
                        profile = result.profile
                    )
                    loadInitialContent()
                    refreshNovenaProgress()
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
        _session.value = SessionUiState(status = SessionStatus.SignedOut)
        _novenaProgress.value = NovenaProgressUiState()
        loadInitialContent()
    }

    fun updateSaintQuery(query: String) {
        _saints.update { it.copy(query = query) }
    }

    fun updateNovenaQuery(query: String) {
        _novenas.update { it.copy(query = query) }
    }

    fun updatePrayerQuery(query: String) {
        _prayers.update { it.copy(query = query) }
    }

    fun updateIntentionsQuery(query: String) {
        _intentions.update { it.copy(query = query) }
    }

    fun loadSaints() {
        viewModelScope.launch {
            _saints.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.listSaints(_saints.value.query)
            }.onSuccess { items ->
                _saints.value = _saints.value.copy(items = items, isLoading = false, error = null)
            }.onFailure { failure ->
                _saints.value = _saints.value.copy(isLoading = false, error = failure.message)
            }
        }
    }

    fun loadNovenas() {
        viewModelScope.launch {
            _novenas.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.listNovenas(_novenas.value.query)
            }.onSuccess { items ->
                _novenas.value = _novenas.value.copy(items = items, isLoading = false, error = null)
            }.onFailure { failure ->
                _novenas.value = _novenas.value.copy(isLoading = false, error = failure.message)
            }
        }
    }

    fun loadPrayers() {
        viewModelScope.launch {
            _prayers.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.listPrayers(_prayers.value.query)
            }.onSuccess { items ->
                _prayers.value = _prayers.value.copy(items = items, isLoading = false, error = null)
            }.onFailure { failure ->
                _prayers.value = _prayers.value.copy(isLoading = false, error = failure.message)
            }
        }
    }

    fun loadIntentions() {
        viewModelScope.launch {
            _intentions.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.listNovenasByIntentions(_intentions.value.query)
            }.onSuccess { items ->
                _intentions.value = _intentions.value.copy(items = items, isLoading = false, error = null)
            }.onFailure { failure ->
                _intentions.value = _intentions.value.copy(isLoading = false, error = failure.message)
            }
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

                            val novenaDetails = novenaIds.associateWith { id ->
                                runCatching { repository.fetchNovenaDetail(id) }.getOrNull()
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

                            _novenaProgress.value = NovenaProgressUiState(
                                commitments = commitments,
                                favorites = favorites,
                                saintNames = saintNames,
                                saintSlugs = saintSlugs,
                                novenaTitles = novenaDetails.mapNotNull { (id, detail) -> detail?.let { id to it.title } }.toMap(),
                                novenaDurations = novenaDetails.mapNotNull { (id, detail) -> detail?.let { id to it.durationDays } }.toMap(),
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
        val currentlyFavorite = _novenaProgress.value.favorites.any { it.itemType == itemType && it.itemId == itemId }
        viewModelScope.launch {
            runCatching {
                repository.toggleFavorite(
                    itemType = itemType,
                    itemId = itemId,
                    enabled = !currentlyFavorite
                )
            }.onSuccess {
                refreshNovenaProgress()
            }.onFailure { failure ->
                _novenaProgress.update { it.copy(error = failure.message) }
            }
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
        loadIntentions()
    }

    private fun setBusy() {
        _session.update {
            it.copy(status = SessionStatus.Loading, message = null, isErrorMessage = false)
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
