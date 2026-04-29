package app.sanctuary.android

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import coil3.compose.AsyncImage
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import app.sanctuary.android.data.NovenaSummary
import app.sanctuary.android.data.PrayerDetail
import app.sanctuary.android.data.PrayerSummary
import app.sanctuary.android.data.SaintDetail
import app.sanctuary.android.data.SaintSummary
import app.sanctuary.android.data.CommitmentStatus
import app.sanctuary.android.data.FavoriteItemType
import app.sanctuary.android.data.UserNovenaCommitment
import app.sanctuary.android.ui.theme.SanctuaryTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SanctuaryTheme {
                SanctuaryApp(viewModel)
            }
        }
    }
}

private enum class AuthStep {
    Landing,
    Login,
    Register,
    Confirm,
    ForgotPassword,
    ResetPassword
}

private enum class AppTab(val label: String) {
    Home("Home"),
    Novenas("Novenas"),
    Liturgical("Liturgical"),
    Saints("Saints"),
    Me("Me")
}

private enum class CalendarMode(val label: String) {
    Day("Day"),
    Week("Week"),
    Month("Month")
}

private enum class HomeAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
    val illustrationColors: List<Color>
) {
    Saints(
        "Saints",
        "Feasts and biographies",
        Icons.Filled.People,
        Color(0xFFE7C76A),
        listOf(Color(0xFF7BB4CF), Color(0xFF385E77))
    ),
    Novenas(
        "Novenas",
        "Prayer journeys and intentions",
        Icons.Filled.MenuBook,
        Color(0xFF8FE0FF),
        listOf(Color(0xFF6EB9DE), Color(0xFF345C76))
    ),
    Liturgical(
        "Liturgical",
        "Follow the Church calendar",
        Icons.Filled.CalendarMonth,
        Color(0xFFB7D8FF),
        listOf(Color(0xFF7FA4D2), Color(0xFF344E76))
    ),
    Prayers(
        "Prayers",
        "Daily prayer companions",
        Icons.Filled.SelfImprovement,
        Color(0xFFF2A8C4),
        listOf(Color(0xFFB08FCF), Color(0xFF5D4D7C))
    ),
    Intentions(
        "Intentions",
        "Search novena intentions",
        Icons.Filled.Favorite,
        Color(0xFFF2A8C4),
        listOf(Color(0xFF5B4167), Color(0xFF184754))
    ),
    Daily(
        "Daily",
        "Scripture and reflections",
        Icons.Filled.WbSunny,
        Color(0xFFF5D57A),
        listOf(Color(0xFFE0C487), Color(0xFF6C5A3B))
    )
}

@Composable
private fun SanctuaryApp(viewModel: MainViewModel) {
    val session by viewModel.session.collectAsState()
    val saints by viewModel.saints.collectAsState()
    val novenas by viewModel.novenas.collectAsState()
    val intentions by viewModel.intentions.collectAsState()
    val prayers by viewModel.prayers.collectAsState()
    val saintDetail by viewModel.saintDetail.collectAsState()
    val novenaDetail by viewModel.novenaDetail.collectAsState()
    val prayerDetail by viewModel.prayerDetail.collectAsState()
    val novenaProgress by viewModel.novenaProgress.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF132433), Color(0xFF09141F))
                )
            )
    ) {
        if (session.isBootstrapping && session.status == SessionStatus.Loading && session.session == null) {
            BrandedLaunchScreen()
        } else {
            AuthenticatedShell(
                session = session,
                saints = saints,
                novenas = novenas,
                intentions = intentions,
                prayers = prayers,
                onAction = viewModel,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onLogout = viewModel::logout,
                onSaintQueryChanged = viewModel::updateSaintQuery,
                onNovenaQueryChanged = viewModel::updateNovenaQuery,
                onIntentionsQueryChanged = viewModel::updateIntentionsQuery,
                onPrayerQueryChanged = viewModel::updatePrayerQuery,
                onReloadSaints = viewModel::loadSaints,
                onReloadNovenas = viewModel::loadNovenas,
                onReloadIntentions = viewModel::loadIntentions,
                onReloadPrayers = viewModel::loadPrayers,
                onShowSaints = { selectedTab = AppTab.Saints },
                onShowNovenas = { selectedTab = AppTab.Novenas },
                saintDetail = saintDetail,
                novenaDetail = novenaDetail,
                prayerDetail = prayerDetail,
                novenaProgress = novenaProgress,
                onOpenSaint = viewModel::openSaint,
                onOpenNovena = viewModel::openNovena,
                onOpenPrayer = viewModel::openPrayer,
                onCloseSaintDetail = viewModel::closeSaintDetail,
                onCloseNovenaDetail = viewModel::closeNovenaDetail,
                onClosePrayerDetail = viewModel::closePrayerDetail,
                onStartNovena = viewModel::startNovena,
                onStopNovena = viewModel::stopNovena,
                onCompleteNovenaDay = viewModel::completeNovenaDay,
                onToggleFavorite = viewModel::toggleFavorite,
                onUpdateReminderPreferences = viewModel::updateReminderPreferences,
                fetchSaintsInRange = viewModel::fetchSaintsInRange,
                fetchNovenasInRange = viewModel::fetchNovenasInRange,
                fetchLiturgicalRange = viewModel::fetchLiturgicalRange
            )
        }
    }
}

@Composable
private fun BrandedLaunchScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            BrandLogoMark(size = 132.dp, corner = 30.dp, glowExtra = 44.dp)
            Text("Preparing Sanctuary…", color = Color.White)
        }
    }
}

@Composable
private fun AccountAccessScreen(
    session: SessionUiState,
    onAction: MainViewModel,
    embedded: Boolean = false
) {
    var step by rememberSaveable {
        mutableStateOf(
            if (session.status == SessionStatus.AwaitingConfirmation) AuthStep.Confirm else AuthStep.Landing
        )
    }
    var loginEmail by rememberSaveable { mutableStateOf(session.pendingConfirmationEmail.orEmpty()) }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var registerEmail by rememberSaveable { mutableStateOf(session.pendingConfirmationEmail.orEmpty()) }
    var registerPassword by rememberSaveable { mutableStateOf("") }
    var registerPasswordConfirmation by rememberSaveable { mutableStateOf("") }
    var confirmationCode by rememberSaveable { mutableStateOf("") }
    var forgotEmail by rememberSaveable { mutableStateOf(session.pendingPasswordResetEmail.orEmpty()) }
    var resetEmail by rememberSaveable { mutableStateOf(session.pendingPasswordResetEmail.orEmpty()) }
    var resetCode by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var resetPasswordConfirmation by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(session.status, session.pendingConfirmationEmail, session.pendingPasswordResetEmail) {
        if (session.status == SessionStatus.AwaitingConfirmation) {
            step = AuthStep.Confirm
            registerEmail = session.pendingConfirmationEmail.orEmpty()
        }
        if (!session.pendingPasswordResetEmail.isNullOrBlank()) {
            step = AuthStep.ResetPassword
            resetEmail = session.pendingPasswordResetEmail.orEmpty()
        }
    }

    val isBusy = session.status == SessionStatus.Loading
    val introTitle = if (embedded) "Account" else "Sanctuary for Android"
    val introBody = if (embedded) {
        "Log in or create an account to sync your saved saints, novenas, and progress."
    } else {
        "This first Android build is focused on auth parity and live Sanctuary content, so we can distribute something real to the dev track today."
    }

    if (embedded) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AccountAccessContent(
                step = step,
                introTitle = introTitle,
                introBody = introBody,
                session = session,
                isBusy = isBusy,
                loginEmail = loginEmail,
                onLoginEmailChange = { loginEmail = it },
                loginPassword = loginPassword,
                onLoginPasswordChange = { loginPassword = it },
                firstName = firstName,
                onFirstNameChange = { firstName = it },
                lastName = lastName,
                onLastNameChange = { lastName = it },
                registerEmail = registerEmail,
                onRegisterEmailChange = { registerEmail = it },
                registerPassword = registerPassword,
                onRegisterPasswordChange = { registerPassword = it },
                registerPasswordConfirmation = registerPasswordConfirmation,
                onRegisterPasswordConfirmationChange = { registerPasswordConfirmation = it },
                confirmationCode = confirmationCode,
                onConfirmationCodeChange = { confirmationCode = it },
                forgotEmail = forgotEmail,
                onForgotEmailChange = { forgotEmail = it },
                resetEmail = resetEmail,
                onResetEmailChange = { resetEmail = it },
                resetCode = resetCode,
                onResetCodeChange = { resetCode = it },
                newPassword = newPassword,
                onNewPasswordChange = { newPassword = it },
                resetPasswordConfirmation = resetPasswordConfirmation,
                onResetPasswordConfirmationChange = { resetPasswordConfirmation = it },
                onStepChange = { step = it },
                onAction = onAction,
                showAccountEyebrow = false
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AccountAccessContent(
                    step = step,
                    introTitle = introTitle,
                    introBody = introBody,
                    session = session,
                    isBusy = isBusy,
                    loginEmail = loginEmail,
                    onLoginEmailChange = { loginEmail = it },
                    loginPassword = loginPassword,
                    onLoginPasswordChange = { loginPassword = it },
                    firstName = firstName,
                    onFirstNameChange = { firstName = it },
                    lastName = lastName,
                    onLastNameChange = { lastName = it },
                    registerEmail = registerEmail,
                    onRegisterEmailChange = { registerEmail = it },
                    registerPassword = registerPassword,
                    onRegisterPasswordChange = { registerPassword = it },
                    registerPasswordConfirmation = registerPasswordConfirmation,
                    onRegisterPasswordConfirmationChange = { registerPasswordConfirmation = it },
                    confirmationCode = confirmationCode,
                    onConfirmationCodeChange = { confirmationCode = it },
                    forgotEmail = forgotEmail,
                    onForgotEmailChange = { forgotEmail = it },
                    resetEmail = resetEmail,
                    onResetEmailChange = { resetEmail = it },
                    resetCode = resetCode,
                    onResetCodeChange = { resetCode = it },
                    newPassword = newPassword,
                    onNewPasswordChange = { newPassword = it },
                    resetPasswordConfirmation = resetPasswordConfirmation,
                    onResetPasswordConfirmationChange = { resetPasswordConfirmation = it },
                    onStepChange = { step = it },
                    onAction = onAction,
                    showAccountEyebrow = true
                )
            }
        }
    }
}

