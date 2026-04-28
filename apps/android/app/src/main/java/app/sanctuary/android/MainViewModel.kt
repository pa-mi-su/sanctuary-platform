package app.sanctuary.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.sanctuary.android.data.AuthRegistrationResponse
import app.sanctuary.android.data.NovenaSummary
import app.sanctuary.android.data.SaintSummary
import app.sanctuary.android.data.SanctuaryApiFactory
import app.sanctuary.android.data.SessionRepository
import app.sanctuary.android.data.StoredSession
import app.sanctuary.android.data.UserProfile
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

    init {
        bootstrap()
    }

    fun bootstrap() {
        viewModelScope.launch {
            _session.update { it.copy(status = SessionStatus.Loading, message = null, isErrorMessage = false) }
            val result = repository.bootstrap()
            if (result.authenticated) {
                _session.value = SessionUiState(
                    status = SessionStatus.Authenticated,
                    session = result.session,
                    profile = result.profile
                )
                loadInitialContent()
            } else {
                _session.value = SessionUiState(
                    status = if (result.errorMessage != null) SessionStatus.Failed else SessionStatus.SignedOut,
                    message = result.errorMessage,
                    isErrorMessage = result.errorMessage != null
                )
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
        _saints.value = ContentListUiState()
        _novenas.value = ContentListUiState()
    }

    fun updateSaintQuery(query: String) {
        _saints.update { it.copy(query = query) }
    }

    fun updateNovenaQuery(query: String) {
        _novenas.update { it.copy(query = query) }
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

    private fun loadInitialContent() {
        loadSaints()
        loadNovenas()
    }

    private fun setBusy() {
        _session.update {
            it.copy(status = SessionStatus.Loading, message = null, isErrorMessage = false)
        }
    }
}