@Composable
private fun AccountAccessContent(
    step: AuthStep,
    introTitle: String,
    introBody: String,
    session: SessionUiState,
    isBusy: Boolean,
    loginEmail: String,
    onLoginEmailChange: (String) -> Unit,
    loginPassword: String,
    onLoginPasswordChange: (String) -> Unit,
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    registerEmail: String,
    onRegisterEmailChange: (String) -> Unit,
    registerPassword: String,
    onRegisterPasswordChange: (String) -> Unit,
    registerPasswordConfirmation: String,
    onRegisterPasswordConfirmationChange: (String) -> Unit,
    confirmationCode: String,
    onConfirmationCodeChange: (String) -> Unit,
    forgotEmail: String,
    onForgotEmailChange: (String) -> Unit,
    resetEmail: String,
    onResetEmailChange: (String) -> Unit,
    resetCode: String,
    onResetCodeChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    resetPasswordConfirmation: String,
    onResetPasswordConfirmationChange: (String) -> Unit,
    onStepChange: (AuthStep) -> Unit,
    onAction: MainViewModel,
    showAccountEyebrow: Boolean
) {
    val loginReady = loginEmail.trim().isNotEmpty() && loginPassword.isNotEmpty()
    val registerRules = passwordRules(registerPassword)
    val registerPasswordsMatch = passwordsMatch(registerPassword, registerPasswordConfirmation)
    val canSubmitRegistration =
        firstName.trim().isNotEmpty() &&
            lastName.trim().isNotEmpty() &&
            registerEmail.trim().isNotEmpty() &&
            registerRules.all { it.met } &&
            registerPasswordsMatch
    val resetRules = passwordRules(newPassword)
    val resetPasswordsMatch = passwordsMatch(newPassword, resetPasswordConfirmation)
    val canSubmitReset =
        resetEmail.trim().isNotEmpty() &&
            resetCode.trim().isNotEmpty() &&
            resetRules.all { it.met } &&
            resetPasswordsMatch

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (showAccountEyebrow) {
            Text(
                text = "Sanctuary account",
                color = Color(0xFF7AC8EA),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Text(
            text = when (step) {
                AuthStep.Landing -> introTitle
                AuthStep.Login -> "Sign in calmly"
                AuthStep.Register -> "Create your account"
                AuthStep.Confirm -> "Confirm your account"
                AuthStep.ForgotPassword -> "Reset your password calmly"
                AuthStep.ResetPassword -> "Choose a new password"
            },
            color = Color.White,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = introBody,
            color = Color(0xFFD0DFEA),
            lineHeight = 22.sp
        )

        if (!session.message.isNullOrBlank()) {
            Banner(message = session.message, isError = session.isErrorMessage)
        }

        when (step) {
            AuthStep.Landing -> ChoiceStack(
                isBusy = isBusy,
                onLogin = { onStepChange(AuthStep.Login) },
                onRegister = { onStepChange(AuthStep.Register) }
            )

            AuthStep.Login -> AuthCard {
                TextButton(onClick = { onStepChange(AuthStep.Landing) }, enabled = !isBusy) {
                    Text("Back")
                }
                TextFieldBlock("Email", loginEmail, keyboardType = KeyboardType.Email, onValueChange = onLoginEmailChange)
                TextFieldBlock("Password", loginPassword, secure = true, onValueChange = onLoginPasswordChange)
                PrimaryButton("Login", isBusy, enabled = loginReady) {
                    onAction.login(loginEmail.trim(), loginPassword)
                }
                TextButton(onClick = { onStepChange(AuthStep.ForgotPassword) }, enabled = !isBusy) {
                    Text("Forgot password?")
                }
            }

            AuthStep.Register -> AuthCard {
                TextButton(onClick = { onStepChange(AuthStep.Landing) }, enabled = !isBusy) {
                    Text("Back")
                }
                TextFieldBlock("First name", firstName, onValueChange = onFirstNameChange)
                TextFieldBlock("Last name", lastName, onValueChange = onLastNameChange)
                TextFieldBlock("Email", registerEmail, keyboardType = KeyboardType.Email, onValueChange = onRegisterEmailChange)
                TextFieldBlock("Password", registerPassword, secure = true, onValueChange = onRegisterPasswordChange)
                TextFieldBlock("Confirm password", registerPasswordConfirmation, secure = true, onValueChange = onRegisterPasswordConfirmationChange)
                PasswordPanel(
                    rules = registerRules,
                    strengthLabel = passwordStrengthLabel(registerRules),
                    matches = registerPasswordsMatch,
                    confirmationWarning = "Passwords must match before you can create the account."
                )
                PrimaryButton("Create account", isBusy, enabled = canSubmitRegistration) {
                    onAction.register(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        email = registerEmail.trim(),
                        password = registerPassword
                    )
                }
            }

            AuthStep.Confirm -> AuthCard {
                TextButton(onClick = { onStepChange(AuthStep.Login) }, enabled = !isBusy) {
                    Text("Back")
                }
                Text(
                    text = "We sent a confirmation code to ${session.pendingConfirmationEmail ?: registerEmail.trim()}.",
                    color = Color(0xFFD0DFEA)
                )
                TextFieldBlock("Verification code", confirmationCode, keyboardType = KeyboardType.Number, onValueChange = onConfirmationCodeChange)
                PrimaryButton("Confirm account", isBusy, enabled = confirmationCode.trim().isNotEmpty()) {
                    onAction.confirmRegistration(confirmationCode.trim())
                }
                SecondaryButton("Send a new code", isBusy, enabled = true, onClick = onAction::resendConfirmation)
            }

            AuthStep.ForgotPassword -> AuthCard {
                TextButton(onClick = { onStepChange(AuthStep.Login) }, enabled = !isBusy) {
                    Text("Back")
                }
                TextFieldBlock("Email", forgotEmail, keyboardType = KeyboardType.Email, onValueChange = onForgotEmailChange)
                PrimaryButton("Send reset code", isBusy, enabled = forgotEmail.trim().isNotEmpty()) {
                    onAction.forgotPassword(forgotEmail.trim())
                }
            }

            AuthStep.ResetPassword -> AuthCard {
                TextButton(onClick = { onStepChange(AuthStep.Login) }, enabled = !isBusy) {
                    Text("Back")
                }
                Text(
                    text = "We sent a reset code to ${session.pendingPasswordResetEmail ?: forgotEmail.trim()}.",
                    color = Color(0xFFD0DFEA)
                )
                TextFieldBlock("Email", resetEmail, keyboardType = KeyboardType.Email, onValueChange = onResetEmailChange)
                TextFieldBlock("Reset code", resetCode, keyboardType = KeyboardType.Number, onValueChange = onResetCodeChange)
                TextFieldBlock("New password", newPassword, secure = true, onValueChange = onNewPasswordChange)
                TextFieldBlock("Confirm new password", resetPasswordConfirmation, secure = true, onValueChange = onResetPasswordConfirmationChange)
                PasswordPanel(
                    rules = resetRules,
                    strengthLabel = passwordStrengthLabel(resetRules),
                    matches = resetPasswordsMatch,
                    confirmationWarning = "Passwords must match before you can save the new password."
                )
                PrimaryButton("Save new password", isBusy, enabled = canSubmitReset) {
                    onAction.resetPassword(resetEmail.trim(), resetCode.trim(), newPassword)
                }
                SecondaryButton("Send a new reset code", isBusy, enabled = resetEmail.trim().isNotEmpty()) {
                    onAction.forgotPassword(resetEmail.trim())
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AuthenticatedShell(
    session: SessionUiState,
    saints: ContentListUiState<SaintSummary>,
    novenas: ContentListUiState<NovenaSummary>,
    intentions: ContentListUiState<NovenaSummary>,
    prayers: ContentListUiState<PrayerSummary>,
    onAction: MainViewModel,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onLogout: () -> Unit,
    onSaintQueryChanged: (String) -> Unit,
    onNovenaQueryChanged: (String) -> Unit,
    onIntentionsQueryChanged: (String) -> Unit,
    onPrayerQueryChanged: (String) -> Unit,
    onReloadSaints: () -> Unit,
    onReloadNovenas: () -> Unit,
    onReloadIntentions: () -> Unit,
    onReloadPrayers: () -> Unit,
    onShowSaints: () -> Unit,
    onShowNovenas: () -> Unit,
    saintDetail: ContentDetailUiState<SaintDetail>,
    novenaDetail: ContentDetailUiState<app.sanctuary.android.data.NovenaDetail>,
    prayerDetail: ContentDetailUiState<PrayerDetail>,
    novenaProgress: NovenaProgressUiState,
    onOpenSaint: (String) -> Unit,
    onOpenNovena: (String) -> Unit,
    onOpenPrayer: (String) -> Unit,
    onCloseSaintDetail: () -> Unit,
    onCloseNovenaDetail: () -> Unit,
    onClosePrayerDetail: () -> Unit,
    onStartNovena: (String) -> Unit,
    onStopNovena: (String) -> Unit,
    onCompleteNovenaDay: (String, Int) -> Unit,
    onToggleFavorite: (FavoriteItemType, String) -> Unit,
    onUpdateReminderPreferences: (Boolean, Boolean) -> Unit,
    fetchSaintsInRange: suspend (String, String) -> List<app.sanctuary.android.data.SaintDateGroup>,
    fetchNovenasInRange: suspend (String, String) -> List<app.sanctuary.android.data.NovenaCalendarDate>,
    fetchLiturgicalRange: suspend (String, String) -> List<app.sanctuary.android.data.LiturgicalDay>
) {
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showSaintSearch by rememberSaveable { mutableStateOf(false) }
    var showNovenaSearch by rememberSaveable { mutableStateOf(false) }
    var showIntentionsSearch by rememberSaveable { mutableStateOf(false) }
    var showPrayerSearch by rememberSaveable { mutableStateOf(false) }
    var dailyReadingsUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var dailyReadingError by rememberSaveable { mutableStateOf<String?>(null) }
    var saintsCalendarMode by rememberSaveable { mutableStateOf(CalendarMode.Day) }
    var novenasCalendarMode by rememberSaveable { mutableStateOf(CalendarMode.Day) }
    var liturgicalCalendarMode by rememberSaveable { mutableStateOf(CalendarMode.Month) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xCC1B2E3C),
                tonalElevation = 0.dp
            ) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF7AC8EA),
                            selectedTextColor = Color(0xFF7AC8EA),
                            indicatorColor = Color(0x332F9FD9),
                            unselectedIconColor = Color(0xFFBCC9D6),
                            unselectedTextColor = Color(0xFFBCC9D6)
                        ),
                        icon = {
                            Icon(
                                imageVector = tab.icon(),
                                contentDescription = tab.label
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                AppTab.Home -> {
                    item {
                        HomeTopActions(
                            onShowAbout = { showAbout = true }
                        )
                    }
                    item {
                        HomeHeroCard(session)
                    }
                    item {
                        HomeFeatureCard(
                            action = HomeAction.Saints,
                            onClick = { showSaintSearch = true }
                        )
                    }
                    item {
                        HomeFeatureCard(
                            action = HomeAction.Novenas,
                            onClick = { showNovenaSearch = true }
                        )
                    }
                    item {
                        HomeFeatureCard(
                            action = HomeAction.Liturgical,
                            onClick = { onTabSelected(AppTab.Liturgical) }
                        )
                    }
                    item {
                        HomeFeatureCard(
                            action = HomeAction.Prayers,
                            onClick = { showPrayerSearch = true }
                        )
                    }
                    item {
                        HomeFeatureCard(
                            action = HomeAction.Daily,
                            onClick = {
                                scope.launch {
                                    val today = LocalDate.now().toString()
                                    runCatching { fetchLiturgicalRange(today, today) }
                                        .onSuccess { days ->
                                            val readingsUrl = days.firstOrNull()?.readingsUrl
                                            if (!readingsUrl.isNullOrBlank()) {
                                                dailyReadingsUrl = readingsUrl
                                            } else {
                                                dailyReadingError = "Sanctuary could not find today's USCCB reading link right now."
                                            }
                                        }
                                        .onFailure {
                                            dailyReadingError = it.message ?: "Sanctuary could not open today's readings right now."
                                        }
                                }
                            }
                        )
                    }
                    item {
                        HomeFeatureCard(
                            action = HomeAction.Intentions,
                            onClick = { showIntentionsSearch = true }
                        )
                    }
                    if (session.status != SessionStatus.Authenticated) {
                        item {
                            SectionHint(
                                title = "Sign in when you're ready",
                                body = "You can browse Sanctuary first, then use the Me tab to log in or create an account and sync your progress."
                            )
                        }
                    }
                }

                AppTab.Novenas -> {
                    item {
                        NovenasCalendarScreen(
                            mode = novenasCalendarMode,
                            onModeChange = { novenasCalendarMode = it },
                            onSearch = { showNovenaSearch = true },
                            onSearchIntentions = { showIntentionsSearch = true },
                            onOpenNovena = onOpenNovena,
                            fetchNovenasInRange = fetchNovenasInRange
                        )
                    }
                }

                AppTab.Liturgical -> {
                    item {
                        LiturgicalCalendarScreen(
                            mode = liturgicalCalendarMode,
                            onModeChange = { liturgicalCalendarMode = it },
                            fetchLiturgicalRange = fetchLiturgicalRange,
                            onOpenReadings = { dailyReadingsUrl = it }
                        )
                    }
                }

                AppTab.Saints -> {
                    item {
                        SaintsCalendarScreen(
                            mode = saintsCalendarMode,
                            onModeChange = { saintsCalendarMode = it },
                            onSearch = { showSaintSearch = true },
                            onOpenSaint = onOpenSaint,
                            fetchSaintsInRange = fetchSaintsInRange
                        )
                    }
                }

                AppTab.Me -> {
                    if (session.status == SessionStatus.Authenticated) {
                        item {
                            MeScreen(
                                session = session,
                                progress = novenaProgress,
                                onOpenNovena = onOpenNovena,
                                onOpenSaint = onOpenSaint,
                                onLogout = onLogout,
                                onUpdateReminderPreferences = onUpdateReminderPreferences
                            )
                        }
                    } else if (session.status == SessionStatus.Loading && session.session != null) {
                        item { LoadingCard() }
                    } else {
                        item {
                            AccountAccessScreen(
                                session = session,
                                onAction = onAction,
                                embedded = true
                            )
                        }
                    }
                }
            }
        }

        if (showAbout) {
            SanctuaryModalSheet(onDismissRequest = { showAbout = false }) {
                DetailSheetScaffold(
                    title = "About Sanctuary",
                    subtitle = "A calm Catholic prayer companion."
                ) {
                    Text(
                        text = "Sanctuary brings saints, novenas, liturgical rhythms, and your personal prayer progress into one place. Android is now moving onto the same real API-backed foundation as iOS.",
                        color = Color(0xFFD0DFEA),
                        lineHeight = 22.sp
                    )
                }
            }
        }

        dailyReadingError?.let { message ->
            SanctuaryModalSheet(onDismissRequest = { dailyReadingError = null }) {
                DetailErrorSheet(message = message, onDismiss = { dailyReadingError = null })
            }
        }

        dailyReadingsUrl?.let { url ->
            SanctuaryModalSheet(onDismissRequest = { dailyReadingsUrl = null }) {
                DailyReadingsSheet(
                    url = url,
                    onDismiss = { dailyReadingsUrl = null }
                )
            }
        }

        if (showSaintSearch) {
            SanctuaryModalSheet(onDismissRequest = { showSaintSearch = false }) {
                SearchListSheet(
                    title = "Search Saints",
                    query = saints.query,
                    onQueryChanged = onSaintQueryChanged,
                    onSubmit = onReloadSaints,
                    isLoading = saints.isLoading,
                    error = saints.error,
                    emptyLabel = "No saints found yet.",
                    items = saints.items
                ) { item ->
                    ContentCard(
                        title = item.name,
                        subtitle = item.summary ?: "Featured in Sanctuary",
                        detail = item.feastLabel,
                        imageUrl = item.imageUrl,
                        onClick = {
                            showSaintSearch = false
                            onOpenSaint(item.slug)
                        }
                    )
                }
            }
        }

        if (showNovenaSearch) {
            SanctuaryModalSheet(onDismissRequest = { showNovenaSearch = false }) {
                SearchListSheet(
                    title = "Search Novenas",
                    query = novenas.query,
                    onQueryChanged = onNovenaQueryChanged,
                    onSubmit = onReloadNovenas,
                    isLoading = novenas.isLoading,
                    error = novenas.error,
                    emptyLabel = "No novenas found yet.",
                    items = novenas.items
                ) { item ->
                    ContentCard(
                        title = item.title,
                        subtitle = item.description,
                        detail = "${item.durationDays}-day novena",
                        imageUrl = item.imageUrl,
                        onClick = {
                            showNovenaSearch = false
                            onOpenNovena(item.slug)
                        }
                    )
                }
            }
        }

        if (showPrayerSearch) {
            SanctuaryModalSheet(onDismissRequest = { showPrayerSearch = false }) {
                SearchListSheet(
                    title = "Search Prayers",
                    query = prayers.query,
                    onQueryChanged = onPrayerQueryChanged,
                    onSubmit = onReloadPrayers,
                    isLoading = prayers.isLoading,
                    error = prayers.error,
                    emptyLabel = "No prayers found yet.",
                    items = prayers.items
                ) { item ->
                    ContentCard(
                        title = item.title,
                        subtitle = item.bodyPreview,
                        detail = item.category,
                        imageUrl = item.imageUrl,
                        onClick = {
                            showPrayerSearch = false
                            onOpenPrayer(item.slug)
                        }
                    )
                }
            }
        }

        if (showIntentionsSearch) {
            SanctuaryModalSheet(onDismissRequest = { showIntentionsSearch = false }) {
                SearchListSheet(
                    title = "Search Novena Intentions",
                    query = intentions.query,
                    onQueryChanged = onIntentionsQueryChanged,
                    onSubmit = onReloadIntentions,
                    isLoading = intentions.isLoading,
                    error = intentions.error,
                    emptyLabel = "No novena intentions found yet.",
                    items = intentions.items
                ) { item ->
                    ContentCard(
                        title = item.title,
                        subtitle = item.description,
                        detail = item.intentions.take(3).joinToString(" • ").ifBlank { "${item.durationDays}-day novena" },
                        imageUrl = item.imageUrl,
                        onClick = {
                            showIntentionsSearch = false
                            onOpenNovena(item.slug)
                        }
                    )
                }
            }
        }

        if (saintDetail.isLoading || saintDetail.item != null || saintDetail.error != null) {
            SanctuaryModalSheet(onDismissRequest = onCloseSaintDetail) {
                when {
                    saintDetail.isLoading -> DetailLoadingSheet("Loading saint…")
                    saintDetail.error != null -> DetailErrorSheet(saintDetail.error, onCloseSaintDetail)
                    saintDetail.item != null -> SaintDetailSheet(
                        detail = saintDetail.item,
                        session = session,
                        progress = novenaProgress,
                        onToggleFavorite = onToggleFavorite,
                        onDismiss = onCloseSaintDetail
                    )
                }
            }
        }

        if (novenaDetail.isLoading || novenaDetail.item != null || novenaDetail.error != null) {
            SanctuaryModalSheet(onDismissRequest = onCloseNovenaDetail) {
                when {
                    novenaDetail.isLoading -> DetailLoadingSheet("Loading novena…")
                    novenaDetail.error != null -> DetailErrorSheet(novenaDetail.error, onCloseNovenaDetail)
                    novenaDetail.item != null -> NovenaDetailSheet(
                        detail = novenaDetail.item,
                        session = session,
                        progress = novenaProgress,
                        onStart = onStartNovena,
                        onStop = onStopNovena,
                        onCompleteDay = onCompleteNovenaDay,
                        onToggleFavorite = onToggleFavorite,
                        onDismiss = onCloseNovenaDetail
                    )
                }
            }
        }

        if (prayerDetail.isLoading || prayerDetail.item != null || prayerDetail.error != null) {
            SanctuaryModalSheet(onDismissRequest = onClosePrayerDetail) {
                when {
                    prayerDetail.isLoading -> DetailLoadingSheet("Loading prayer…")
                    prayerDetail.error != null -> DetailErrorSheet(prayerDetail.error, onClosePrayerDetail)
                    prayerDetail.item != null -> PrayerDetailSheet(
                        detail = prayerDetail.item,
                        onDismiss = onClosePrayerDetail
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceStack(
    isBusy: Boolean,
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceCard(
            eyebrow = "Returning to Sanctuary",
            title = "Login",
            body = "Sign in to your saved saints, novenas, and progress.",
            enabled = !isBusy,
            onClick = onLogin
        )
        ChoiceCard(
            eyebrow = "New to Sanctuary",
            title = "Register",
            body = "Create a free account so this Android build can sync with your existing Sanctuary account.",
            enabled = !isBusy,
            onClick = onRegister
        )
    }
}

@Composable
private fun ChoiceCard(
    eyebrow: String,
    title: String,
    body: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
        shape = RoundedCornerShape(24.dp),
        onClick = onClick,
        enabled = enabled
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(eyebrow, color = Color(0xFF7AC8EA), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(body, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
        }
    }
}

@Composable
private fun AuthCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun Banner(message: String, isError: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) Color(0x4D7B1E26) else Color(0x332F9FD9)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = if (isError) Color(0xFFFFD9DD) else Color(0xFFE9F7FF),
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun TextFieldBlock(
    label: String,
    value: String,
    secure: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        visualTransformation = if (secure) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun PrimaryButton(
    title: String,
    isBusy: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isBusy,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5CAED4)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            }
            Text(title, color = Color.White)
        }
    }
}

@Composable
private fun SecondaryButton(
    title: String,
    isBusy: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isBusy,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0x6622394C),
            contentColor = Color(0xFF7AC8EA)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color(0xFF7AC8EA),
                    strokeWidth = 2.dp
                )
            }
            Text(title)
        }
    }
}

private data class PasswordRuleUi(
    val label: String,
    val met: Boolean
)

private fun passwordRules(password: String): List<PasswordRuleUi> = listOf(
    PasswordRuleUi("At least 8 characters", password.length >= 8),
    PasswordRuleUi("One uppercase letter", password.any(Char::isUpperCase)),
    PasswordRuleUi("One lowercase letter", password.any(Char::isLowerCase)),
    PasswordRuleUi("One number", password.any(Char::isDigit)),
    PasswordRuleUi("One special character", password.any { !it.isLetterOrDigit() })
)

private fun passwordsMatch(password: String, confirmation: String): Boolean =
    confirmation.isNotEmpty() && password == confirmation

private fun passwordStrengthLabel(rules: List<PasswordRuleUi>): String {
    val metCount = rules.count { it.met }
    return when {
        metCount == rules.size -> "Ready"
        metCount >= 4 -> "Almost there"
        metCount >= 2 -> "Needs work"
        else -> "Too weak"
    }
}

@Composable
private fun PasswordPanel(
    rules: List<PasswordRuleUi>,
    strengthLabel: String,
    matches: Boolean,
    confirmationWarning: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x6622394C)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Password strength",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = strengthLabel,
                    color = if (rules.all { it.met }) Color(0xFF7AC8EA) else Color(0xFFD0DFEA),
                    fontWeight = FontWeight.SemiBold
                )
            }

            rules.forEach { rule ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (rule.met) "✓" else "•",
                        color = if (rule.met) Color(0xFF7AC8EA) else Color(0xFFD0DFEA),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = rule.label,
                        color = if (rule.met) Color.White else Color(0xFFD0DFEA)
                    )
                }
            }

            Text(
                text = if (matches) "Passwords match." else confirmationWarning,
                color = if (matches) Color(0xFF7AC8EA) else Color(0xFFD0DFEA)
            )
        }
    }
}

@Composable
private fun HomeCard(session: SessionUiState) {
    val profile = session.profile
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = profile?.displayName ?: session.session?.displayName ?: "Sanctuary account",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = session.session?.email.orEmpty(),
                color = Color(0xFFD0DFEA)
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProfileMetric("Active novenas", profile?.activeNovenaCount ?: 0)
            ProfileMetric("Favorite novenas", profile?.favoriteNovenaCount ?: 0)
            ProfileMetric("Favorite saints", profile?.favoriteSaintCount ?: 0)
            ProfileMetric("Environment", BuildConfig.ENVIRONMENT.uppercase())
        }
    }
}

@Composable
private fun MeScreen(
    session: SessionUiState,
    progress: NovenaProgressUiState,
    onOpenNovena: (String) -> Unit,
    onOpenSaint: (String) -> Unit,
    onLogout: () -> Unit,
    onUpdateReminderPreferences: (Boolean, Boolean) -> Unit
) {
    val profile = session.profile
    val favoriteNovenas = progress.favorites.filter { it.itemType == FavoriteItemType.Novena }
    val favoriteSaints = progress.favorites.filter { it.itemType == FavoriteItemType.Saint }
    var novenaReminderToggle by remember(profile?.novenaRemindersEnabled) {
        mutableStateOf(profile?.novenaRemindersEnabled == true)
    }
    var dailyReminderToggle by remember(profile?.feastRemindersEnabled) {
        mutableStateOf(profile?.feastRemindersEnabled == true)
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Me", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
        Text(
            "Your novenas in progress and saved favorites.",
            color = Color(0xFFD0DFEA),
            fontSize = 18.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Signed in", color = Color(0xFF7AC8EA), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initialsFor(profile?.displayName ?: session.session?.displayName ?: "S"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            profile?.displayName ?: session.session?.displayName ?: "Sanctuary account",
                            color = Color.White,
                            fontSize = 30.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            profile?.email ?: session.session?.email.orEmpty(),
                            color = Color(0xFFD0DFEA)
                        )
                        Text(
                            "Your favorites, active novenas, and future account settings live here.",
                            color = Color(0xFFD0DFEA),
                            lineHeight = 20.sp
                        )
                    }
                }
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A4153)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Logout", color = Color.White)
                }
            }
        }

        MeSectionCard(title = "Reminders") {
            ReminderToggleRow(
                title = "Novenas in progress",
                subtitle = "Send morning and evening reminders when you have a novena in progress.",
                checked = novenaReminderToggle,
                enabled = !session.isSavingReminderPreferences,
                onCheckedChange = { checked ->
                    novenaReminderToggle = checked
                    onUpdateReminderPreferences(checked, dailyReminderToggle)
                }
            )
            ReminderToggleRow(
                title = "Once-daily Sanctuary reminder",
                subtitle = "Send a gentle morning reminder when you do not have a novena in progress.",
                checked = dailyReminderToggle,
                enabled = !session.isSavingReminderPreferences,
                onCheckedChange = { checked ->
                    dailyReminderToggle = checked
                    onUpdateReminderPreferences(novenaReminderToggle, checked)
                }
            )
        }

        MeSectionCard(title = "Novenas in Progress") {
            if (progress.commitments.none { it.status == CommitmentStatus.Active }) {
                Text("No novenas in progress.", color = Color(0xFFD0DFEA))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    progress.commitments
                        .filter { it.status == CommitmentStatus.Active }
                        .sortedByDescending { it.updatedAt }
                        .forEach { commitment ->
                            LinkedMeRow(
                                title = progress.novenaTitles[commitment.novenaId]
                                    ?: commitment.novenaId.replace("-", " ").replaceFirstChar { it.uppercase() },
                                subtitle = "Day ${commitment.currentDay} of ${progress.novenaDurations[commitment.novenaId] ?: 9}",
                                onClick = { onOpenNovena(commitment.novenaId) }
                            )
                        }
                }
            }
        }

        MeSectionCard(title = "Favorite Novenas") {
            if (favoriteNovenas.isEmpty()) {
                Text("No favorite novenas yet.", color = Color(0xFFD0DFEA))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    favoriteNovenas.forEach { favorite ->
                        LinkedMeRow(
                            title = progress.novenaTitles[favorite.itemId]
                                ?: favorite.itemId.replace("-", " ").replaceFirstChar { it.uppercase() },
                            subtitle = null,
                            onClick = { onOpenNovena(favorite.itemId) }
                        )
                    }
                }
            }
        }

        MeSectionCard(title = "Favorite Saints") {
            if (favoriteSaints.isEmpty()) {
                Text("No favorite saints yet.", color = Color(0xFFD0DFEA))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    favoriteSaints.forEach { favorite ->
                        LinkedMeRow(
                            title = progress.saintNames[favorite.itemId]
                                ?: favorite.itemId.replace("_", " ").replace("-", " ").replaceFirstChar { it.uppercase() },
                            subtitle = null,
                            onClick = { onOpenSaint(progress.saintSlugs[favorite.itemId] ?: favorite.itemId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MeSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ReminderToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (checked) Color(0x245CAED4) else Color(0xFF2A4153))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color(0xFFD0DFEA), lineHeight = 18.sp, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF8EDBFF),
                checkedBorderColor = Color(0xFF8EDBFF),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.16f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.10f),
                disabledCheckedThumbColor = Color.White.copy(alpha = 0.9f),
                disabledCheckedTrackColor = Color(0xFF8EDBFF).copy(alpha = 0.55f),
                disabledUncheckedThumbColor = Color.White.copy(alpha = 0.75f),
                disabledUncheckedTrackColor = Color.White.copy(alpha = 0.10f)
            )
        )
    }
}

@Composable
private fun LinkedMeRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A4153)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.let {
                    Text(it, color = Color(0xFFD0DFEA), fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.85f))
        }
    }
}

private fun initialsFor(name: String): String {
    val parts = name.split(" ").filter { it.isNotBlank() }
    if (parts.isEmpty()) return "S"
    return parts.take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifBlank { "S" }
}

@Composable
private fun HomeHeroCard(session: SessionUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "#1 Catholic Prayer Companion",
                color = Color(0xFF7AC8EA),
                style = MaterialTheme.typography.labelLarge
            )
            Box(
                modifier = Modifier
                    .size(156.dp),
                contentAlignment = Alignment.Center
            ) {
                BrandLogoMark(size = 132.dp, corner = 30.dp, glowExtra = 44.dp)
            }
            Text(
                text = if (session.status == SessionStatus.Authenticated) {
                    "Welcome back to your sanctuary"
                } else {
                    "Welcome to your sanctuary"
                },
                color = Color.White,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "How do you want to connect with God?",
                color = Color(0xFFE7F2FA),
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Prayer, liturgy, and saints in one calm place.",
                color = Color(0xFFD0DFEA),
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun BrandLogoMark(
    size: androidx.compose.ui.unit.Dp,
    corner: androidx.compose.ui.unit.Dp,
    glowExtra: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier.size(size + glowExtra),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size + glowExtra)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x38E8C56A),
                            Color(0x12E8C56A),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Image(
            painter = painterResource(id = R.drawable.brand_logo),
            contentDescription = "Sanctuary",
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(corner))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(corner))
        )
    }
}

@Composable
private fun HomeTopActions(
    onShowAbout: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TopPillButton(
            modifier = Modifier.weight(1f),
            title = "About Sanctuary",
            icon = Icons.Filled.Info,
            onClick = onShowAbout
        )
        TopPillButton(
            modifier = Modifier.weight(1f),
            title = "Language: English",
            icon = Icons.Filled.Language,
            onClick = {}
        )
    }
}

@Composable
private fun TopPillButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = title, tint = Color(0xFFD0DFEA), modifier = Modifier.size(18.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun HomeFeatureCard(
    action: HomeAction,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(28.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(188.dp)
                .background(
                    brush = action.cardBrush(),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.06f), Color.Transparent, Color.Black.copy(alpha = 0.18f))
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 22.dp, end = 148.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.title,
                            tint = action.iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            action.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Text(
                            action.subtitle,
                            color = Color(0xFFD0DFEA),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .size(width = 156.dp, height = 108.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            brush = Brush.linearGradient(action.illustrationColors)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                )

                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SouthEast,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .size(width = 78.dp, height = 4.dp)
                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
                )
            }
        }
    }
}

private fun HomeAction.cardBrush(): Brush = when (this) {
    HomeAction.Saints -> Brush.linearGradient(
        listOf(Color(0xFF153646).copy(alpha = 0.92f), Color(0xFF1C5461).copy(alpha = 0.76f))
    )
    HomeAction.Novenas -> Brush.linearGradient(
        listOf(Color(0xFF0D2535).copy(alpha = 0.94f), Color(0xFF1B576C).copy(alpha = 0.72f))
    )
    HomeAction.Liturgical -> Brush.linearGradient(
        listOf(Color(0xFF243652).copy(alpha = 0.92f), Color(0xFF4A66A0).copy(alpha = 0.74f))
    )
    HomeAction.Prayers -> Brush.linearGradient(
        listOf(Color(0xFF2C3144).copy(alpha = 0.90f), Color(0xFF15424D).copy(alpha = 0.72f))
    )
    HomeAction.Intentions -> Brush.linearGradient(
        listOf(Color(0xFF4C3B56).copy(alpha = 0.90f), Color(0xFF15404B).copy(alpha = 0.74f))
    )
    HomeAction.Daily -> Brush.linearGradient(
        listOf(Color(0xFF1C514C).copy(alpha = 0.90f), Color(0xFF143B4D).copy(alpha = 0.74f))
    )
}

@Composable
private fun LoadingCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(0xFF7AC8EA),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text("Preparing Sanctuary…", color = Color.White)
        }
    }
}


@Composable
private fun ProfileMetric(label: String, value: Any) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFFBCC9D6))
        Text(value.toString(), color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SearchCard(
    title: String,
    query: String,
    onQueryChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            TextFieldBlock(label = "Search", value = query, onValueChange = onQueryChanged)
            PrimaryButton("Refresh", false, onClick = onSubmit)
        }
    }
}

@Composable
private fun ContentCard(
    title: String,
    subtitle: String,
    detail: String,
    imageUrl: String? = null,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xB323394C)),
        shape = RoundedCornerShape(22.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            ThumbnailImage(
                imageUrl = imageUrl,
                contentDescription = title,
                modifier = Modifier.size(82.dp),
                shape = RoundedCornerShape(18.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(detail, color = Color(0xFF7AC8EA), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = subtitle,
                    color = Color(0xFFD0DFEA),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
            Icon(
                imageVector = Icons.Filled.SouthEast,
                contentDescription = null,
                tint = Color(0xFFBCC9D6)
            )
        }
    }
}

@Composable
private fun DetailSheetScaffold(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp)
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, color = Color(0xFF7AC8EA))
        }
        content()
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SanctuaryModalSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            content()
        }
    }
}

@Composable
private fun DetailLoadingSheet(message: String) {
    DetailSheetScaffold(title = message) {
        InlineLoading(message)
    }
}

@Composable
private fun DetailErrorSheet(message: String, onDismiss: () -> Unit) {
    DetailSheetScaffold(title = "Could not load this right now") {
        Banner(message ?: "Sanctuary could not complete that request right now.", isError = true)
        PrimaryButton("Close", false, onClick = onDismiss)
    }
}

@Composable
private fun SaintDetailSheet(
    detail: SaintDetail,
    session: SessionUiState,
    progress: NovenaProgressUiState,
    onToggleFavorite: (FavoriteItemType, String) -> Unit,
    onDismiss: () -> Unit
) {
    val isFavorite = progress.favorites.any { it.itemType == FavoriteItemType.Saint && it.itemId == detail.id }
    DetailSheetScaffold(
        title = detail.name,
        subtitle = detail.feastLabel
    ) {
        ThumbnailImage(
            imageUrl = detail.imageUrl,
            contentDescription = detail.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f),
            shape = RoundedCornerShape(24.dp)
        )
        if (session.status == SessionStatus.Authenticated) {
            Button(
                onClick = { onToggleFavorite(FavoriteItemType.Saint, detail.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFavorite) Color(0xFF5CAED4) else Color(0xFF22394C),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isFavorite) "Saved to Favorites" else "Add to Favorites")
            }
        }
        detail.summary?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = Color.White, lineHeight = 24.sp)
        }
        detail.biography?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
        }
        if (detail.sources.isNotEmpty()) {
            Text("Sources", color = Color.White, fontWeight = FontWeight.SemiBold)
            detail.sources.take(3).forEach { source ->
                Text("• ${source.title}", color = Color(0xFFD0DFEA), lineHeight = 20.sp)
            }
        }
        PrimaryButton("Close", false, onClick = onDismiss)
    }
}

@Composable
private fun NovenaDetailSheet(
    detail: app.sanctuary.android.data.NovenaDetail,
    session: SessionUiState,
    progress: NovenaProgressUiState,
    onStart: (String) -> Unit,
    onStop: (String) -> Unit,
    onCompleteDay: (String, Int) -> Unit,
    onToggleFavorite: (FavoriteItemType, String) -> Unit,
    onDismiss: () -> Unit
) {
    val activeCommitment = progress.commitments.firstOrNull {
        it.novenaId == detail.id && it.status == CommitmentStatus.Active
    }
    val isFavorite = progress.favorites.any { it.itemType == FavoriteItemType.Novena && it.itemId == detail.id }
    val latestCommitment = progress.commitments
        .filter { it.novenaId == detail.id }
        .maxByOrNull { it.updatedAt }
    val orderedDays = detail.days.sortedBy { it.dayNumber }
    var selectedDay by rememberSaveable(detail.id) {
        mutableStateOf(activeCommitment?.currentDay?.coerceIn(1, maxOf(1, detail.durationDays)) ?: orderedDays.firstOrNull()?.dayNumber ?: 1)
    }
    val selectedDayDetail = orderedDays.firstOrNull { it.dayNumber == selectedDay }
    val canStart = session.status == SessionStatus.Authenticated &&
        activeCommitment == null &&
        latestCommitment?.status != CommitmentStatus.Completed
    val completionLabel = when {
        latestCommitment?.status == CommitmentStatus.Completed -> "Completed"
        activeCommitment != null -> "Complete Day ${activeCommitment.currentDay}"
        else -> "Complete Day 1"
    }

    LaunchedEffect(activeCommitment?.currentDay, detail.id) {
        val nextDay = activeCommitment?.currentDay?.coerceIn(1, maxOf(1, detail.durationDays))
            ?: orderedDays.firstOrNull()?.dayNumber
            ?: 1
        selectedDay = nextDay
    }

    DetailSheetScaffold(
        title = detail.title,
        subtitle = "${detail.durationDays}-day novena"
    ) {
        ThumbnailImage(
            imageUrl = detail.imageUrl,
            contentDescription = detail.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f),
            shape = RoundedCornerShape(24.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            detail.description.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Color.White, lineHeight = 24.sp)
            }
            if (detail.intentions.isNotEmpty()) {
                Text("Intentions", color = Color.White, fontWeight = FontWeight.SemiBold)
                detail.intentions.take(4).forEach { intention ->
                    Text("• $intention", color = Color(0xFFD0DFEA), lineHeight = 20.sp)
                }
            }
        }

        if (session.status == SessionStatus.Authenticated) {
            Button(
                onClick = { onToggleFavorite(FavoriteItemType.Novena, detail.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFavorite) Color(0xFF5CAED4) else Color(0xFF22394C),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isFavorite) "Saved to Favorites" else "Add to Favorites")
            }
        }

        Text("Choose Day", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 92.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(orderedDays.size) { index ->
                val day = orderedDays[index]
                val active = day.dayNumber == selectedDay
                Button(
                    onClick = { selectedDay = day.dayNumber },
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) Color(0xFF5FAED5) else Color(0xFF20384B),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Day", fontSize = 12.sp, color = Color.White.copy(alpha = 0.78f))
                        Text("${day.dayNumber}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }
        }

        when {
            session.status != SessionStatus.Authenticated -> {
                Banner("Log in or register to start this novena and track your progress.", isError = false)
            }
            activeCommitment != null -> {
                PrimaryButton("Stop Novena", false, onClick = { onStop(detail.id) })
            }
            canStart -> {
                PrimaryButton("Start Novena", false, onClick = { onStart(detail.id) })
            }
        }

        DetailSectionCard(title = "Day $selectedDay") {
            if (selectedDayDetail == null) {
                Text("No day content was found for this novena yet.", color = Color(0xFFD0DFEA), lineHeight = 22.sp)
            } else {
                selectedDayDetail.title?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp)
                }
                selectedDayDetail.scripture?.takeIf { it.isNotBlank() }?.let {
                    DetailSectionLabel("Scripture")
                    Text(it, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
                }
                selectedDayDetail.prayer?.takeIf { it.isNotBlank() }?.let {
                    DetailSectionLabel("Prayer")
                    Text(it, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
                }
                selectedDayDetail.reflection?.takeIf { it.isNotBlank() }?.let {
                    DetailSectionLabel("Reflection")
                    Text(it, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
                }
                selectedDayDetail.body?.takeIf { it.isNotBlank() }?.let {
                    DetailSectionLabel("Content")
                    Text(it, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
                }
                selectedDayDetail.openingPrayer?.takeIf { it.isNotBlank() }?.let {
                    DetailSectionLabel("Opening Prayer")
                    Text(it, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
                }
                selectedDayDetail.meditation?.takeIf { it.isNotBlank() }?.let {
                    DetailSectionLabel("Meditation")
                    Text(it, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
                }
                selectedDayDetail.closingPrayer?.takeIf { it.isNotBlank() }?.let {
                    DetailSectionLabel("Closing Prayer")
                    Text(it, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
                }
            }
        }

        if (session.status == SessionStatus.Authenticated && (activeCommitment != null || latestCommitment?.status == CommitmentStatus.Completed)) {
            Button(
                onClick = { onCompleteDay(detail.id, detail.durationDays) },
                enabled = latestCommitment?.status != CommitmentStatus.Completed,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5CAED4)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(completionLabel, color = Color.White)
            }
        }

        PrimaryButton("Close", false, onClick = onDismiss)
    }
}

@Composable
private fun PrayerDetailSheet(
    detail: PrayerDetail,
    onDismiss: () -> Unit
) {
    DetailSheetScaffold(
        title = detail.title,
        subtitle = detail.category
    ) {
        ThumbnailImage(
            imageUrl = detail.imageUrl,
            contentDescription = detail.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f),
            shape = RoundedCornerShape(24.dp)
        )
        detail.alternateTitle?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = Color(0xFF7AC8EA), lineHeight = 22.sp)
        }
        Text(detail.body, color = Color.White, lineHeight = 24.sp)
        detail.note?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
        }
        PrimaryButton("Close", false, onClick = onDismiss)
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            content()
        }
    }
}

@Composable
private fun DetailSectionLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFF7AC8EA),
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    )
}

private fun AppTab.icon(): ImageVector = when (this) {
    AppTab.Home -> Icons.Filled.Today
    AppTab.Novenas -> Icons.Filled.MenuBook
    AppTab.Liturgical -> Icons.Filled.CalendarMonth
    AppTab.Saints -> Icons.Filled.People
    AppTab.Me -> Icons.Filled.Person
}

@Composable
private fun InlineLoading(message: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = Color(0xFF7AC8EA),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(message, color = Color(0xFFD0DFEA))
    }
}

@Composable
private fun SectionHint(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xB323394C)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(body, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
        }
    }
}

@Composable
private fun ThumbnailImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp)
) {
    val resolvedUrl = imageUrl?.takeIf { it.isNotBlank() }?.let {
        if (it.startsWith("http://") || it.startsWith("https://")) it
        else "${BuildConfig.API_BASE_URL.trimEnd('/')}/$it"
    }

    if (resolvedUrl != null) {
        AsyncImage(
            model = resolvedUrl,
            contentDescription = contentDescription,
            modifier = modifier.clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF385E77), Color(0xFF22394C))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun <T> SearchListSheet(
    title: String,
    query: String,
    onQueryChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean,
    error: String?,
    emptyLabel: String,
    items: List<T>,
    itemContent: @Composable (T) -> Unit
) {
    DetailSheetScaffold(title = title) {
        SearchCard(
            title = title,
            query = query,
            onQueryChanged = onQueryChanged,
            onSubmit = onSubmit
        )
        when {
            isLoading -> InlineLoading("Loading…")
            error != null -> Banner(error, isError = true)
            items.isEmpty() -> Text(emptyLabel, color = Color(0xFFD0DFEA))
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { item -> itemContent(item) }
            }
        }
    }
}

@Composable
private fun SaintsCalendarScreen(
    mode: CalendarMode,
    onModeChange: (CalendarMode) -> Unit,
    onSearch: () -> Unit,
    onOpenSaint: (String) -> Unit,
    fetchSaintsInRange: suspend (String, String) -> List<app.sanctuary.android.data.SaintDateGroup>
) {
    val today = LocalDate.now()
    var selectedDay by rememberSaveable { mutableStateOf(today.dayOfMonth) }
    var selectedMonth by rememberSaveable { mutableStateOf(today.monthValue) }
    var selectedYear by rememberSaveable { mutableStateOf(today.year) }
    val month = remember(selectedYear, selectedMonth) { YearMonth.of(selectedYear, selectedMonth) }
    val state by produceState<CalendarLoadState<List<app.sanctuary.android.data.SaintDateGroup>>>(
        initialValue = CalendarLoadState.Loading,
        month, selectedYear, selectedMonth
    ) {
        value = runCatching { fetchSaintsInRange(month.atDay(1).toString(), month.atEndOfMonth().toString()) }
            .fold(
                onSuccess = { CalendarLoadState.Ready(it) },
                onFailure = { CalendarLoadState.Error(it.message ?: "Could not load saints right now.") }
            )
    }

    LaunchedEffect(selectedMonth, selectedYear) {
        selectedDay = selectedDay.coerceIn(1, month.lengthOfMonth())
    }

    CalendarSurface(
        title = if (mode == CalendarMode.Day) {
            LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth()))
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
        } else {
            month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        },
        subtitle = "Saints • Tap to jump",
        mode = mode,
        onModeChange = onModeChange,
        onToday = {
            selectedDay = today.dayOfMonth
            selectedMonth = today.monthValue
            selectedYear = today.year
        },
        onPrev = {
            when (mode) {
                CalendarMode.Day -> {
                    val previous = LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())).minusDays(1)
                    selectedYear = previous.year
                    selectedMonth = previous.monthValue
                    selectedDay = previous.dayOfMonth
                }
                CalendarMode.Week -> {
                    val previous = LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())).minusWeeks(1)
                    selectedYear = previous.year
                    selectedMonth = previous.monthValue
                    selectedDay = previous.dayOfMonth
                }
                CalendarMode.Month -> {
                    val previous = month.minusMonths(1)
                    selectedYear = previous.year
                    selectedMonth = previous.monthValue
                    selectedDay = selectedDay.coerceIn(1, previous.lengthOfMonth())
                }
            }
        },
        onNext = {
            when (mode) {
                CalendarMode.Day -> {
                    val next = LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())).plusDays(1)
                    selectedYear = next.year
                    selectedMonth = next.monthValue
                    selectedDay = next.dayOfMonth
                }
                CalendarMode.Week -> {
                    val next = LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())).plusWeeks(1)
                    selectedYear = next.year
                    selectedMonth = next.monthValue
                    selectedDay = next.dayOfMonth
                }
                CalendarMode.Month -> {
                    val next = month.plusMonths(1)
                    selectedYear = next.year
                    selectedMonth = next.monthValue
                    selectedDay = selectedDay.coerceIn(1, next.lengthOfMonth())
                }
            }
        }
    ) {
        when (val current = state) {
            CalendarLoadState.Loading -> InlineLoading("Loading saints…")
            is CalendarLoadState.Error -> Banner(current.message, isError = true)
            is CalendarLoadState.Ready -> {
                val saintByDate = current.value.mapNotNull { group ->
                    group.saints.firstOrNull()?.let { LocalDate.parse(group.date) to it }
                }.toMap()
                when (mode) {
                    CalendarMode.Day -> {
                        val previewDate = month.atDay(selectedDay.coerceIn(1, month.lengthOfMonth()))
                        val preview = saintByDate[previewDate]
                        if (preview != null) {
                            DayPreviewCard(
                                date = previewDate,
                                title = preview.name,
                                subtitle = preview.feastLabel,
                                imageUrl = preview.imageUrl,
                                buttonLabel = "Open details",
                                onClick = { onOpenSaint(preview.slug) }
                            )
                        } else {
                            SectionHint("No saints found", "Sanctuary did not return saint observances for this day yet.")
                        }
                    }
                    CalendarMode.Week -> {
                        CalendarWeekGrid(
                            month = month,
                            selectedDay = selectedDay,
                            labelForDay = { day -> saintByDate[month.atDay(day)]?.let { shortLabel(sanitizedSaintName(it.name)) } ?: "·" },
                            borderColorForDay = { Color(0xFFF5F5F5) },
                            onDaySelected = {
                                selectedDay = it
                                onModeChange(CalendarMode.Day)
                            }
                        )
                    }
                    CalendarMode.Month -> {
                        CalendarMonthGrid(
                            month = month,
                            selectedDay = selectedDay,
                            labelForDay = { day -> saintByDate[month.atDay(day)]?.let { shortLabel(sanitizedSaintName(it.name)) } ?: "·" },
                            borderColorForDay = { Color(0xFFF5F5F5) },
                            onDaySelected = {
                                selectedDay = it
                                onModeChange(CalendarMode.Day)
                            }
                        )
                    }
                }
                PrimaryButton("Search Saints", false, onClick = onSearch)
                SeasonLegend()
            }
        }
    }
}

@Composable
private fun NovenasCalendarScreen(
    mode: CalendarMode,
    onModeChange: (CalendarMode) -> Unit,
    onSearch: () -> Unit,
    onSearchIntentions: () -> Unit,
    onOpenNovena: (String) -> Unit,
    fetchNovenasInRange: suspend (String, String) -> List<app.sanctuary.android.data.NovenaCalendarDate>
) {
    val today = LocalDate.now()
    var selectedDay by rememberSaveable { mutableStateOf(today.dayOfMonth) }
    var selectedMonth by rememberSaveable { mutableStateOf(today.monthValue) }
    var selectedYear by rememberSaveable { mutableStateOf(today.year) }
    val month = remember(selectedYear, selectedMonth) { YearMonth.of(selectedYear, selectedMonth) }
    val state by produceState<CalendarLoadState<List<app.sanctuary.android.data.NovenaCalendarDate>>>(
        initialValue = CalendarLoadState.Loading,
        month, selectedYear, selectedMonth
    ) {
        value = runCatching { fetchNovenasInRange(month.atDay(1).toString(), month.atEndOfMonth().toString()) }
            .fold(
                onSuccess = { CalendarLoadState.Ready(it) },
                onFailure = { CalendarLoadState.Error(it.message ?: "Could not load novenas right now.") }
            )
    }

    LaunchedEffect(selectedMonth, selectedYear) {
        selectedDay = selectedDay.coerceIn(1, month.lengthOfMonth())
    }

    CalendarSurface(
        title = if (mode == CalendarMode.Day) {
            LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth()))
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
        } else {
            month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        },
        subtitle = "Novenas • Tap to jump",
        mode = mode,
        onModeChange = onModeChange,
        onToday = {
            selectedDay = today.dayOfMonth
            selectedMonth = today.monthValue
            selectedYear = today.year
        },
        onPrev = {
            when (mode) {
                CalendarMode.Day -> {
                    val previous = LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())).minusDays(1)
                    selectedYear = previous.year
                    selectedMonth = previous.monthValue
                    selectedDay = previous.dayOfMonth
                }
                CalendarMode.Week -> {
                    val previous = LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())).minusWeeks(1)
                    selectedYear = previous.year
                    selectedMonth = previous.monthValue
                    selectedDay = previous.dayOfMonth
                }
                CalendarMode.Month -> {
                    val previous = month.minusMonths(1)
                    selectedYear = previous.year
                    selectedMonth = previous.monthValue
                    selectedDay = selectedDay.coerceIn(1, previous.lengthOfMonth())
                }
            }
        },
        onNext = {
            when (mode) {
                CalendarMode.Day -> {
                    val next = LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())).plusDays(1)
                    selectedYear = next.year
                    selectedMonth = next.monthValue
                    selectedDay = next.dayOfMonth
                }
                CalendarMode.Week -> {
                    val next = LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())).plusWeeks(1)
                    selectedYear = next.year
                    selectedMonth = next.monthValue
                    selectedDay = next.dayOfMonth
                }
                CalendarMode.Month -> {
                    val next = month.plusMonths(1)
                    selectedYear = next.year
                    selectedMonth = next.monthValue
                    selectedDay = selectedDay.coerceIn(1, next.lengthOfMonth())
                }
            }
        }
    ) {
        when (val current = state) {
            CalendarLoadState.Loading -> InlineLoading("Loading novenas…")
            is CalendarLoadState.Error -> Banner(current.message, isError = true)
            is CalendarLoadState.Ready -> {
                val novenaByDate = current.value.mapNotNull { entry ->
                    (entry.startingNovena ?: entry.novenas.firstOrNull())?.let { LocalDate.parse(entry.date) to it }
                }.toMap()
                when (mode) {
                    CalendarMode.Day -> {
                        val previewDate = month.atDay(selectedDay.coerceIn(1, month.lengthOfMonth()))
                        val preview = novenaByDate[previewDate]
                        if (preview != null) {
                            DayPreviewCard(
                                date = previewDate,
                                title = preview.title,
                                subtitle = preview.description,
                                imageUrl = preview.imageUrl,
                                buttonLabel = "Open details",
                                onClick = { onOpenNovena(preview.slug) }
                            )
                        } else {
                            SectionHint("No novenas found", "Sanctuary did not return novena observances for this day yet.")
                        }
                    }
                    CalendarMode.Week -> {
                        CalendarWeekGrid(
                            month = month,
                            selectedDay = selectedDay,
                            labelForDay = { day -> novenaByDate[month.atDay(day)]?.let { shortLabel(it.title) } ?: "·" },
                            borderColorForDay = { Color(0xFF7AC8EA) },
                            onDaySelected = {
                                selectedDay = it
                                onModeChange(CalendarMode.Day)
                            }
                        )
                    }
                    CalendarMode.Month -> {
                        CalendarMonthGrid(
                            month = month,
                            selectedDay = selectedDay,
                            labelForDay = { day -> novenaByDate[month.atDay(day)]?.let { shortLabel(it.title) } ?: "·" },
                            borderColorForDay = { Color(0xFF7AC8EA) },
                            onDaySelected = {
                                selectedDay = it
                                onModeChange(CalendarMode.Day)
                            }
                        )
                    }
                }
                PrimaryButton("Search Novenas", false, onClick = onSearch)
                PrimaryButton("Search Novena Intentions", false, onClick = onSearchIntentions)
                SeasonLegend()
            }
        }
    }
}

@Composable
private fun LiturgicalCalendarScreen(
    mode: CalendarMode,
    onModeChange: (CalendarMode) -> Unit,
    fetchLiturgicalRange: suspend (String, String) -> List<app.sanctuary.android.data.LiturgicalDay>,
    onOpenReadings: (String) -> Unit
) {
    val today = LocalDate.now()
    var selectedDay by rememberSaveable { mutableStateOf(today.dayOfMonth) }
    var selectedMonth by rememberSaveable { mutableStateOf(today.monthValue) }
    var selectedYear by rememberSaveable { mutableStateOf(today.year) }
    var readingError by rememberSaveable { mutableStateOf<String?>(null) }
    val month = remember(selectedYear, selectedMonth) { YearMonth.of(selectedYear, selectedMonth) }
    val state by produceState<CalendarLoadState<List<app.sanctuary.android.data.LiturgicalDay>>>(
        initialValue = CalendarLoadState.Loading,
        month, selectedYear, selectedMonth
    ) {
        value = runCatching {
            fetchLiturgicalRange(month.atDay(1).toString(), month.atEndOfMonth().toString())
        }.fold(
            onSuccess = { CalendarLoadState.Ready(it) },
            onFailure = { CalendarLoadState.Error(it.message ?: "Could not load liturgical days right now.") }
        )
    }

    LaunchedEffect(selectedMonth, selectedYear) {
        selectedDay = selectedDay.coerceIn(1, month.lengthOfMonth())
    }

    CalendarSurface(
        title = if (mode == CalendarMode.Day) {
            LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth()))
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
        } else {
            month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        },
        subtitle = "Liturgical • Tap to jump",
        mode = mode,
        onModeChange = onModeChange,
        onToday = {
            selectedDay = today.dayOfMonth
            selectedMonth = today.monthValue
            selectedYear = today.year
        },
        onPrev = {
            when (mode) {
                CalendarMode.Day -> {
                    val previous = LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())).minusDays(1)
                    selectedYear = previous.year
                    selectedMonth = previous.monthValue
                    selectedDay = previous.dayOfMonth
                }
                CalendarMode.Week -> {
                    val previous = LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())).minusWeeks(1)
                    selectedYear = previous.year
                    selectedMonth = previous.monthValue
                    selectedDay = previous.dayOfMonth
                }
                CalendarMode.Month -> {
                    val previous = month.minusMonths(1)
                    selectedYear = previous.year
                    selectedMonth = previous.monthValue
                    selectedDay = selectedDay.coerceIn(1, previous.lengthOfMonth())
                }
            }
        },
        onNext = {
            when (mode) {
                CalendarMode.Day -> {
                    val next = LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())).plusDays(1)
                    selectedYear = next.year
                    selectedMonth = next.monthValue
                    selectedDay = next.dayOfMonth
                }
                CalendarMode.Week -> {
                    val next = LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())).plusWeeks(1)
                    selectedYear = next.year
                    selectedMonth = next.monthValue
                    selectedDay = next.dayOfMonth
                }
                CalendarMode.Month -> {
                    val next = month.plusMonths(1)
                    selectedYear = next.year
                    selectedMonth = next.monthValue
                    selectedDay = selectedDay.coerceIn(1, next.lengthOfMonth())
                }
            }
        }
    ) {
        when (val current = state) {
            CalendarLoadState.Loading -> InlineLoading("Loading liturgical days…")
            is CalendarLoadState.Error -> Banner(current.message, isError = true)
            is CalendarLoadState.Ready -> {
                val liturgicalByDate = current.value.associateBy { LocalDate.parse(it.date) }
                when (mode) {
                    CalendarMode.Day -> {
                        val previewDate = month.atDay(selectedDay.coerceIn(1, month.lengthOfMonth()))
                        val preview = liturgicalByDate[previewDate]
                        if (preview != null) {
                            LiturgicalDayPreviewCard(
                                date = previewDate,
                                detail = preview,
                                onOpenReadings = {
                                    val readingsUrl = preview.readingsUrl
                                    if (!readingsUrl.isNullOrBlank()) {
                                        onOpenReadings(readingsUrl)
                                    } else {
                                        readingError = "Sanctuary could not find daily readings for this day."
                                    }
                                }
                            )
                        } else {
                            SectionHint("No liturgical reading found", "Sanctuary did not return a liturgical observance for this day yet.")
                        }
                    }
                    CalendarMode.Week -> {
                        CalendarWeekGrid(
                            month = month,
                            selectedDay = selectedDay,
                            labelForDay = { day ->
                                liturgicalByDate[month.atDay(day)]?.let { shortLiturgicalLabel(it) } ?: "·"
                            },
                            borderColorForDay = { day ->
                                liturgicalBorderColor(liturgicalByDate[month.atDay(day)]?.season)
                            },
                            onDaySelected = {
                                selectedDay = it
                                onModeChange(CalendarMode.Day)
                            }
                        )
                    }
                    CalendarMode.Month -> {
                        LiturgicalMonthGrid(
                            month = month,
                            days = liturgicalByDate,
                            selectedDay = selectedDay,
                            onDaySelected = {
                                selectedDay = it
                                onModeChange(CalendarMode.Day)
                            }
                        )
                    }
                }
            }
        }
        readingError?.let { Banner(it, isError = true) }
        SeasonLegend()
    }
}

@Composable
private fun DailyReadingsSheet(
    url: String,
    onDismiss: () -> Unit
) {
    DetailSheetScaffold(
        title = "Daily Readings",
        subtitle = "USCCB readings inside Sanctuary"
    ) {
        DailyReadingsWebView(url = url)
        PrimaryButton("Close", false, onClick = onDismiss)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DailyReadingsWebView(url: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10212E)),
        shape = RoundedCornerShape(24.dp)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    loadUrl(url)
                }
            },
            update = { webView ->
                if (webView.url != url) {
                    webView.loadUrl(url)
                }
            }
        )
    }
}

@Composable
private fun CalendarSurface(
    title: String,
    subtitle: String,
    mode: CalendarMode,
    onModeChange: (CalendarMode) -> Unit,
    onToday: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalendarNavButton(symbol = "‹", onClick = onPrev)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = Color(0xFFD0DFEA))
                }
                CalendarNavButton(symbol = "›", onClick = onNext)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChipButton(title = "Today", selected = false, onClick = onToday)
            CalendarMode.entries.forEach { entry ->
                FilterChipButton(
                    title = entry.label,
                    selected = mode == entry,
                    onClick = { onModeChange(entry) }
                )
            }
        }
        content()
    }
}

@Composable
private fun CalendarNavButton(
    symbol: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x6622394C)),
        shape = RoundedCornerShape(999.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FilterChipButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF5CAED4) else Color(0x6622394C)
        ),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DayPreviewCard(
    date: LocalDate,
    title: String,
    subtitle: String,
    imageUrl: String?,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
        shape = RoundedCornerShape(26.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(date.dayOfMonth.toString(), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = Color(0xFFD0DFEA), maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(buttonLabel, color = Color.White, fontWeight = FontWeight.Medium)
            }
            Box(modifier = Modifier.width(140.dp).aspectRatio(1.15f)) {
                ThumbnailImage(
                    imageUrl = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(20.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .background(Color(0x22324456), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SouthEast,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarWeekGrid(
    month: YearMonth,
    selectedDay: Int,
    labelForDay: (Int) -> String,
    borderColorForDay: (Int) -> Color,
    onDaySelected: (Int) -> Unit
) {
    val clamped = selectedDay.coerceIn(1, month.lengthOfMonth())
    val selectedDate = month.atDay(clamped)
    val start = selectedDate.minusDays((selectedDate.dayOfWeek.value % 7).toLong())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CalendarWeekHeaderRow()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(7) { offset ->
                val date = start.plusDays(offset.toLong())
                if (date.month == month.month) {
                    val today = LocalDate.now()
                    CalendarEntryCell(
                        day = date.dayOfMonth,
                        label = labelForDay(date.dayOfMonth),
                        borderColor = borderColorForDay(date.dayOfMonth),
                        selected = date.dayOfMonth == clamped,
                        isToday = date == today,
                        height = 80.dp,
                        modifier = Modifier.weight(1f),
                        onClick = { onDaySelected(date.dayOfMonth) }
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f).height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    month: YearMonth,
    selectedDay: Int,
    labelForDay: (Int) -> String,
    borderColorForDay: (Int) -> Color,
    onDaySelected: (Int) -> Unit
) {
    val first = month.atDay(1)
    val offset = (first.dayOfWeek.value % 7)
    val total = month.lengthOfMonth()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CalendarWeekHeaderRow()
        var dayNumber = 1
        repeat(6) { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(7) { column ->
                    val index = row * 7 + column
                    if (index < offset || dayNumber > total) {
                        Spacer(modifier = Modifier.weight(1f).height(72.dp))
                    } else {
                        val currentDay = dayNumber
                        val today = LocalDate.now()
                        CalendarEntryCell(
                            day = currentDay,
                            label = labelForDay(currentDay),
                            borderColor = borderColorForDay(currentDay),
                            selected = currentDay == selectedDay,
                            isToday = month.atDay(currentDay) == today,
                            height = 72.dp,
                            modifier = Modifier.weight(1f),
                            onClick = { onDaySelected(currentDay) }
                        )
                        dayNumber += 1
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarEntryCell(
    day: Int,
    label: String,
    borderColor: Color,
    selected: Boolean,
    isToday: Boolean,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale = if (selected) 1f else 0.985f
    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .scale(scale),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2A4153),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    if (isToday) 3.dp else 1.6.dp,
                    if (isToday) Color(0xFFEFD572) else borderColor,
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    day.toString(),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LiturgicalMonthGrid(
    month: YearMonth,
    days: Map<LocalDate, app.sanctuary.android.data.LiturgicalDay>,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    val first = month.atDay(1)
    val offset = (first.dayOfWeek.value % 7)
    val total = month.lengthOfMonth()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CalendarWeekHeaderRow()
        var dayNumber = 1
        repeat(6) { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(7) { column ->
                    val index = row * 7 + column
                    if (index < offset || dayNumber > total) {
                        Spacer(modifier = Modifier.weight(1f).height(72.dp))
                    } else {
                        val date = month.atDay(dayNumber)
                        LiturgicalDayCell(
                            day = dayNumber,
                            detail = days[date],
                            modifier = Modifier.weight(1f),
                            selected = dayNumber == selectedDay,
                            onClick = { onDaySelected(dayNumber) }
                        )
                        dayNumber += 1
                    }
                }
            }
        }
    }
}

private fun shortLabel(raw: String, max: Int = 14): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return "·"
    return if (trimmed.length > max) "${trimmed.take(max - 1)}…" else trimmed
}

private fun shortWord(raw: String, max: Int = 7): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    return if (trimmed.length > max) "${trimmed.take(max - 1)}…" else trimmed
}

private fun sanitizedSaintName(raw: String): String {
    return raw.replace(Regex(""",\s*\d{3,4}[–-]\d{2,4}$"""), "").trim()
}

@Composable
private fun LiturgicalDayCell(
    day: Int,
    detail: app.sanctuary.android.data.LiturgicalDay?,
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit
) {
    val strokeColor = liturgicalBorderColor(detail?.season)
    val compactLabel = detail?.let(::shortLiturgicalLabel).orEmpty()
    Card(
        modifier = modifier.aspectRatio(0.86f),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xB323394C) else Color(0x9922394C)),
        shape = RoundedCornerShape(18.dp)
        ,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(if (selected) 2.dp else 1.dp, strokeColor, RoundedCornerShape(18.dp))
                .padding(horizontal = 6.dp, vertical = 7.dp)
        ) {
            Text(
                day.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            Text(
                text = compactLabel,
                color = Color(0xFFD0DFEA),
                fontSize = 10.sp,
                lineHeight = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun CalendarWeekHeaderRow() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { label ->
            Text(
                label,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LiturgicalDayPreviewCard(
    date: LocalDate,
    detail: app.sanctuary.android.data.LiturgicalDay,
    onOpenReadings: () -> Unit
) {
    DayPreviewCard(
        date = date,
        title = detail.primaryRank,
        subtitle = detail.observances.firstOrNull().orEmpty().ifBlank { detail.season.replaceFirstChar { it.uppercase() } },
        imageUrl = null,
        buttonLabel = "Open daily readings",
        onClick = onOpenReadings
    )
}

private fun liturgicalBorderColor(season: String?): Color = when (season?.lowercase()) {
    "advent" -> Color(0xFF8B5CF6)
    "christmas" -> Color(0xFFE7C76A)
    "lent" -> Color(0xFFD16BA5)
    "easter" -> Color(0xFFF5F5F5)
    else -> Color(0xFF6FB56B)
}

private fun shortLiturgicalLabel(detail: app.sanctuary.android.data.LiturgicalDay): String {
    val source = detail.observances.firstOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: detail.primaryRank
    val significantWords = source
        .split(Regex("\\s+"))
        .map { it.trim().trim(',', '.', ';', ':') }
        .filter { it.isNotBlank() }
        .filterNot { it.equals("of", ignoreCase = true) || it.equals("the", ignoreCase = true) || it.equals("within", ignoreCase = true) }

    if (significantWords.isEmpty()) return "·"
    if (significantWords.size == 1) return shortWord(significantWords.first(), max = 10)

    val first = shortWord(significantWords[0], max = 7)
    val second = shortWord(significantWords[1], max = 7)
    return "$first\n$second"
}

@Composable
private fun SeasonLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SeasonDot("Advent", Color(0xFF8B5CF6))
        SeasonDot("Christmas", Color(0xFFE7C76A))
        SeasonDot("Lent", Color(0xFFD16BA5))
        SeasonDot("Easter", Color(0xFFF5F5F5))
        SeasonDot("Ordinary Time", Color(0xFF6FB56B))
    }
}

@Composable
private fun SeasonDot(label: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(7.dp).background(color, CircleShape))
        Text(label, color = Color(0xFFD0DFEA), fontSize = 12.sp)
    }
}

private sealed interface CalendarLoadState<out T> {
    data object Loading : CalendarLoadState<Nothing>
    data class Ready<T>(val value: T) : CalendarLoadState<T>
    data class Error(val message: String) : CalendarLoadState<Nothing>
}

private fun calendarRange(mode: CalendarMode, anchor: LocalDate): Pair<String, String> {
    return when (mode) {
        CalendarMode.Day -> anchor.toString() to anchor.toString()
        CalendarMode.Week -> anchor.toString() to anchor.plusDays(6).toString()
        CalendarMode.Month -> {
            val month = YearMonth.from(anchor)
            month.atDay(1).toString() to month.atEndOfMonth().toString()
        }
    }
}

private fun headerTitle(mode: CalendarMode, anchor: LocalDate): String {
    return when (mode) {
        CalendarMode.Day -> anchor.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
        CalendarMode.Week -> "${anchor.format(DateTimeFormatter.ofPattern("MMMM d"))} - ${anchor.plusDays(6).format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))}"
        CalendarMode.Month -> YearMonth.from(anchor).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
}
