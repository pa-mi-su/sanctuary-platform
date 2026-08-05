package app.sanctuary.android

import android.annotation.SuppressLint
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
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
import app.sanctuary.android.data.SearchTerm
import app.sanctuary.android.data.CommitmentStatus
import app.sanctuary.android.data.FavoriteItemType
import app.sanctuary.android.data.UserNovenaCommitment
import app.sanctuary.android.ui.theme.SanctuaryTheme
import app.sanctuary.android.ui.theme.SanctuaryGradientBottom
import app.sanctuary.android.ui.theme.SanctuaryGradientMid
import app.sanctuary.android.ui.theme.SanctuaryGradientTop
import app.sanctuary.android.ui.theme.SanctuaryCardElevated
import app.sanctuary.android.ui.theme.SanctuaryTabActive
import app.sanctuary.android.ui.theme.SanctuaryTabBackground
import app.sanctuary.android.ui.theme.SanctuaryTabBorder
import app.sanctuary.android.ui.theme.SanctuaryTabInactive
import app.sanctuary.android.ui.AppLanguage
import app.sanctuary.android.ui.LocalSanctuaryStrings
import app.sanctuary.android.ui.SanctuaryStrings
import app.sanctuary.android.ui.sanctuaryStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun Modifier.sanctuaryCardShadow(shape: RoundedCornerShape = RoundedCornerShape(24.dp)) =
    this.shadow(14.dp, shape, clip = false)

private fun Modifier.calendarDaySwipe(
    enabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
): Modifier {
    if (!enabled) {
        return this
    }

    return pointerInput(onPrevious, onNext) {
        val swipeThreshold = 80.dp.toPx()
        var dragAmountTotal = 0f
        detectHorizontalDragGestures(
            onDragStart = { dragAmountTotal = 0f },
            onHorizontalDrag = { change, dragAmount ->
                dragAmountTotal += dragAmount
                change.consume()
            },
            onDragEnd = {
                when (calendarSwipeDirection(dragAmountTotal, swipeThreshold)) {
                    CalendarSwipeDirection.Previous -> onPrevious()
                    CalendarSwipeDirection.Next -> onNext()
                    CalendarSwipeDirection.None -> Unit
                }
            }
        )
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SanctuaryTheme {
                SanctuaryApp(viewModel)
            }
        }
        handleSharedContentIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedContentIntent(intent)
    }

    private fun handleSharedContentIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val host = uri.host?.lowercase()
        if (uri.scheme != "https" || (host != "mydailysanctuary.com" && host != "www.mydailysanctuary.com")) {
            return
        }

        val segments = uri.pathSegments
        if (segments.size < 2) {
            return
        }

        when (segments[0]) {
            "saints" -> viewModel.openSaint(segments[1])
            "novenas" -> viewModel.openNovena(segments[1])
            "prayers" -> viewModel.openPrayer(segments[1])
        }
    }
}

private enum class SharedContentKind(val path: String) {
    Saint("saints"),
    Novena("novenas"),
    Prayer("prayers")
}

private fun sharedContentUrl(kind: SharedContentKind, slug: String): String =
    "https://mydailysanctuary.com/${kind.path}/$slug"

private fun shareContent(context: android.content.Context, title: String, message: String, url: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TITLE, title)
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "$message\n$url")
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share"))
}

private enum class AuthStep {
    Landing,
    Login,
    Register,
    Confirm,
    ForgotPassword,
    ResetPassword
}

private enum class AppTab {
    Home,
    Novenas,
    Liturgical,
    Saints,
    Me
}

private enum class CalendarMode {
    Day,
    Week,
    Month
}

private enum class AboutDocument {
    Support,
    Privacy
}

private enum class HomeAction(
    val titleKey: String,
    val subtitleKey: String,
    val icon: ImageVector,
    val iconTint: Color,
    val illustrationColors: List<Color>,
    val artworkAssetPath: String? = null
) {
    Saints(
        "home.saints",
        "home.saintsSubtitle",
        Icons.Filled.People,
        Color(0xFFE7C76A),
        listOf(Color(0xFF7BB4CF), Color(0xFF385E77)),
        "file:///android_asset/home_cards/saints.svg"
    ),
    Novenas(
        "home.novenas",
        "home.novenasSubtitle",
        Icons.Filled.MenuBook,
        Color(0xFF8FE0FF),
        listOf(Color(0xFF6EB9DE), Color(0xFF345C76)),
        "file:///android_asset/home_cards/novenas.svg"
    ),
    Liturgical(
        "home.liturgical",
        "home.liturgicalSubtitle",
        Icons.Filled.CalendarMonth,
        Color(0xFFB7D8FF),
        listOf(Color(0xFF7FA4D2), Color(0xFF344E76))
    ),
    Prayers(
        "home.prayers",
        "home.prayersSubtitle",
        Icons.Filled.SelfImprovement,
        Color(0xFFF2A8C4),
        listOf(Color(0xFFB08FCF), Color(0xFF5D4D7C)),
        "file:///android_asset/home_cards/prayers.svg"
    ),
    Rosary(
        "home.rosary",
        "home.rosarySubtitle",
        Icons.Filled.SelfImprovement,
        Color(0xFFE7C76A),
        listOf(Color(0xFF43506B), Color(0xFF10394A)),
        "file:///android_asset/home_cards/rosary.svg"
    ),
    Intentions(
        "home.intentions",
        "home.intentionsSubtitle",
        Icons.Filled.Favorite,
        Color(0xFFF2A8C4),
        listOf(Color(0xFF5B4167), Color(0xFF184754)),
        "file:///android_asset/home_cards/intentions.svg"
    ),
    Patronage(
        "home.patronage",
        "home.patronageSubtitle",
        Icons.Filled.People,
        Color(0xFFE7C76A),
        listOf(Color(0xFF7BB4CF), Color(0xFF385E77)),
        "file:///android_asset/home_cards/saints.svg"
    ),
    Daily(
        "home.daily",
        "home.dailySubtitle",
        Icons.Filled.WbSunny,
        Color(0xFFF5D57A),
        listOf(Color(0xFFE0C487), Color(0xFF6C5A3B)),
        "file:///android_asset/home_cards/daily-readings.svg"
    )
}

private enum class TermSearchMode {
    Intentions,
    Patronage
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SanctuaryApp(viewModel: MainViewModel) {
    val session by viewModel.session.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val saints by viewModel.saints.collectAsState()
    val novenas by viewModel.novenas.collectAsState()
    val intentionTerms by viewModel.intentionTerms.collectAsState()
    val patronageTerms by viewModel.patronageTerms.collectAsState()
    val prayers by viewModel.prayers.collectAsState()
    val rosaries by viewModel.rosaries.collectAsState()
    val saintDetail by viewModel.saintDetail.collectAsState()
    val novenaDetail by viewModel.novenaDetail.collectAsState()
    val prayerDetail by viewModel.prayerDetail.collectAsState()
    val novenaProgress by viewModel.novenaProgress.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home) }
    var showLanguagePicker by rememberSaveable { mutableStateOf(false) }

    CompositionLocalProvider(LocalSanctuaryStrings provides SanctuaryStrings(appLanguage)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            SanctuaryBackdrop()
            if (session.isBootstrapping && session.status == SessionStatus.Loading && session.session == null) {
                BrandedLaunchScreen()
            } else {
                AuthenticatedShell(
                    session = session,
                    saints = saints,
                    novenas = novenas,
                    intentionTerms = intentionTerms,
                    patronageTerms = patronageTerms,
                    prayers = prayers,
                    rosaries = rosaries,
                    selectedLanguage = appLanguage,
                    onUpdateLanguage = {
                        viewModel.updateLanguage(it)
                        showLanguagePicker = false
                    },
                    onShowLanguagePicker = { showLanguagePicker = true },
                    onAction = viewModel,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onLogout = viewModel::logout,
                    onDeleteAccount = viewModel::deleteAccount,
                    onSaintQueryChanged = viewModel::updateSaintQuery,
                    onNovenaQueryChanged = viewModel::updateNovenaQuery,
                    onIntentionTermQueryChanged = viewModel::updateIntentionTermQuery,
                    onPatronageTermQueryChanged = viewModel::updatePatronageTermQuery,
                    onPrayerQueryChanged = viewModel::updatePrayerQuery,
                    onRosaryQueryChanged = viewModel::updateRosaryQuery,
                    onReloadSaints = viewModel::loadSaints,
                    onReloadNovenas = viewModel::loadNovenas,
                    onReloadIntentionTerms = viewModel::loadIntentionTerms,
                    onReloadPatronageTerms = viewModel::loadPatronageTerms,
                    onReloadPrayers = viewModel::loadPrayers,
                    onReloadRosaries = viewModel::loadRosaries,
                    onShowSaints = { selectedTab = AppTab.Saints },
                    onShowNovenas = { selectedTab = AppTab.Novenas },
                    saintDetail = saintDetail,
                    novenaDetail = novenaDetail,
                    prayerDetail = prayerDetail,
                    novenaProgress = novenaProgress,
                    onOpenSaint = viewModel::openSaint,
                    onOpenNovena = viewModel::openNovena,
                    onOpenPrayer = viewModel::openPrayer,
                    onSelectIntentionTerm = viewModel::selectIntentionTerm,
                    onSelectPatronageTerm = viewModel::selectPatronageTerm,
                    onClearSelectedIntentionTerm = viewModel::clearSelectedIntentionTerm,
                    onClearSelectedPatronageTerm = viewModel::clearSelectedPatronageTerm,
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
            if (showLanguagePicker) {
                ModalBottomSheet(
                    onDismissRequest = { showLanguagePicker = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    LanguagePickerSheet(
                        current = appLanguage,
                        onSelect = {
                            viewModel.updateLanguage(it)
                            showLanguagePicker = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SanctuaryBackdrop() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        SanctuaryGradientTop,
                        SanctuaryGradientMid,
                        SanctuaryGradientBottom
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.09f), Color.Transparent, Color.Black.copy(alpha = 0.18f))
                    )
                )
        )
    }
}

@Composable
private fun BrandedLaunchScreen() {
    val l10n = sanctuaryStrings()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            BrandLogoMark(size = 132.dp, corner = 30.dp, glowExtra = 44.dp)
            Text(l10n.t("common.loading"), color = Color.White)
        }
    }
}

@Composable
private fun AccountAccessScreen(
    session: SessionUiState,
    onAction: MainViewModel,
    embedded: Boolean = false,
    initialStep: AuthStep = AuthStep.Landing
) {
    var step by rememberSaveable(initialStep) {
        mutableStateOf(
            if (session.status == SessionStatus.AwaitingConfirmation) AuthStep.Confirm else initialStep
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
        if (
            session.status == SessionStatus.SignedOut &&
            step == AuthStep.Confirm &&
            !session.isErrorMessage &&
            !session.message.isNullOrBlank()
        ) {
            step = AuthStep.Login
            loginEmail = session.pendingConfirmationEmail ?: registerEmail.trim()
            loginPassword = ""
            confirmationCode = ""
        }
        if (!session.pendingPasswordResetEmail.isNullOrBlank()) {
            step = AuthStep.ResetPassword
            resetEmail = session.pendingPasswordResetEmail.orEmpty()
        }
    }

    val isBusy = session.status == SessionStatus.Loading
    val l10n = sanctuaryStrings()
    val introTitle = if (embedded) l10n.t("auth.account") else l10n.t("auth.androidTitle")
    val introBody = if (embedded) {
        l10n.t("auth.accountBody")
    } else {
        l10n.t("auth.androidIntro")
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
    val l10n = sanctuaryStrings()
    val loginReady = loginEmail.trim().isNotEmpty() && loginPassword.isNotEmpty()
    val registerRules = passwordRules(registerPassword, l10n)
    val registerPasswordsMatch = passwordsMatch(registerPassword, registerPasswordConfirmation)
    val canSubmitRegistration =
        firstName.trim().isNotEmpty() &&
            lastName.trim().isNotEmpty() &&
            registerEmail.trim().isNotEmpty() &&
            registerRules.all { it.met } &&
            registerPasswordsMatch
    val resetRules = passwordRules(newPassword, l10n)
    val resetPasswordsMatch = passwordsMatch(newPassword, resetPasswordConfirmation)
    val canSubmitReset =
        resetEmail.trim().isNotEmpty() &&
            resetCode.trim().isNotEmpty() &&
            resetRules.all { it.met } &&
            resetPasswordsMatch

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (showAccountEyebrow) {
            Text(
                text = l10n.t("auth.account"),
                color = Color(0xFF7AC8EA),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Text(
            text = when (step) {
                AuthStep.Landing -> introTitle
                AuthStep.Login -> l10n.t("auth.signInCalmly")
                AuthStep.Register -> l10n.t("auth.createAccount")
                AuthStep.Confirm -> l10n.t("auth.confirmAccount")
                AuthStep.ForgotPassword -> l10n.t("auth.resetCalmly")
                AuthStep.ResetPassword -> l10n.t("auth.chooseNewPassword")
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
                    Text(l10n.t("auth.back"))
                }
                TextFieldBlock(l10n.t("auth.email"), loginEmail, keyboardType = KeyboardType.Email, onValueChange = onLoginEmailChange)
                TextFieldBlock(l10n.t("auth.password"), loginPassword, secure = true, onValueChange = onLoginPasswordChange)
                PrimaryButton(l10n.t("auth.login"), isBusy, enabled = loginReady) {
                    onAction.login(loginEmail.trim(), loginPassword)
                }
                TextButton(onClick = { onStepChange(AuthStep.ForgotPassword) }, enabled = !isBusy) {
                    Text(l10n.t("auth.forgotPassword"))
                }
            }

            AuthStep.Register -> AuthCard {
                TextButton(onClick = { onStepChange(AuthStep.Landing) }, enabled = !isBusy) {
                    Text(l10n.t("auth.back"))
                }
                TextFieldBlock(l10n.t("auth.firstName"), firstName, onValueChange = onFirstNameChange)
                TextFieldBlock(l10n.t("auth.lastName"), lastName, onValueChange = onLastNameChange)
                TextFieldBlock(l10n.t("auth.email"), registerEmail, keyboardType = KeyboardType.Email, onValueChange = onRegisterEmailChange)
                TextFieldBlock(l10n.t("auth.password"), registerPassword, secure = true, onValueChange = onRegisterPasswordChange)
                TextFieldBlock(l10n.t("auth.confirmPassword"), registerPasswordConfirmation, secure = true, onValueChange = onRegisterPasswordConfirmationChange)
                PasswordPanel(
                    rules = registerRules,
                    strengthLabel = passwordStrengthLabel(registerRules, l10n),
                    matches = registerPasswordsMatch,
                    confirmationWarning = l10n.t("auth.passwordsMustMatchCreate")
                )
                PrimaryButton(l10n.t("auth.createAccountCta"), isBusy, enabled = canSubmitRegistration) {
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
                    Text(l10n.t("auth.back"))
                }
                Text(
                    text = "${l10n.t("auth.confirmationSentPrefix")} ${session.pendingConfirmationEmail ?: registerEmail.trim()}.",
                    color = Color(0xFFD0DFEA)
                )
                TextFieldBlock(l10n.t("auth.verificationCode"), confirmationCode, keyboardType = KeyboardType.Number, onValueChange = onConfirmationCodeChange)
                PrimaryButton(l10n.t("auth.confirmAccountCta"), isBusy, enabled = confirmationCode.trim().isNotEmpty()) {
                    onAction.confirmRegistration(confirmationCode.trim(), registerPassword)
                }
                SecondaryButton(l10n.t("auth.sendNewCode"), isBusy, enabled = true, onClick = onAction::resendConfirmation)
            }

            AuthStep.ForgotPassword -> AuthCard {
                TextButton(onClick = { onStepChange(AuthStep.Login) }, enabled = !isBusy) {
                    Text(l10n.t("auth.back"))
                }
                TextFieldBlock(l10n.t("auth.email"), forgotEmail, keyboardType = KeyboardType.Email, onValueChange = onForgotEmailChange)
                PrimaryButton(l10n.t("auth.sendResetCode"), isBusy, enabled = forgotEmail.trim().isNotEmpty()) {
                    onAction.forgotPassword(forgotEmail.trim())
                }
            }

            AuthStep.ResetPassword -> AuthCard {
                TextButton(onClick = { onStepChange(AuthStep.Login) }, enabled = !isBusy) {
                    Text(l10n.t("auth.back"))
                }
                Text(
                    text = "${l10n.t("auth.resetSentPrefix")} ${session.pendingPasswordResetEmail ?: forgotEmail.trim()}.",
                    color = Color(0xFFD0DFEA)
                )
                TextFieldBlock(l10n.t("auth.email"), resetEmail, keyboardType = KeyboardType.Email, onValueChange = onResetEmailChange)
                TextFieldBlock(l10n.t("auth.resetCode"), resetCode, keyboardType = KeyboardType.Number, onValueChange = onResetCodeChange)
                TextFieldBlock(l10n.t("auth.newPassword"), newPassword, secure = true, onValueChange = onNewPasswordChange)
                TextFieldBlock(l10n.t("auth.confirmNewPassword"), resetPasswordConfirmation, secure = true, onValueChange = onResetPasswordConfirmationChange)
                PasswordPanel(
                    rules = resetRules,
                    strengthLabel = passwordStrengthLabel(resetRules, l10n),
                    matches = resetPasswordsMatch,
                    confirmationWarning = l10n.t("auth.passwordsMustMatchSave")
                )
                PrimaryButton(l10n.t("auth.saveNewPassword"), isBusy, enabled = canSubmitReset) {
                    onAction.resetPassword(resetEmail.trim(), resetCode.trim(), newPassword)
                }
                SecondaryButton(l10n.t("auth.sendNewResetCode"), isBusy, enabled = resetEmail.trim().isNotEmpty()) {
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
    intentionTerms: TermSearchUiState,
    patronageTerms: TermSearchUiState,
    prayers: ContentListUiState<PrayerSummary>,
    rosaries: ContentListUiState<PrayerSummary>,
    selectedLanguage: AppLanguage,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onShowLanguagePicker: () -> Unit,
    onAction: MainViewModel,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSaintQueryChanged: (String) -> Unit,
    onNovenaQueryChanged: (String) -> Unit,
    onIntentionTermQueryChanged: (String) -> Unit,
    onPatronageTermQueryChanged: (String) -> Unit,
    onPrayerQueryChanged: (String) -> Unit,
    onRosaryQueryChanged: (String) -> Unit,
    onReloadSaints: () -> Unit,
    onReloadNovenas: () -> Unit,
    onReloadIntentionTerms: () -> Unit,
    onReloadPatronageTerms: () -> Unit,
    onReloadPrayers: () -> Unit,
    onReloadRosaries: () -> Unit,
    onShowSaints: () -> Unit,
    onShowNovenas: () -> Unit,
    saintDetail: ContentDetailUiState<SaintDetail>,
    novenaDetail: ContentDetailUiState<app.sanctuary.android.data.NovenaDetail>,
    prayerDetail: ContentDetailUiState<PrayerDetail>,
    novenaProgress: NovenaProgressUiState,
    onOpenSaint: (String) -> Unit,
    onOpenNovena: (String) -> Unit,
    onOpenPrayer: (String) -> Unit,
    onSelectIntentionTerm: (SearchTerm, (String) -> Unit) -> Unit,
    onSelectPatronageTerm: (SearchTerm, (String) -> Unit) -> Unit,
    onClearSelectedIntentionTerm: () -> Unit,
    onClearSelectedPatronageTerm: () -> Unit,
    onCloseSaintDetail: () -> Unit,
    onCloseNovenaDetail: () -> Unit,
    onClosePrayerDetail: () -> Unit,
    onStartNovena: (String) -> Unit,
    onStopNovena: (String) -> Unit,
    onCompleteNovenaDay: (String, Int) -> Unit,
    onToggleFavorite: (FavoriteItemType, String, String?, String?, Int?) -> Unit,
    onUpdateReminderPreferences: (Boolean, Boolean) -> Unit,
    fetchSaintsInRange: suspend (String, String) -> List<app.sanctuary.android.data.SaintDateGroup>,
    fetchNovenasInRange: suspend (String, String) -> List<app.sanctuary.android.data.NovenaCalendarDate>,
    fetchLiturgicalRange: suspend (String, String) -> List<app.sanctuary.android.data.LiturgicalDay>
) {
    val l10n = sanctuaryStrings()
    val context = LocalContext.current
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showSaintSearch by rememberSaveable { mutableStateOf(false) }
    var showNovenaSearch by rememberSaveable { mutableStateOf(false) }
    var showIntentionsSearch by rememberSaveable { mutableStateOf(false) }
    var showPatronageSearch by rememberSaveable { mutableStateOf(false) }
    var showPrayerSearch by rememberSaveable { mutableStateOf(false) }
    var showRosarySearch by rememberSaveable { mutableStateOf(false) }
    var dailyReadingsUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var dailyReadingError by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoadingDailyReadings by rememberSaveable { mutableStateOf(false) }
    var aboutDocument by rememberSaveable { mutableStateOf<AboutDocument?>(null) }
    var showAccountRequiredPrompt by rememberSaveable { mutableStateOf(false) }
    var requestedAuthStep by rememberSaveable { mutableStateOf<AuthStep?>(null) }
    var saintsCalendarMode by rememberSaveable { mutableStateOf(CalendarMode.Day) }
    var novenasCalendarMode by rememberSaveable { mutableStateOf(CalendarMode.Day) }
    fun openSupportEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("info@mydailysanctuary.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Sanctuary support")
        }
        try {
            context.startActivity(Intent.createChooser(intent, l10n.t("about.emailSupport")))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, l10n.t("about.emailUnavailable"), Toast.LENGTH_LONG).show()
        }
    }
    var liturgicalCalendarMode by rememberSaveable { mutableStateOf(CalendarMode.Month) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(session.status) {
        if (session.status == SessionStatus.Authenticated) {
            requestedAuthStep = null
        }
    }

    fun requireAccount() {
        showAccountRequiredPrompt = true
    }

    fun openAccountAccess(step: AuthStep) {
        showAccountRequiredPrompt = false
        onCloseSaintDetail()
        onCloseNovenaDetail()
        onClosePrayerDetail()
        requestedAuthStep = step
        onTabSelected(AppTab.Me)
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .shadow(18.dp, RoundedCornerShape(26.dp), clip = false),
                shape = RoundedCornerShape(26.dp),
                color = SanctuaryTabBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, SanctuaryTabBorder.copy(alpha = 0.55f))
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { onTabSelected(tab) },
                            label = {
                                Text(
                                    tab.label(l10n),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp,
                                    lineHeight = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SanctuaryTabActive,
                                selectedTextColor = SanctuaryTabActive,
                                indicatorColor = SanctuaryTabActive.copy(alpha = 0.16f),
                                unselectedIconColor = SanctuaryTabInactive,
                                unselectedTextColor = SanctuaryTabInactive
                            ),
                            icon = {
                                Icon(
                                    imageVector = tab.icon(),
                                    contentDescription = tab.label(l10n)
                                )
                            }
                        )
                    }
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
                            language = selectedLanguage,
                            onShowAbout = { showAbout = true }
                            ,
                            onShowLanguage = onShowLanguagePicker
                        )
                    }
                    item {
                        HomeHeroCard(session)
                    }
                    item {
                        HomeFeatureCard(
                            action = HomeAction.Daily,
                            onClick = {
                                isLoadingDailyReadings = true
                                scope.launch {
                                    val today = LocalDate.now().toString()
                                    runCatching { fetchLiturgicalRange(today, today) }
                                        .onSuccess { days ->
                                            isLoadingDailyReadings = false
                                            val readingsUrl = days.firstOrNull()?.readingsUrl
                                            if (!readingsUrl.isNullOrBlank()) {
                                                dailyReadingsUrl = readingsUrl
                                            } else {
                                                dailyReadingError = l10n.t("calendar.dailyReadingsMissing")
                                            }
                                        }
                                        .onFailure {
                                            isLoadingDailyReadings = false
                                            dailyReadingError = it.message ?: l10n.t("calendar.dailyReadingsOpenError")
                                        }
                                }
                            }
                        )
                    }
                    item {
                        HomeFeatureCard(
                            action = HomeAction.Prayers,
                            onClick = {
                                showPrayerSearch = true
                                if (prayers.items.isEmpty() && !prayers.isLoading) {
                                    onReloadPrayers()
                                }
                            }
                        )
                    }
                    item {
                        HomeFeatureCard(
                            action = HomeAction.Patronage,
                            onClick = {
                                showPatronageSearch = true
                                if (patronageTerms.terms.isEmpty() && !patronageTerms.isLoading) {
                                    onReloadPatronageTerms()
                                }
                            }
                        )
                    }
                    item {
                        HomeFeatureCard(
                            action = HomeAction.Intentions,
                            onClick = {
                                showIntentionsSearch = true
                                if (intentionTerms.terms.isEmpty() && !intentionTerms.isLoading) {
                                    onReloadIntentionTerms()
                                }
                            }
                        )
                    }
                    item {
                        HomeFeatureCard(
                            action = HomeAction.Rosary,
                            onClick = {
                                showRosarySearch = true
                                if (rosaries.items.isEmpty() && !rosaries.isLoading) {
                                    onReloadRosaries()
                                }
                            }
                        )
                    }
                }

                AppTab.Novenas -> {
                    item {
                        Box(modifier = Modifier.fillParentMaxHeight()) {
                            NovenasCalendarScreen(
                                mode = novenasCalendarMode,
                                onModeChange = { novenasCalendarMode = it },
                                onSearch = { showNovenaSearch = true },
                                onOpenNovena = onOpenNovena,
                                fetchNovenasInRange = fetchNovenasInRange,
                                fetchLiturgicalRange = fetchLiturgicalRange
                            )
                        }
                    }
                }

                AppTab.Liturgical -> {
                    item {
                        Box(modifier = Modifier.fillParentMaxHeight()) {
                            LiturgicalCalendarScreen(
                                mode = liturgicalCalendarMode,
                                onModeChange = { liturgicalCalendarMode = it },
                                fetchLiturgicalRange = fetchLiturgicalRange,
                                onOpenReadings = { dailyReadingsUrl = it }
                            )
                        }
                    }
                }

                AppTab.Saints -> {
                    item {
                        Box(modifier = Modifier.fillParentMaxHeight()) {
                            SaintsCalendarScreen(
                                mode = saintsCalendarMode,
                                onModeChange = { saintsCalendarMode = it },
                                onSearch = { showSaintSearch = true },
                                onOpenSaint = onOpenSaint,
                                fetchSaintsInRange = fetchSaintsInRange,
                                fetchLiturgicalRange = fetchLiturgicalRange
                            )
                        }
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
                                onOpenPrayer = onOpenPrayer,
                                onLogout = onLogout,
                                onDeleteAccount = onDeleteAccount,
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
                                embedded = true,
                                initialStep = requestedAuthStep ?: AuthStep.Landing
                            )
                        }
                    }
                }
            }
        }

        if (showAbout) {
            SanctuaryModalSheet(onDismissRequest = { showAbout = false }) {
                AboutOverviewSheet(
                    onOpenDesktop = { dailyReadingsUrl = "https://mydailysanctuary.com" },
                    onOpenUsccb = { dailyReadingsUrl = "https://bible.usccb.org/daily-bible-reading" },
                    onOpenWikipedia = { dailyReadingsUrl = "https://www.wikipedia.org/" },
                    onOpenSupport = { aboutDocument = AboutDocument.Support },
                    onOpenPrivacy = { aboutDocument = AboutDocument.Privacy },
                    onEmailSupport = { openSupportEmail() }
                )
            }
        }

        aboutDocument?.let { document ->
            SanctuaryModalSheet(onDismissRequest = { aboutDocument = null }) {
                AboutDocumentSheet(
                    document = document,
                    onEmailSupport = { openSupportEmail() }
                )
            }
        }

        dailyReadingError?.let { message ->
            SanctuaryModalSheet(onDismissRequest = { dailyReadingError = null }) {
                DetailErrorSheet(message = message, onDismiss = { dailyReadingError = null })
            }
        }

        if (isLoadingDailyReadings) {
            SanctuaryModalSheet(onDismissRequest = { isLoadingDailyReadings = false }) {
                DetailLoadingSheet(l10n.t("common.loading"))
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
                    title = l10n.t("search.saintsTitle"),
                    query = saints.query,
                    onQueryChanged = onSaintQueryChanged,
                    onSubmit = onReloadSaints,
                    isLoading = saints.isLoading,
                    error = saints.error,
                    emptyLabel = l10n.t("search.saintsTitle"),
                    items = saints.items
                ) { item ->
                    ContentCard(
                        title = item.name,
                        subtitle = item.summary ?: l10n.t("home.saintsSubtitle"),
                        detail = item.feastLabel,
                        imageUrl = item.imageUrl,
                        onClick = {
                            onOpenSaint(item.slug)
                        }
                    )
                }
            }
        }

        if (showNovenaSearch) {
            SanctuaryModalSheet(onDismissRequest = { showNovenaSearch = false }) {
                SearchListSheet(
                    title = l10n.t("search.novenasTitle"),
                    query = novenas.query,
                    onQueryChanged = onNovenaQueryChanged,
                    onSubmit = onReloadNovenas,
                    isLoading = novenas.isLoading,
                    error = novenas.error,
                    emptyLabel = l10n.t("search.novenasTitle"),
                    items = novenas.items
                ) { item ->
                    ContentCard(
                        title = item.title,
                        subtitle = item.description,
                        detail = "${item.durationDays}-day novena",
                        imageUrl = item.imageUrl,
                        onClick = {
                            onOpenNovena(item.slug)
                        }
                    )
                }
            }
        }

        if (showPrayerSearch) {
            SanctuaryModalSheet(onDismissRequest = { showPrayerSearch = false }) {
                SearchListSheet(
                    title = l10n.t("search.prayersTitle"),
                    query = prayers.query,
                    onQueryChanged = onPrayerQueryChanged,
                    onSubmit = onReloadPrayers,
                    isLoading = prayers.isLoading,
                    error = prayers.error,
                    emptyLabel = l10n.t("search.prayersTitle"),
                    items = prayers.items
                ) { item ->
                    ContentCard(
                        title = item.title,
                        subtitle = item.bodyPreview,
                        detail = visiblePrayerCategory(item.category),
                        imageUrl = item.imageUrl,
                        onClick = {
                            onOpenPrayer(item.slug)
                        }
                    )
                }
            }
        }

        if (showRosarySearch) {
            SanctuaryModalSheet(onDismissRequest = { showRosarySearch = false }) {
                SearchListSheet(
                    title = l10n.t("search.rosaryTitle"),
                    query = rosaries.query,
                    onQueryChanged = onRosaryQueryChanged,
                    onSubmit = onReloadRosaries,
                    isLoading = rosaries.isLoading,
                    error = rosaries.error,
                    emptyLabel = l10n.t("search.rosaryTitle"),
                    items = rosaries.items
                ) { item ->
                    ContentCard(
                        title = item.title,
                        subtitle = null,
                        detail = item.bodyPreview,
                        imageUrl = item.imageUrl,
                        onClick = {
                            onOpenPrayer(item.slug)
                        }
                    )
                }
            }
        }

        if (showIntentionsSearch) {
            fun closeIntentionsSearch() {
                showIntentionsSearch = false
                onClearSelectedIntentionTerm()
            }

            SanctuaryModalSheet(onDismissRequest = { closeIntentionsSearch() }) {
                TermSearchSheet(
                    title = l10n.t("search.intentionsTitle"),
                    query = intentionTerms.query,
                    prompt = l10n.t("search.intentionsPrompt"),
                    state = intentionTerms,
                    mode = TermSearchMode.Intentions,
                    onQueryChanged = onIntentionTermQueryChanged,
                    onSubmit = onReloadIntentionTerms,
                    onClearSelectedTerm = onClearSelectedIntentionTerm,
                    onSelectTerm = { term ->
                        onSelectIntentionTerm(term) {
                            onOpenNovena(it)
                        }
                    },
                    onOpenSaint = {
                        onOpenSaint(it)
                    },
                    onOpenNovena = {
                        onOpenNovena(it)
                    }
                )
            }
        }

        if (showPatronageSearch) {
            fun closePatronageSearch() {
                showPatronageSearch = false
                onClearSelectedPatronageTerm()
            }

            SanctuaryModalSheet(onDismissRequest = { closePatronageSearch() }) {
                TermSearchSheet(
                    title = l10n.t("search.patronageTitle"),
                    query = patronageTerms.query,
                    prompt = l10n.t("search.patronagePrompt"),
                    state = patronageTerms,
                    mode = TermSearchMode.Patronage,
                    onQueryChanged = onPatronageTermQueryChanged,
                    onSubmit = onReloadPatronageTerms,
                    onClearSelectedTerm = onClearSelectedPatronageTerm,
                    onSelectTerm = { term ->
                        onSelectPatronageTerm(term) {
                            onOpenSaint(it)
                        }
                    },
                    onOpenSaint = {
                        onOpenSaint(it)
                    },
                    onOpenNovena = {
                        onOpenNovena(it)
                    }
                )
            }
        }

        if (saintDetail.isLoading || saintDetail.item != null || saintDetail.error != null) {
            SanctuaryModalSheet(onDismissRequest = onCloseSaintDetail) {
                when {
                    saintDetail.isLoading -> DetailLoadingSheet(l10n.t("common.loading"))
                    saintDetail.error != null -> DetailErrorSheet(saintDetail.error, onCloseSaintDetail)
                    saintDetail.item != null -> SaintDetailSheet(
                        detail = saintDetail.item,
                        session = session,
                        progress = novenaProgress,
                        onToggleFavorite = onToggleFavorite,
                        onRequireAccount = ::requireAccount,
                        onDismiss = onCloseSaintDetail
                    )
                }
            }
        }

        if (novenaDetail.isLoading || novenaDetail.item != null || novenaDetail.error != null) {
            SanctuaryModalSheet(onDismissRequest = onCloseNovenaDetail) {
                when {
                    novenaDetail.isLoading -> DetailLoadingSheet(l10n.t("common.loading"))
                    novenaDetail.error != null -> DetailErrorSheet(novenaDetail.error, onCloseNovenaDetail)
                    novenaDetail.item != null -> NovenaDetailSheet(
                        detail = novenaDetail.item,
                        session = session,
                        progress = novenaProgress,
                        onStart = onStartNovena,
                        onStop = onStopNovena,
                        onCompleteDay = onCompleteNovenaDay,
                        onToggleFavorite = onToggleFavorite,
                        onRequireAccount = ::requireAccount,
                        onDismiss = onCloseNovenaDetail
                    )
                }
            }
        }

        if (prayerDetail.isLoading || prayerDetail.item != null || prayerDetail.error != null) {
            SanctuaryModalSheet(onDismissRequest = onClosePrayerDetail) {
                when {
                    prayerDetail.isLoading -> DetailLoadingSheet(l10n.t("common.loading"))
                    prayerDetail.error != null -> DetailErrorSheet(prayerDetail.error, onClosePrayerDetail)
                    prayerDetail.item != null -> PrayerDetailSheet(
                        detail = prayerDetail.item,
                        session = session,
                        progress = novenaProgress,
                        onToggleFavorite = onToggleFavorite,
                        onRequireAccount = ::requireAccount,
                        onDismiss = onClosePrayerDetail
                    )
                }
            }
        }

        if (showAccountRequiredPrompt) {
            AlertDialog(
                onDismissRequest = { showAccountRequiredPrompt = false },
                title = { Text(l10n.t("accountRequired.title")) },
                text = { Text(l10n.t("accountRequired.body")) },
                confirmButton = {
                    TextButton(onClick = { openAccountAccess(AuthStep.Register) }) {
                        Text(l10n.t("accountRequired.createAccount"))
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { openAccountAccess(AuthStep.Login) }) {
                            Text(l10n.t("accountRequired.signIn"))
                        }
                        TextButton(onClick = { showAccountRequiredPrompt = false }) {
                            Text(l10n.t("accountRequired.dismiss"))
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ChoiceStack(
    isBusy: Boolean,
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {
    val l10n = sanctuaryStrings()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceCard(
            eyebrow = l10n.t("auth.returning"),
            title = l10n.t("auth.login"),
            body = l10n.t("auth.returningBody"),
            enabled = !isBusy,
            onClick = onLogin
        )
        ChoiceCard(
            eyebrow = l10n.t("auth.newToSanctuary"),
            title = l10n.t("auth.register"),
            body = l10n.t("auth.newToSanctuaryBody"),
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
    val l10n = sanctuaryStrings()
    Card(
        modifier = Modifier.sanctuaryCardShadow(),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCardElevated),
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
        modifier = Modifier.sanctuaryCardShadow(),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCardElevated),
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
        modifier = Modifier.sanctuaryCardShadow(RoundedCornerShape(18.dp)),
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
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val visualTransformation = when {
        secure && !passwordVisible -> PasswordVisualTransformation()
        else -> VisualTransformation.None
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        visualTransformation = visualTransformation,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Cancel,
                            contentDescription = "Clear ${label.lowercase()}"
                        )
                    }
                }
                if (secure) {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide ${label.lowercase()}" else "Show ${label.lowercase()}"
                        )
                    }
                }
            }
        },
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

private fun passwordRules(password: String, l10n: SanctuaryStrings): List<PasswordRuleUi> = listOf(
    PasswordRuleUi(l10n.t("auth.rule.length"), password.length >= 10),
    PasswordRuleUi(l10n.t("auth.rule.upper"), password.any(Char::isUpperCase)),
    PasswordRuleUi(l10n.t("auth.rule.lower"), password.any(Char::isLowerCase)),
    PasswordRuleUi(l10n.t("auth.rule.number"), password.any(Char::isDigit))
)

private fun passwordsMatch(password: String, confirmation: String): Boolean =
    confirmation.isNotEmpty() && password == confirmation

private fun passwordStrengthLabel(rules: List<PasswordRuleUi>, l10n: SanctuaryStrings): String {
    val metCount = rules.count { it.met }
    return when {
        metCount == rules.size -> l10n.t("auth.passwordStrengthReady")
        metCount >= 4 -> l10n.t("auth.passwordStrengthAlmost")
        metCount >= 2 -> l10n.t("auth.passwordStrengthNeedsWork")
        else -> l10n.t("auth.passwordStrengthTooWeak")
    }
}

@Composable
private fun PasswordPanel(
    rules: List<PasswordRuleUi>,
    strengthLabel: String,
    matches: Boolean,
    confirmationWarning: String
) {
    val l10n = sanctuaryStrings()
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
                    text = l10n.t("auth.passwordStrength"),
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
                text = if (matches) l10n.t("auth.passwordsMatch") else confirmationWarning,
                color = if (matches) Color(0xFF7AC8EA) else Color(0xFFD0DFEA)
            )
        }
    }
}

@Composable
private fun HomeCard(session: SessionUiState) {
    val l10n = sanctuaryStrings()
    val profile = session.profile
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = profile?.displayName ?: session.session?.displayName ?: l10n.t("me.accountFallback"),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = session.session?.email.orEmpty(),
                color = Color(0xFFD0DFEA)
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProfileMetric(l10n.t("me.metric.activeNovenas"), profile?.activeNovenaCount ?: 0)
            ProfileMetric(l10n.t("me.metric.favoriteNovenas"), profile?.favoriteNovenaCount ?: 0)
            ProfileMetric(l10n.t("me.metric.favoriteSaints"), profile?.favoriteSaintCount ?: 0)
            ProfileMetric(l10n.t("me.metric.favoritePrayers"), profile?.favoritePrayerCount ?: 0)
            if (BuildConfig.ENVIRONMENT != "prod") {
                ProfileMetric(l10n.t("me.metric.environment"), BuildConfig.ENVIRONMENT.uppercase())
            }
        }
    }
}

@Composable
private fun MeScreen(
    session: SessionUiState,
    progress: NovenaProgressUiState,
    onOpenNovena: (String) -> Unit,
    onOpenSaint: (String) -> Unit,
    onOpenPrayer: (String) -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onUpdateReminderPreferences: (Boolean, Boolean) -> Unit
) {
    val l10n = sanctuaryStrings()
    val context = LocalContext.current
    val profile = session.profile
    val favoriteNovenas = progress.favorites.filter { it.itemType == FavoriteItemType.Novena }
    val favoriteSaints = progress.favorites.filter { it.itemType == FavoriteItemType.Saint }
    val favoritePrayers = progress.favorites.filter { it.itemType == FavoriteItemType.Prayer }
    val novenaReminderToggle = profile?.novenaRemindersEnabled == true
    val dailyReminderToggle = profile?.feastRemindersEnabled == true
    var pendingReminderUpdate by remember { mutableStateOf<Pair<Boolean, Boolean>?>(null) }
    var showDeleteAccountDialog by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val update = pendingReminderUpdate
        pendingReminderUpdate = null
        if (granted && update != null) {
            onUpdateReminderPreferences(update.first, update.second)
        }
    }

    fun notificationsPermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun requestReminderUpdate(nextNovenaEnabled: Boolean, nextDailyEnabled: Boolean) {
        val needsPermission = (nextNovenaEnabled || nextDailyEnabled) && notificationsPermissionRequired()
        if (needsPermission) {
            pendingReminderUpdate = nextNovenaEnabled to nextDailyEnabled
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onUpdateReminderPreferences(nextNovenaEnabled, nextDailyEnabled)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(l10n.t("me.title"), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
        Text(
            l10n.t("me.subtitle"),
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
                Text(l10n.t("me.signedIn"), color = Color(0xFF7AC8EA), fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                            profile?.displayName ?: session.session?.displayName ?: l10n.t("me.accountFallback"),
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
                            l10n.t("me.accountSummary"),
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
                    Text(l10n.t("me.logout"), color = Color.White)
                }
            }
        }

        MeSectionCard(title = l10n.t("me.reminders")) {
            ReminderToggleRow(
                title = l10n.t("reminder.novenas"),
                subtitle = l10n.t("reminder.novenasBody"),
                checked = novenaReminderToggle,
                enabled = !session.isSavingReminderPreferences,
                onCheckedChange = { checked ->
                    requestReminderUpdate(checked, dailyReminderToggle)
                }
            )
            ReminderToggleRow(
                title = l10n.t("reminder.daily"),
                subtitle = l10n.t("reminder.dailyBody"),
                checked = dailyReminderToggle,
                enabled = !session.isSavingReminderPreferences,
                onCheckedChange = { checked ->
                    requestReminderUpdate(novenaReminderToggle, checked)
                }
            )
        }

        MeSectionCard(title = l10n.t("me.inProgress")) {
            if (progress.commitments.none { it.status == CommitmentStatus.Active }) {
                Text(l10n.t("me.noInProgress"), color = Color(0xFFD0DFEA))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    progress.commitments
                        .filter { it.status == CommitmentStatus.Active }
                        .sortedByDescending { it.updatedAt }
                        .forEach { commitment ->
                            LinkedMeRow(
                                title = progress.novenaTitles[commitment.novenaId]
                                    ?: commitment.novenaId.replace("_", " ").replace("-", " ").replaceFirstChar { it.uppercase() },
                                subtitle = "${l10n.t("calendar.dayNumberPrefix")} ${commitment.currentDay} / ${progress.novenaDurations[commitment.novenaId] ?: 9}",
                                onClick = { onOpenNovena(commitment.novenaId) }
                            )
                        }
                }
            }
        }

        MeSectionCard(title = l10n.t("me.favoriteNovenas")) {
            if (favoriteNovenas.isEmpty()) {
                Text(l10n.t("me.noneFavoriteNovenas"), color = Color(0xFFD0DFEA))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    favoriteNovenas.forEach { favorite ->
                        LinkedMeRow(
                            title = progress.novenaTitles[favorite.itemId]
                                ?: favorite.itemId.replace("_", " ").replace("-", " ").replaceFirstChar { it.uppercase() },
                            subtitle = null,
                            onClick = { onOpenNovena(favorite.itemId) }
                        )
                    }
                }
            }
        }

        MeSectionCard(title = l10n.t("me.favoriteSaints")) {
            if (favoriteSaints.isEmpty()) {
                Text(l10n.t("me.noneFavoriteSaints"), color = Color(0xFFD0DFEA))
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

        MeSectionCard(title = l10n.t("me.favoritePrayers")) {
            if (favoritePrayers.isEmpty()) {
                Text(l10n.t("me.noneFavoritePrayers"), color = Color(0xFFD0DFEA))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    favoritePrayers.forEach { favorite ->
                        LinkedMeRow(
                            title = progress.prayerTitles[favorite.itemId]
                                ?: favorite.itemId.replace("_", " ").replace("-", " ").replaceFirstChar { it.uppercase() },
                            subtitle = null,
                            onClick = { onOpenPrayer(progress.prayerSlugs[favorite.itemId] ?: favorite.itemId) }
                        )
                    }
                }
            }
        }

        MeSectionCard(title = l10n.t("me.deleteAccount.title")) {
            Text(
                l10n.t("me.deleteAccount.body"),
                color = Color(0xFFD0DFEA),
                lineHeight = 20.sp
            )
            Button(
                onClick = { showDeleteAccountDialog = true },
                enabled = !session.isDeletingAccount,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C2630)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (session.isDeletingAccount) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 8.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
                Text(
                    l10n.t(if (session.isDeletingAccount) "me.deleteAccount.deleting" else "me.deleteAccount.action"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = {
                Text(l10n.t("me.deleteAccount.confirmTitle"))
            },
            text = {
                Text(l10n.t("me.deleteAccount.confirmBody"))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccount()
                    }
                ) {
                    Text(l10n.t("me.deleteAccount.confirmAction"), color = Color(0xFFB3261E))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(l10n.t("common.cancel"))
                }
            },
            containerColor = Color(0xFF172D3C),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFD0DFEA)
        )
    }
}

@Composable
private fun MeSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.sanctuaryCardShadow(),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCardElevated),
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
    val l10n = sanctuaryStrings()
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
                text = l10n.t("home.eyebrow"),
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF7AC8EA),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .size(156.dp),
                contentAlignment = Alignment.Center
            ) {
                BrandLogoMark(size = 132.dp, corner = 30.dp, glowExtra = 44.dp)
            }
            Text(
                text = l10n.t("home.welcome"),
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = l10n.t("home.connect"),
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFE7F2FA),
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = l10n.t("home.supporting"),
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFD0DFEA),
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AboutOverviewSheet(
    onOpenDesktop: () -> Unit,
    onOpenUsccb: () -> Unit,
    onOpenWikipedia: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onEmailSupport: () -> Unit
) {
    val l10n = sanctuaryStrings()
    DetailSheetScaffold(
        title = l10n.t("about.title"),
        subtitle = l10n.t("about.subtitle")
    ) {
        AboutInfoCard(title = l10n.t("about.brand")) {
            Text(l10n.t("about.brand"), color = Color(0xFF7AC8EA), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(l10n.t("about.title"), color = Color.White, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
            Text(
                l10n.t("about.subtitle"),
                color = Color(0xFFD0DFEA),
                lineHeight = 21.sp
            )
        }

        AboutInfoCard(title = l10n.t("about.desktopVersion")) {
            Text(
                l10n.t("about.desktopBody"),
                color = Color(0xFFD0DFEA),
                lineHeight = 21.sp
            )
            PrimarySheetButton(title = l10n.t("about.link.desktop"), onClick = onOpenDesktop)
        }

        AboutInfoCard(title = l10n.t("about.versionTitle")) {
            Text(
                "${l10n.t("about.versionLabel")}: ${BuildConfig.VERSION_NAME}",
                color = Color(0xFFD0DFEA),
                lineHeight = 21.sp
            )
            Text(
                "${l10n.t("about.buildLabel")}: ${BuildConfig.VERSION_CODE}",
                color = Color(0xFFD0DFEA),
                lineHeight = 21.sp
            )
            if (BuildConfig.ENVIRONMENT != "prod") {
                Text(
                    "${l10n.t("about.environmentLabel")}: ${BuildConfig.ENVIRONMENT.uppercase()}",
                    color = Color(0xFFD0DFEA),
                    lineHeight = 21.sp
                )
            }
        }

        AboutInfoCard(title = l10n.t("about.whatsInApp")) {
            Text("• ${l10n.t("about.whatsInApp.liturgical")}", color = Color(0xFFD0DFEA), lineHeight = 21.sp)
            Text("• ${l10n.t("about.whatsInApp.saints")}", color = Color(0xFFD0DFEA), lineHeight = 21.sp)
            Text("• ${l10n.t("about.whatsInApp.novenas")}", color = Color(0xFFD0DFEA), lineHeight = 21.sp)
        }

        AboutInfoCard(title = l10n.t("about.references")) {
            Text(l10n.t("about.referencesBody"), color = Color(0xFFD0DFEA), lineHeight = 21.sp)
            Text("• ${l10n.t("about.reference.usccb")}", color = Color(0xFFD0DFEA))
            Text("• ${l10n.t("about.reference.wikipedia")}", color = Color(0xFFD0DFEA))
            PrimarySheetButton(title = l10n.t("about.link.usccb"), onClick = onOpenUsccb)
            PrimarySheetButton(title = l10n.t("about.link.wikipedia"), onClick = onOpenWikipedia)
        }

        AboutInfoCard(title = l10n.t("about.contact")) {
            Text(
                l10n.t("about.contactBody"),
                color = Color(0xFFD0DFEA),
                lineHeight = 21.sp
            )
            PrimarySheetButton(title = l10n.t("about.emailSupport"), onClick = onEmailSupport)
            SecondarySheetButton(title = l10n.t("about.link.support"), onClick = onOpenSupport)
            SecondarySheetButton(title = l10n.t("about.link.privacy"), onClick = onOpenPrivacy)
        }

        Text(
            l10n.t("about.copyright"),
            color = Color(0xFFD0DFEA),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
    }
}

@Composable
private fun AboutDocumentSheet(
    document: AboutDocument,
    onEmailSupport: () -> Unit
) {
    val l10n = sanctuaryStrings()
    val title = when (document) {
        AboutDocument.Support -> l10n.t("about.supportTitle")
        AboutDocument.Privacy -> l10n.t("about.privacyTitle")
    }
    val sections = when (document) {
        AboutDocument.Support -> listOf(
            l10n.t("about.support.section.help") to l10n.t("about.support.body.help"),
            l10n.t("about.support.section.features") to l10n.t("about.support.body.features"),
            l10n.t("about.support.section.response") to l10n.t("about.support.body.response")
        )
        AboutDocument.Privacy -> listOf(
            l10n.t("about.privacy.section.collect") to l10n.t("about.privacy.body.collect"),
            l10n.t("about.privacy.section.location") to l10n.t("about.privacy.body.location"),
            l10n.t("about.privacy.section.notifications") to l10n.t("about.privacy.body.notifications"),
            l10n.t("about.privacy.section.sharing") to l10n.t("about.privacy.body.sharing"),
            l10n.t("about.privacy.section.choices") to l10n.t("about.privacy.body.choices"),
            l10n.t("about.privacy.section.contact") to l10n.t("about.privacy.body.contact")
        )
    }

    DetailSheetScaffold(
        title = title,
        subtitle = if (document == AboutDocument.Privacy) l10n.t("about.privacySubtitle") else l10n.t("about.supportSubtitle")
    ) {
        sections.forEach { (sectionTitle, body) ->
            AboutInfoCard(title = sectionTitle) {
                Text(body, color = Color(0xFFD0DFEA), lineHeight = 21.sp)
            }
        }
        PrimarySheetButton(title = l10n.t("about.emailSupport"), onClick = onEmailSupport)
        Text(
            l10n.t("about.copyright"),
            color = Color(0xFFD0DFEA),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
    }
}

@Composable
private fun AboutInfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                content()
            }
        )
    }
}

@Composable
private fun PrimarySheetButton(title: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp), clip = false)
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF3E9FC1), Color(0xFF195E78))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecondarySheetButton(title: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF7CC7DE).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
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
            contentDescription = sanctuaryStrings().t("about.brand"),
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(corner))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(corner))
        )
    }
}

@Composable
private fun HomeTopActions(
    language: AppLanguage,
    onShowAbout: () -> Unit,
    onShowLanguage: () -> Unit
) {
    val l10n = sanctuaryStrings()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TopPillButton(
            modifier = Modifier.weight(1f),
            title = l10n.t("home.about"),
            icon = Icons.Filled.Info,
            onClick = onShowAbout
        )
        TopPillButton(
            modifier = Modifier.weight(1f),
            title = "${l10n.t("home.language")}: ${language.displayName}",
            icon = Icons.Filled.Language,
            onClick = onShowLanguage
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
        colors = CardDefaults.cardColors(containerColor = Color(0x1222394C)),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = Color(0xFFD0DFEA), modifier = Modifier.size(13.dp))
            }
            Text(title, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CalendarSearchButton(
    label: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(22.dp, shape, clip = false),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF55C0E1),
                            Color(0xFF2B8DAA),
                            Color(0xFF22394C)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.White.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LanguagePickerSheet(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit
) {
    val l10n = sanctuaryStrings()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = l10n.t("home.chooseLanguage"),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        AppLanguage.entries.forEach { language ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (language == current) Color(0x223CB8F2) else Color(0x1222394C)
                ),
                shape = RoundedCornerShape(18.dp),
                onClick = { onSelect(language) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(language.displayName, color = Color.White, fontWeight = FontWeight.SemiBold)
                    if (language == current) {
                        Text("✓", color = SanctuaryTabActive, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun HomeFeatureCard(
    action: HomeAction,
    onClick: () -> Unit
) {
    val l10n = sanctuaryStrings()
    val fontScale = LocalDensity.current.fontScale
    Card(
        modifier = Modifier.shadow(18.dp, RoundedCornerShape(28.dp), clip = false),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(28.dp),
        onClick = onClick
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 360.dp || fontScale >= 1.18f
            val cardHeight = if (compact) 148.dp else 188.dp
            val artworkWidth = if (maxWidth < 420.dp) 128.dp else 156.dp
            val artworkHeight = if (maxWidth < 420.dp) 88.dp else 108.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
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

                if (compact) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            HomeActionBadgeGlyph(
                                action = action,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                l10n.t(action.titleKey),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                lineHeight = 25.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                l10n.t(action.subtitleKey),
                                color = Color(0xFFD0DFEA),
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SouthEast,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 22.dp, top = 16.dp, end = 16.dp, bottom = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                HomeActionBadgeGlyph(
                                    action = action,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    l10n.t(action.titleKey),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    l10n.t(action.subtitleKey),
                                    color = Color(0xFFD0DFEA),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        HomeFeatureArtwork(
                            action = action,
                            contentDescription = l10n.t(action.titleKey),
                            modifier = Modifier.size(width = artworkWidth, height = artworkHeight)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeFeatureArtwork(
    action: HomeAction,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    brush = Brush.linearGradient(action.illustrationColors)
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
        )

        if (action.artworkAssetPath != null) {
            HomeActionArtwork(
                assetPath = action.artworkAssetPath,
                contentDescription = contentDescription,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)
            )
        } else {
            HomeActionIllustration(
                action = action,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            )
        }

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

@Composable
private fun HomeActionArtwork(
    assetPath: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(assetPath)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

private fun HomeAction.cardBrush(): Brush = when (this) {
    HomeAction.Saints, HomeAction.Patronage -> Brush.linearGradient(
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
    HomeAction.Rosary -> Brush.linearGradient(
        listOf(Color(0xFF30384F).copy(alpha = 0.92f), Color(0xFF123E4D).copy(alpha = 0.74f))
    )
    HomeAction.Intentions -> Brush.linearGradient(
        listOf(Color(0xFF4C3B56).copy(alpha = 0.90f), Color(0xFF15404B).copy(alpha = 0.74f))
    )
    HomeAction.Daily -> Brush.linearGradient(
        listOf(Color(0xFF1C514C).copy(alpha = 0.90f), Color(0xFF143B4D).copy(alpha = 0.74f))
    )
}

@Composable
private fun HomeActionBadgeGlyph(
    action: HomeAction,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val gold = Color(0xFFF2D37B)
        val cyan = Color(0xFF9DDFF4)
        val rose = Color(0xFFF2A8C4)
        val ivory = Color(0xFFF1E6CF)
        val navy = Color(0xFF173244)
        val stroke = size.minDimension * 0.09f
        val center = Offset(size.width / 2f, size.height / 2f)

        when (action) {
            HomeAction.Saints, HomeAction.Patronage -> {
                drawCircle(gold, radius = size.minDimension * 0.12f, center = Offset(size.width * 0.5f, size.height * 0.32f))
                drawCircle(navy, radius = size.minDimension * 0.06f, center = Offset(size.width * 0.5f, size.height * 0.25f))
                drawCircle(gold.copy(alpha = 0.85f), radius = size.minDimension * 0.09f, center = Offset(size.width * 0.28f, size.height * 0.44f))
                drawCircle(gold.copy(alpha = 0.85f), radius = size.minDimension * 0.09f, center = Offset(size.width * 0.72f, size.height * 0.44f))
                drawArc(
                    color = gold,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.13f, size.height * 0.45f),
                    size = Size(size.width * 0.74f, size.height * 0.34f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            HomeAction.Novenas -> {
                drawRoundRect(
                    color = Color(0xFF163245),
                    topLeft = Offset(size.width * 0.16f, size.height * 0.16f),
                    size = Size(size.width * 0.42f, size.height * 0.58f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.12f)
                )
                repeat(2) { index ->
                    drawLine(
                        color = cyan.copy(alpha = 0.65f),
                        start = Offset(size.width * 0.24f, size.height * (0.30f + 0.12f * index)),
                        end = Offset(size.width * 0.46f, size.height * (0.30f + 0.12f * index)),
                        strokeWidth = stroke * 0.8f,
                        cap = StrokeCap.Round
                    )
                }
                drawLine(gold, Offset(size.width * 0.36f, size.height * 0.54f), Offset(size.width * 0.36f, size.height * 0.72f), stroke, StrokeCap.Round)
                drawLine(gold, Offset(size.width * 0.27f, size.height * 0.63f), Offset(size.width * 0.45f, size.height * 0.63f), stroke, StrokeCap.Round)
                drawArc(
                    color = Color(0xFFD9C49B),
                    startAngle = -70f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.58f, size.height * 0.14f),
                    size = Size(size.width * 0.22f, size.height * 0.52f),
                    style = Stroke(width = stroke * 0.72f, cap = StrokeCap.Round)
                )
                listOf(0.0f, 0.12f, 0.24f, 0.36f).forEach { fraction ->
                    drawCircle(
                        color = gold,
                        radius = size.minDimension * 0.045f,
                        center = Offset(size.width * 0.73f, size.height * (0.20f + fraction))
                    )
                }
            }
            HomeAction.Liturgical -> {
                drawRoundRect(
                    color = Color(0xFFAEC7F0),
                    topLeft = Offset(size.width * 0.2f, size.height * 0.22f),
                    size = Size(size.width * 0.6f, size.height * 0.52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.12f)
                )
                drawRoundRect(
                    color = Color(0xFF6D88B8),
                    topLeft = Offset(size.width * 0.2f, size.height * 0.22f),
                    size = Size(size.width * 0.6f, size.height * 0.16f)
                )
                drawLine(Color(0xFF50698F), Offset(size.width * 0.32f, size.height * 0.16f), Offset(size.width * 0.32f, size.height * 0.30f), stroke * 0.8f, StrokeCap.Round)
                drawLine(Color(0xFF50698F), Offset(size.width * 0.68f, size.height * 0.16f), Offset(size.width * 0.68f, size.height * 0.30f), stroke * 0.8f, StrokeCap.Round)
                drawLine(Color(0xFF50698F), Offset(size.width * 0.28f, size.height * 0.5f), Offset(size.width * 0.72f, size.height * 0.5f), stroke * 0.72f, StrokeCap.Round)
            }
            HomeAction.Prayers -> {
                drawCircle(gold.copy(alpha = 0.4f), radius = size.minDimension * 0.26f, center = Offset(size.width * 0.58f, size.height * 0.24f))
                drawOval(
                    color = gold,
                    topLeft = Offset(size.width * 0.48f, size.height * 0.12f),
                    size = Size(size.width * 0.18f, size.height * 0.22f)
                )
                drawRoundRect(
                    color = ivory,
                    topLeft = Offset(size.width * 0.44f, size.height * 0.34f),
                    size = Size(size.width * 0.18f, size.height * 0.4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.04f)
                )
                drawLine(
                    color = Color(0xFF244A5B),
                    start = Offset(size.width * 0.53f, size.height * 0.40f),
                    end = Offset(size.width * 0.53f, size.height * 0.64f),
                    strokeWidth = stroke * 0.72f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF244A5B),
                    start = Offset(size.width * 0.45f, size.height * 0.52f),
                    end = Offset(size.width * 0.61f, size.height * 0.52f),
                    strokeWidth = stroke * 0.72f,
                    cap = StrokeCap.Round
                )
            }
            HomeAction.Rosary -> {
                drawArc(
                    color = gold,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.18f, size.height * 0.34f),
                    size = Size(size.width * 0.64f, size.height * 0.42f),
                    style = Stroke(width = stroke * 0.72f, cap = StrokeCap.Round)
                )
                listOf(0.18f, 0.28f, 0.39f, 0.50f, 0.61f, 0.72f, 0.82f).forEach { x ->
                    drawCircle(gold, radius = size.minDimension * 0.035f, center = Offset(size.width * x, size.height * 0.58f))
                }
                drawLine(gold, Offset(center.x, size.height * 0.34f), Offset(center.x, size.height * 0.66f), stroke, StrokeCap.Round)
                drawLine(gold, Offset(size.width * 0.36f, size.height * 0.50f), Offset(size.width * 0.64f, size.height * 0.50f), stroke, StrokeCap.Round)
            }
            HomeAction.Intentions -> {
                val heart = androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x, size.height * 0.72f)
                    cubicTo(size.width * 0.18f, size.height * 0.54f, size.width * 0.16f, size.height * 0.28f, size.width * 0.34f, size.height * 0.28f)
                    cubicTo(size.width * 0.46f, size.height * 0.28f, size.width * 0.5f, size.height * 0.38f, center.x, size.height * 0.44f)
                    cubicTo(size.width * 0.5f, size.height * 0.38f, size.width * 0.54f, size.height * 0.28f, size.width * 0.66f, size.height * 0.28f)
                    cubicTo(size.width * 0.84f, size.height * 0.28f, size.width * 0.82f, size.height * 0.54f, center.x, size.height * 0.72f)
                    close()
                }
                drawPath(heart, rose.copy(alpha = 0.8f))
                drawLine(Color.White.copy(alpha = 0.8f), Offset(center.x, size.height * 0.38f), Offset(center.x, size.height * 0.56f), stroke, StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.8f), Offset(size.width * 0.41f, size.height * 0.47f), Offset(size.width * 0.59f, size.height * 0.47f), stroke, StrokeCap.Round)
            }
            HomeAction.Daily -> {
                drawCircle(gold.copy(alpha = 0.75f), radius = size.minDimension * 0.09f, center = Offset(size.width * 0.74f, size.height * 0.22f))
                drawLine(gold, Offset(size.width * 0.5f, size.height * 0.12f), Offset(size.width * 0.5f, size.height * 0.34f), stroke, StrokeCap.Round)
                drawLine(gold, Offset(size.width * 0.40f, size.height * 0.23f), Offset(size.width * 0.60f, size.height * 0.23f), stroke, StrokeCap.Round)
                val pageColor = Color(0xFFE8DCC2)
                drawArc(
                    color = pageColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(size.width * 0.18f, size.height * 0.50f),
                    size = Size(size.width * 0.32f, size.height * 0.18f)
                )
                drawArc(
                    color = pageColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(size.width * 0.50f, size.height * 0.50f),
                    size = Size(size.width * 0.32f, size.height * 0.18f)
                )
            }
        }
    }
}

@Composable
private fun HomeActionIllustration(
    action: HomeAction,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val gold = Color(0xFFF7DF91)
        val cyan = Color(0xFF8EE0F2)
        val rose = Color(0xFFF2ACC8)
        val ivory = Color(0xFFF2E8D0)
        val navy = Color(0xFF102E3D)
        val deep = Color(0xFF243949)
        val stroke = size.minDimension * 0.035f
        val center = Offset(size.width / 2f, size.height / 2f)

        val scaleFactor = when (action) {
            HomeAction.Saints, HomeAction.Patronage -> 1.22f
            HomeAction.Novenas -> 1.20f
            HomeAction.Liturgical -> 1.16f
            HomeAction.Prayers -> 1.18f
            HomeAction.Rosary -> 1.18f
            HomeAction.Intentions -> 1.16f
            HomeAction.Daily -> 1.20f
        }
        val offsetX = when (action) {
            HomeAction.Saints, HomeAction.Patronage -> -size.width * 0.035f
            HomeAction.Novenas -> -size.width * 0.03f
            HomeAction.Liturgical -> -size.width * 0.02f
            HomeAction.Prayers -> -size.width * 0.02f
            HomeAction.Rosary -> -size.width * 0.02f
            HomeAction.Intentions -> -size.width * 0.02f
            HomeAction.Daily -> -size.width * 0.02f
        }
        val offsetY = when (action) {
            HomeAction.Saints, HomeAction.Patronage -> size.height * 0.01f
            HomeAction.Novenas -> size.height * 0.015f
            HomeAction.Liturgical -> size.height * 0.01f
            HomeAction.Prayers -> size.height * 0.01f
            HomeAction.Rosary -> size.height * 0.01f
            HomeAction.Intentions -> size.height * 0.01f
            HomeAction.Daily -> size.height * 0.015f
        }

        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = center)
        }) {
        when (action) {
            HomeAction.Saints, HomeAction.Patronage -> {
                drawCircle(gold.copy(alpha = 0.35f), radius = size.minDimension * 0.34f, center = Offset(size.width * 0.64f, size.height * 0.26f))
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.16f, size.height * 0.82f)
                        quadraticBezierTo(size.width * 0.50f, size.height * 0.62f, size.width * 0.84f, size.height * 0.82f)
                        lineTo(size.width * 0.84f, size.height)
                        lineTo(size.width * 0.16f, size.height)
                        close()
                    },
                    color = Color(0xFF2A5E72)
                )
                drawCircle(Color(0xFFD9C49B), radius = size.minDimension * 0.17f, center = Offset(size.width * 0.64f, size.height * 0.30f))
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.48f, size.height * 0.28f)
                        quadraticBezierTo(size.width * 0.54f, size.height * 0.08f, size.width * 0.82f, size.height * 0.20f)
                        lineTo(size.width * 0.82f, size.height * 0.30f)
                        close()
                    },
                    color = deep
                )
                drawLine(gold, Offset(size.width * 0.64f, size.height * 0.10f), Offset(size.width * 0.64f, size.height * 0.40f), stroke * 1.2f, StrokeCap.Round)
                drawLine(gold, Offset(size.width * 0.50f, size.height * 0.25f), Offset(size.width * 0.78f, size.height * 0.25f), stroke * 1.2f, StrokeCap.Round)
                drawLine(cyan.copy(alpha = 0.45f), Offset(size.width * 0.14f, size.height * 0.88f), Offset(size.width * 0.45f, size.height * 0.78f), stroke * 1.1f, StrokeCap.Round)
                drawLine(gold.copy(alpha = 0.38f), Offset(size.width * 0.70f, size.height * 0.76f), Offset(size.width * 0.92f, size.height * 0.86f), stroke * 1.1f, StrokeCap.Round)
            }
            HomeAction.Novenas -> {
                drawRoundRect(
                    color = Color(0xFF102D3D),
                    topLeft = Offset(size.width * 0.12f, size.height * 0.16f),
                    size = Size(size.width * 0.34f, size.height * 0.54f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.10f)
                )
                drawLine(cyan.copy(alpha = 0.42f), Offset(size.width * 0.18f, size.height * 0.28f), Offset(size.width * 0.40f, size.height * 0.28f), stroke, StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.22f), Offset(size.width * 0.18f, size.height * 0.42f), Offset(size.width * 0.36f, size.height * 0.42f), stroke * 0.9f, StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.18f), Offset(size.width * 0.18f, size.height * 0.54f), Offset(size.width * 0.38f, size.height * 0.54f), stroke * 0.9f, StrokeCap.Round)
                drawLine(gold, Offset(size.width * 0.29f, size.height * 0.58f), Offset(size.width * 0.29f, size.height * 0.76f), stroke, StrokeCap.Round)
                drawLine(gold, Offset(size.width * 0.20f, size.height * 0.67f), Offset(size.width * 0.38f, size.height * 0.67f), stroke, StrokeCap.Round)
                drawArc(
                    color = Color(0xFFD9C49B),
                    startAngle = -70f,
                    sweepAngle = 170f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.56f, size.height * 0.12f),
                    size = Size(size.width * 0.24f, size.height * 0.62f),
                    style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round)
                )
                listOf(0.0f, 0.12f, 0.24f, 0.36f, 0.52f).forEach { fraction ->
                    drawCircle(gold, radius = size.minDimension * 0.03f, center = Offset(size.width * 0.70f, size.height * (0.16f + fraction)))
                }
                drawLine(gold, Offset(size.width * 0.70f, size.height * 0.70f), Offset(size.width * 0.70f, size.height * 0.88f), stroke, StrokeCap.Round)
                drawLine(gold, Offset(size.width * 0.61f, size.height * 0.79f), Offset(size.width * 0.79f, size.height * 0.79f), stroke, StrokeCap.Round)
                drawArc(
                    color = cyan.copy(alpha = 0.55f),
                    startAngle = -68f,
                    sweepAngle = 145f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.76f, size.height * 0.22f),
                    size = Size(size.width * 0.18f, size.height * 0.44f),
                    style = Stroke(width = stroke * 0.8f, cap = StrokeCap.Round)
                )
            }
            HomeAction.Liturgical -> {
                drawRoundRect(
                    color = Color(0xFF9CB9E0),
                    topLeft = Offset(size.width * 0.26f, size.height * 0.20f),
                    size = Size(size.width * 0.34f, size.height * 0.40f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.08f)
                )
                drawRoundRect(
                    color = Color(0xFF6E86B1),
                    topLeft = Offset(size.width * 0.26f, size.height * 0.20f),
                    size = Size(size.width * 0.34f, size.height * 0.12f)
                )
                drawLine(Color(0xFF50698F), Offset(size.width * 0.34f, size.height * 0.14f), Offset(size.width * 0.34f, size.height * 0.28f), stroke * 0.8f, StrokeCap.Round)
                drawLine(Color(0xFF50698F), Offset(size.width * 0.52f, size.height * 0.14f), Offset(size.width * 0.52f, size.height * 0.28f), stroke * 0.8f, StrokeCap.Round)
                drawLine(Color(0xFF50698F), Offset(size.width * 0.30f, size.height * 0.42f), Offset(size.width * 0.56f, size.height * 0.42f), stroke * 0.7f, StrokeCap.Round)
            }
            HomeAction.Prayers -> {
                drawCircle(gold.copy(alpha = 0.30f), radius = size.minDimension * 0.30f, center = Offset(size.width * 0.56f, size.height * 0.22f))
                drawOval(gold, Offset(size.width * 0.44f, size.height * 0.10f), Size(size.width * 0.14f, size.height * 0.18f))
                drawRoundRect(
                    color = ivory,
                    topLeft = Offset(size.width * 0.42f, size.height * 0.30f),
                    size = Size(size.width * 0.12f, size.height * 0.42f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.04f)
                )
                drawLine(Color(0xFF2C5363), Offset(size.width * 0.48f, size.height * 0.36f), Offset(size.width * 0.48f, size.height * 0.62f), stroke, StrokeCap.Round)
                drawLine(Color(0xFF2C5363), Offset(size.width * 0.40f, size.height * 0.49f), Offset(size.width * 0.56f, size.height * 0.49f), stroke, StrokeCap.Round)
                drawLine(cyan.copy(alpha = 0.35f), Offset(size.width * 0.14f, size.height * 0.78f), Offset(size.width * 0.48f, size.height * 0.70f), stroke * 1.1f, StrokeCap.Round)
                drawLine(rose.copy(alpha = 0.40f), Offset(size.width * 0.56f, size.height * 0.70f), Offset(size.width * 0.86f, size.height * 0.78f), stroke * 1.1f, StrokeCap.Round)
            }
            HomeAction.Rosary -> {
                drawCircle(gold.copy(alpha = 0.24f), radius = size.minDimension * 0.32f, center = Offset(size.width * 0.60f, size.height * 0.24f))
                drawArc(
                    color = cyan.copy(alpha = 0.42f),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.18f, size.height * 0.36f),
                    size = Size(size.width * 0.68f, size.height * 0.40f),
                    style = Stroke(width = stroke * 1.1f, cap = StrokeCap.Round)
                )
                listOf(0.18f, 0.28f, 0.39f, 0.50f, 0.61f, 0.72f, 0.82f).forEach { x ->
                    drawCircle(gold, radius = size.minDimension * 0.034f, center = Offset(size.width * x, size.height * 0.58f))
                }
                drawLine(gold, Offset(size.width * 0.50f, size.height * 0.28f), Offset(size.width * 0.50f, size.height * 0.64f), stroke * 1.1f, StrokeCap.Round)
                drawLine(gold, Offset(size.width * 0.38f, size.height * 0.46f), Offset(size.width * 0.62f, size.height * 0.46f), stroke * 1.1f, StrokeCap.Round)
                drawLine(gold, Offset(size.width * 0.50f, size.height * 0.72f), Offset(size.width * 0.50f, size.height * 0.88f), stroke, StrokeCap.Round)
                drawLine(gold, Offset(size.width * 0.42f, size.height * 0.80f), Offset(size.width * 0.58f, size.height * 0.80f), stroke, StrokeCap.Round)
            }
            HomeAction.Intentions -> {
                val heart = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.50f, size.height * 0.74f)
                    cubicTo(size.width * 0.24f, size.height * 0.56f, size.width * 0.22f, size.height * 0.24f, size.width * 0.38f, size.height * 0.24f)
                    cubicTo(size.width * 0.46f, size.height * 0.24f, size.width * 0.50f, size.height * 0.34f, size.width * 0.50f, size.height * 0.40f)
                    cubicTo(size.width * 0.50f, size.height * 0.34f, size.width * 0.54f, size.height * 0.24f, size.width * 0.62f, size.height * 0.24f)
                    cubicTo(size.width * 0.78f, size.height * 0.24f, size.width * 0.76f, size.height * 0.56f, size.width * 0.50f, size.height * 0.74f)
                    close()
                }
                drawPath(heart, rose.copy(alpha = 0.72f))
                drawLine(Color.White.copy(alpha = 0.72f), Offset(size.width * 0.50f, size.height * 0.34f), Offset(size.width * 0.50f, size.height * 0.56f), stroke * 1.1f, StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.72f), Offset(size.width * 0.40f, size.height * 0.45f), Offset(size.width * 0.60f, size.height * 0.45f), stroke * 1.1f, StrokeCap.Round)
                drawCircle(gold.copy(alpha = 0.76f), radius = size.minDimension * 0.04f, center = Offset(size.width * 0.20f, size.height * 0.70f))
                drawCircle(cyan.copy(alpha = 0.70f), radius = size.minDimension * 0.03f, center = Offset(size.width * 0.82f, size.height * 0.18f))
                drawLine(cyan.copy(alpha = 0.32f), Offset(size.width * 0.12f, size.height * 0.82f), Offset(size.width * 0.86f, size.height * 0.76f), stroke * 1.1f, StrokeCap.Round)
            }
            HomeAction.Daily -> {
                drawCircle(gold.copy(alpha = 0.72f), radius = size.minDimension * 0.09f, center = Offset(size.width * 0.78f, size.height * 0.18f))
                drawLine(gold, Offset(size.width * 0.54f, size.height * 0.18f), Offset(size.width * 0.54f, size.height * 0.44f), stroke * 1.1f, StrokeCap.Round)
                drawLine(gold, Offset(size.width * 0.42f, size.height * 0.31f), Offset(size.width * 0.66f, size.height * 0.31f), stroke * 1.1f, StrokeCap.Round)
                drawArc(
                    color = Color(0xFFE8DCC2),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(size.width * 0.14f, size.height * 0.58f),
                    size = Size(size.width * 0.28f, size.height * 0.18f)
                )
                drawArc(
                    color = Color(0xFFE8DCC2),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(size.width * 0.42f, size.height * 0.58f),
                    size = Size(size.width * 0.28f, size.height * 0.18f)
                )
                drawLine(Color(0xFF2B5663).copy(alpha = 0.44f), Offset(size.width * 0.20f, size.height * 0.66f), Offset(size.width * 0.38f, size.height * 0.66f), stroke * 0.7f, StrokeCap.Round)
                drawLine(Color(0xFF2B5663).copy(alpha = 0.44f), Offset(size.width * 0.48f, size.height * 0.66f), Offset(size.width * 0.66f, size.height * 0.66f), stroke * 0.7f, StrokeCap.Round)
            }
        }
        }
    }
}

@Composable
private fun LoadingCard() {
    val l10n = sanctuaryStrings()
    Card(
        modifier = Modifier.shadow(12.dp, RoundedCornerShape(24.dp), clip = false),
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
            Text(l10n.t("common.loading"), color = Color.White)
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
    val l10n = sanctuaryStrings()
    Card(
        modifier = Modifier.sanctuaryCardShadow(),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCardElevated),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            TextFieldBlock(label = l10n.t("search.field"), value = query, onValueChange = onQueryChanged)
            PrimaryButton(l10n.t("common.search"), false, onClick = onSubmit)
        }
    }
}

@Composable
private fun ContentCard(
    title: String,
    subtitle: String?,
    detail: String?,
    imageUrl: String? = null,
    imageUrls: List<String> = emptyList(),
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.sanctuaryCardShadow(RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xC3182F40)),
        shape = RoundedCornerShape(22.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (imageUrls.isNotEmpty()) {
                ThumbnailStack(
                    imageUrls = imageUrls,
                    contentDescription = title,
                    modifier = Modifier.size(82.dp)
                )
            } else {
                ThumbnailImage(
                    imageUrl = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.size(82.dp),
                    shape = RoundedCornerShape(18.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                detail?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Color(0xFF7AC8EA), style = MaterialTheme.typography.labelLarge)
                }
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = Color(0xFFD0DFEA),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }
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
private fun ThumbnailStack(
    imageUrls: List<String>,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val urls = imageUrls.filter { it.isNotBlank() }.take(3)
    if (urls.size <= 1) {
        ThumbnailImage(
            imageUrl = urls.firstOrNull(),
            contentDescription = contentDescription,
            modifier = modifier,
            shape = RoundedCornerShape(18.dp)
        )
        return
    }

    Box(modifier = modifier) {
        urls.forEachIndexed { index, url ->
            ThumbnailImage(
                imageUrl = url,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(58.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (index * 12).dp, y = (index * 4).dp)
                    .border(2.dp, Color(0xFF183246), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
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

@Composable
private fun SanctuaryModalSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            SanctuaryGradientTop,
                            SanctuaryGradientMid,
                            SanctuaryGradientBottom
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 64.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.Top
            ) {
                content()
            }
            Surface(
                color = Color(0xCC102232),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 12.dp, start = 18.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier
                        .size(42.dp)
                        .clickable(onClick = onDismissRequest)
                        .padding(10.dp)
                )
            }
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
    val l10n = sanctuaryStrings()
    DetailSheetScaffold(title = l10n.t("detail.loadErrorTitle")) {
        Banner(message.ifBlank { l10n.t("detail.loadErrorBody") }, isError = true)
    }
}

@Composable
private fun SaintDetailSheet(
    detail: SaintDetail,
    session: SessionUiState,
    progress: NovenaProgressUiState,
    onToggleFavorite: (FavoriteItemType, String, String?, String?, Int?) -> Unit,
    onRequireAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    val l10n = sanctuaryStrings()
    val context = LocalContext.current
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
        Button(
            onClick = {
                if (session.status == SessionStatus.Authenticated) {
                    onToggleFavorite(FavoriteItemType.Saint, detail.id, detail.name, detail.slug, null)
                } else {
                    onRequireAccount()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFavorite) Color(0xFF5CAED4) else Color(0xFF22394C),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isFavorite) l10n.t("detail.favorite.saved") else l10n.t("detail.favorite.add"))
        }
        Button(
            onClick = {
                shareContent(
                    context = context,
                    title = detail.name,
                    message = "Look at this saint in Sanctuary: ${detail.name}",
                    url = sharedContentUrl(SharedContentKind.Saint, detail.slug)
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF22394C),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share")
        }
        if (detail.patronages.isNotEmpty()) {
            DetailSectionCard(title = l10n.t("detail.patronages")) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    detail.patronages.forEach { patronage ->
                        Text("• $patronage", color = Color(0xFFD0DFEA), lineHeight = 20.sp)
                    }
                }
            }
        }
        detail.summary?.takeIf { it.isNotBlank() }?.let { summary ->
            DetailSectionCard(title = l10n.t("detail.summary")) {
                Text(summary, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
            }
        }
        detail.biography?.takeIf { it.isNotBlank() }?.let { biography ->
            DetailSectionCard(title = l10n.t("detail.biography")) {
                Text(biography, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
            }
        }
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
    onToggleFavorite: (FavoriteItemType, String, String?, String?, Int?) -> Unit,
    onRequireAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    val l10n = sanctuaryStrings()
    val context = LocalContext.current
    val activeCommitment = progress.commitments.firstOrNull {
        it.novenaId == detail.id && it.status == CommitmentStatus.Active
    }
    val isStarting = detail.id in progress.pendingNovenaStarts
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
        !isStarting &&
        activeCommitment == null &&
        latestCommitment?.status != CommitmentStatus.Completed
    val completionLabel = when {
        latestCommitment?.status == CommitmentStatus.Completed -> l10n.t("detail.completed")
        activeCommitment != null -> "${l10n.t("detail.completeDay")} ${activeCommitment.currentDay}"
        else -> "${l10n.t("detail.completeDay")} 1"
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
        }

        Button(
            onClick = {
                if (session.status == SessionStatus.Authenticated) {
                    onToggleFavorite(FavoriteItemType.Novena, detail.id, detail.title, detail.slug, detail.durationDays)
                } else {
                    onRequireAccount()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFavorite) Color(0xFF5CAED4) else Color(0xFF22394C),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isFavorite) l10n.t("detail.favorite.saved") else l10n.t("detail.favorite.add"))
        }
        Button(
            onClick = {
                shareContent(
                    context = context,
                    title = detail.title,
                    message = "Pray this novena with me in Sanctuary: ${detail.title}",
                    url = sharedContentUrl(SharedContentKind.Novena, detail.slug)
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF22394C),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share")
        }

        if (detail.intentions.isNotEmpty()) {
            DetailSectionCard(title = l10n.t("detail.intentions")) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    detail.intentions.forEach { intention ->
                        Text("• $intention", color = Color(0xFFD0DFEA), lineHeight = 20.sp)
                    }
                }
            }
        }

        Text(l10n.t("calendar.chooseDay"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
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
                        Text(l10n.t("calendar.dayLabel"), fontSize = 12.sp, color = Color.White.copy(alpha = 0.78f))
                        Text("${day.dayNumber}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }
        }

        when {
            session.status != SessionStatus.Authenticated -> {
                PrimaryButton(l10n.t("detail.startNovena"), false, onClick = onRequireAccount)
            }
            isStarting -> {
                PrimaryButton(l10n.t("detail.startNovena"), true, onClick = {})
            }
            activeCommitment != null -> {
                PrimaryButton(l10n.t("detail.stopNovena"), false, onClick = { onStop(detail.id) })
            }
            canStart -> {
                PrimaryButton(l10n.t("detail.startNovena"), false, onClick = { onStart(detail.id) })
            }
        }

        DetailSectionCard(title = "${l10n.t("calendar.dayNumberPrefix")} $selectedDay") {
            if (selectedDayDetail == null) {
                Text(l10n.t("detail.noDayContent"), color = Color(0xFFD0DFEA), lineHeight = 22.sp)
            } else if (!selectedDayDetail.hasVisibleDayContent()) {
                Text(l10n.t("detail.noDayContent"), color = Color(0xFFD0DFEA), lineHeight = 22.sp)
            } else {
                selectedDayDetail.title?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp)
                }
                selectedDayDetail.scripture?.takeIf { it.isNotBlank() }?.let {
                    DetailSectionLabel(l10n.t("detail.scripture"))
                    Text(it, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
                }
                selectedDayDetail.prayer?.takeIf { it.isNotBlank() }?.let {
                    DetailSectionLabel(l10n.t("detail.prayer"))
                    Text(it, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
                }
                selectedDayDetail.reflection?.takeIf { it.isNotBlank() }?.let {
                    DetailSectionLabel(l10n.t("detail.reflection"))
                    Text(it, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
                }
                selectedDayDetail.body?.takeIf { selectedDayDetail.hasFallbackBodyContent() && it.isNotBlank() }?.let {
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

    }
}

@Composable
private fun PrayerDetailSheet(
    detail: PrayerDetail,
    session: SessionUiState,
    progress: NovenaProgressUiState,
    onToggleFavorite: (FavoriteItemType, String, String?, String?, Int?) -> Unit,
    onRequireAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    val l10n = sanctuaryStrings()
    val context = LocalContext.current
    val bodyText = displayPrayerBody(detail)
    var isShowingExpandedImage by rememberSaveable { mutableStateOf(false) }
    val canExpandHeroImage = detail.slug == "how_to_pray_the_rosary"
    val isFavorite = progress.favorites.any { it.itemType == FavoriteItemType.Prayer && it.itemId == detail.id }
    DetailSheetScaffold(
        title = detail.title,
        subtitle = visiblePrayerCategory(detail.category)
    ) {
        Box {
            ThumbnailImage(
                imageUrl = detail.imageUrl,
                contentDescription = detail.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f)
                    .then(
                        if (canExpandHeroImage) {
                            Modifier.clickable { isShowingExpandedImage = true }
                        } else {
                            Modifier
                        }
                    ),
                shape = RoundedCornerShape(24.dp)
            )
            if (canExpandHeroImage) {
                Surface(
                    color = Color(0xCC102232),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SouthEast,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(34.dp)
                            .padding(8.dp)
                    )
                }
            }
        }
        DetailSectionCard(title = detail.title) {
            Button(
                onClick = {
                    if (session.status == SessionStatus.Authenticated) {
                        onToggleFavorite(FavoriteItemType.Prayer, detail.id, detail.title, detail.slug, null)
                    } else {
                        onRequireAccount()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFavorite) Color(0xFF5CAED4) else Color(0xFF22394C),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFavorite) l10n.t("detail.favorite.saved") else l10n.t("detail.favorite.add"))
            }
            Button(
                onClick = {
                    shareContent(
                        context = context,
                        title = detail.title,
                        message = "Pray this with me in Sanctuary: ${detail.title}",
                        url = sharedContentUrl(SharedContentKind.Prayer, detail.slug)
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22394C),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share")
            }
            detail.note?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
            }
        }
        DetailSectionCard(title = l10n.t("detail.prayer")) {
            Text(bodyText, color = Color(0xFFD0DFEA), lineHeight = 24.sp)
        }
    }

    if (isShowingExpandedImage) {
        ExpandedPrayerImageDialog(
            imageUrl = detail.imageUrl,
            contentDescription = detail.title,
            onDismiss = { isShowingExpandedImage = false }
        )
    }
}

private fun displayPrayerBody(detail: PrayerDetail): String {
    if (!detail.category.equals("rosary", ignoreCase = true)) {
        return detail.body
    }

    val heading = detail.alternateTitle?.trim().orEmpty()
    if (heading.isEmpty()) {
        return detail.body
    }

    val normalized = detail.body.replace("\r\n", "\n").trimStart()
    val lines = normalized.lines()
    if (lines.firstOrNull()?.trim() != heading) {
        return detail.body
    }

    return lines.drop(1).joinToString("\n").trimStart()
}

private fun visiblePrayerCategory(category: String?): String? {
    val normalized = category?.trim().orEmpty()
    if (
        normalized.isBlank() ||
        normalized.equals("user_provided", ignoreCase = true) ||
        normalized.equals("rosary", ignoreCase = true)
    ) {
        return null
    }
    return normalized
}

@Composable
private fun ExpandedPrayerImageDialog(
    imageUrl: String?,
    contentDescription: String,
    onDismiss: () -> Unit
) {
    val resolvedUrl = resolveImageUrl(imageUrl)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            if (resolvedUrl != null) {
                AsyncImage(
                    model = resolvedUrl,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                )
            }

            Surface(
                color = Color.White.copy(alpha = 0.16f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onDismiss() }
                        .padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .sanctuaryCardShadow(),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCardElevated),
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
    AppTab.Novenas -> Icons.Filled.CalendarMonth
    AppTab.Liturgical -> Icons.Filled.CalendarMonth
    AppTab.Saints -> Icons.Filled.CalendarMonth
    AppTab.Me -> Icons.Filled.Person
}

private fun AppTab.label(l10n: SanctuaryStrings): String = when (this) {
    AppTab.Home -> l10n.t("tab.home")
    AppTab.Novenas -> l10n.t("tab.novenas")
    AppTab.Liturgical -> l10n.t("tab.liturgical")
    AppTab.Saints -> l10n.t("tab.saints")
    AppTab.Me -> l10n.t("tab.me")
}

private fun CalendarMode.label(l10n: SanctuaryStrings): String = when (this) {
    CalendarMode.Day -> l10n.t("common.day")
    CalendarMode.Week -> l10n.t("common.week")
    CalendarMode.Month -> l10n.t("common.month")
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
        modifier = Modifier.sanctuaryCardShadow(RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xC3182F40)),
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
    val resolvedUrl = resolveImageUrl(imageUrl)

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

private fun resolveImageUrl(imageUrl: String?): String? =
    imageUrl?.takeIf { it.isNotBlank() }?.let {
        if (it.startsWith("http://") || it.startsWith("https://")) it
        else "${BuildConfig.API_BASE_URL.trimEnd('/')}/$it"
    }

@Composable
private fun TermSearchSheet(
    title: String,
    query: String,
    prompt: String,
    state: TermSearchUiState,
    mode: TermSearchMode,
    onQueryChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onClearSelectedTerm: () -> Unit,
    onSelectTerm: (SearchTerm) -> Unit,
    onOpenSaint: (String) -> Unit,
    onOpenNovena: (String) -> Unit
) {
    val l10n = sanctuaryStrings()

    LaunchedEffect(query) {
        delay(250)
        onSubmit()
    }

    DetailSheetScaffold(title = title) {
        SearchCard(
            title = prompt,
            query = query,
            onQueryChanged = onQueryChanged,
            onSubmit = onSubmit
        )
        state.selectedTerm?.let { term ->
            DetailSectionCard(title = term.label) {
                SecondaryButton(
                    title = l10n.t("common.back"),
                    isBusy = false,
                    onClick = onClearSelectedTerm
                )
            }
        }
        when {
            state.isLoading -> InlineLoading(l10n.t("inline.loading"))
            state.error != null -> Banner(state.error, isError = true)
            state.selectedTerm == null && state.terms.isEmpty() -> Text(title, color = Color(0xFFD0DFEA))
            state.selectedTerm == null -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(state.terms) { term ->
                    ContentCard(
                        title = term.label,
                        subtitle = termResultLabel(term, l10n),
                        detail = null,
                        imageUrls = term.imageUrls,
                        onClick = { onSelectTerm(term) }
                    )
                }
            }
            mode == TermSearchMode.Intentions && state.novenas.isEmpty() -> Text(title, color = Color(0xFFD0DFEA))
            mode == TermSearchMode.Intentions -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(state.novenas) { novena ->
                    ContentCard(
                        title = novena.title,
                        subtitle = novena.description,
                        detail = novena.intentions.take(3).joinToString(" • ").ifBlank { "${novena.durationDays}-day novena" },
                        imageUrl = novena.imageUrl,
                        onClick = { onOpenNovena(novena.slug) }
                    )
                }
            }
            state.saints.isEmpty() -> Text(title, color = Color(0xFFD0DFEA))
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(state.saints) { saint ->
                    ContentCard(
                        title = saint.name,
                        subtitle = saint.summary ?: l10n.t("home.saintsSubtitle"),
                        detail = saint.patronages.take(3).joinToString(" • ").ifBlank { saint.feastLabel },
                        imageUrl = saint.imageUrl,
                        onClick = { onOpenSaint(saint.slug) }
                    )
                }
            }
        }
    }
}

private fun termResultLabel(term: SearchTerm, l10n: SanctuaryStrings): String {
    val labels = term.resultLabels.filter { it.isNotBlank() }.take(2)
    if (labels.isEmpty()) {
        return "${term.resultCount} ${l10n.t("search.results")}"
    }

    val suffix = if (term.resultCount > labels.size) " +${term.resultCount - labels.size}" else ""
    return labels.joinToString(" • ") + suffix
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
    val l10n = sanctuaryStrings()
    LaunchedEffect(query) {
        delay(250)
        onSubmit()
    }

    DetailSheetScaffold(title = title) {
        SearchCard(
            title = title,
            query = query,
            onQueryChanged = onQueryChanged,
            onSubmit = onSubmit
        )
        Text(
            text = "${items.size} ${l10n.t("search.results")}",
            color = Color.White.copy(alpha = 0.92f),
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp
        )
        when {
            isLoading -> InlineLoading(l10n.t("inline.loading"))
            error != null -> Banner(error, isError = true)
            items.isEmpty() -> Text(emptyLabel, color = Color(0xFFD0DFEA))
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(items) { item -> itemContent(item) }
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
    fetchSaintsInRange: suspend (String, String) -> List<app.sanctuary.android.data.SaintDateGroup>,
    fetchLiturgicalRange: suspend (String, String) -> List<app.sanctuary.android.data.LiturgicalDay>
) {
    val l10n = sanctuaryStrings()
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
                onFailure = { CalendarLoadState.Error(it.message ?: l10n.t("search.saintsTitle")) }
            )
    }
    val seasonByDate by produceState<Map<LocalDate, String>>(
        initialValue = emptyMap(),
        month, selectedYear, selectedMonth
    ) {
        value = runCatching { fetchLiturgicalRange(month.atDay(1).toString(), month.atEndOfMonth().toString()) }
            .getOrDefault(emptyList())
            .associate { LocalDate.parse(it.date) to it.season }
    }

    LaunchedEffect(selectedMonth, selectedYear) {
        selectedDay = selectedDay.coerceIn(1, month.lengthOfMonth())
    }

    CalendarSurface(
        title = if (mode == CalendarMode.Day) {
            l10n.formatMonthDayYear(LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())))
        } else {
            l10n.formatMonthYear(selectedMonth, selectedYear)
        },
        subtitle = l10n.t("calendar.subtitle.saints"),
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
        },
        footer = {
            CalendarSearchButton(label = l10n.t("calendar.searchSaints"), onClick = onSearch)
            SeasonLegend()
        }
    ) {
        when (val current = state) {
            CalendarLoadState.Loading -> InlineLoading(l10n.t("common.loading"))
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
                                buttonLabel = l10n.t("common.openDetails"),
                                borderColor = liturgicalBorderColor(seasonByDate[previewDate]),
                                onClick = { onOpenSaint(preview.slug) }
                            )
                        } else {
                            SectionHint(l10n.t("search.saintsTitle"), l10n.t("calendar.noLiturgicalBody"))
                        }
                    }
                    CalendarMode.Week -> {
                        CalendarWeekGrid(
                            month = month,
                            selectedDay = selectedDay,
                            labelForDay = { day -> saintByDate[month.atDay(day)]?.let { shortLabel(sanitizedSaintName(it.name)) } ?: "·" },
                            borderColorForDay = { day -> liturgicalBorderColor(seasonByDate[month.atDay(day)]) },
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
                            borderColorForDay = { day -> liturgicalBorderColor(seasonByDate[month.atDay(day)]) },
                            onDaySelected = {
                                selectedDay = it
                                onModeChange(CalendarMode.Day)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NovenasCalendarScreen(
    mode: CalendarMode,
    onModeChange: (CalendarMode) -> Unit,
    onSearch: () -> Unit,
    onOpenNovena: (String) -> Unit,
    fetchNovenasInRange: suspend (String, String) -> List<app.sanctuary.android.data.NovenaCalendarDate>,
    fetchLiturgicalRange: suspend (String, String) -> List<app.sanctuary.android.data.LiturgicalDay>
) {
    val l10n = sanctuaryStrings()
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
                onFailure = { CalendarLoadState.Error(it.message ?: l10n.t("search.novenasTitle")) }
            )
    }
    val seasonByDate by produceState<Map<LocalDate, String>>(
        initialValue = emptyMap(),
        month, selectedYear, selectedMonth
    ) {
        value = runCatching { fetchLiturgicalRange(month.atDay(1).toString(), month.atEndOfMonth().toString()) }
            .getOrDefault(emptyList())
            .associate { LocalDate.parse(it.date) to it.season }
    }

    LaunchedEffect(selectedMonth, selectedYear) {
        selectedDay = selectedDay.coerceIn(1, month.lengthOfMonth())
    }

    CalendarSurface(
        title = if (mode == CalendarMode.Day) {
            l10n.formatMonthDayYear(LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())))
        } else {
            l10n.formatMonthYear(selectedMonth, selectedYear)
        },
        subtitle = l10n.t("calendar.subtitle.novenas"),
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
        },
        footer = {
            CalendarSearchButton(label = l10n.t("calendar.searchNovenas"), onClick = onSearch)
            SeasonLegend()
        }
    ) {
        when (val current = state) {
            CalendarLoadState.Loading -> InlineLoading(l10n.t("common.loading"))
            is CalendarLoadState.Error -> Banner(current.message, isError = true)
            is CalendarLoadState.Ready -> {
                val novenaByDate = current.value.mapNotNull { entry ->
                    entry.startingNovena?.let { LocalDate.parse(entry.date) to it }
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
                                buttonLabel = l10n.t("common.openDetails"),
                                borderColor = liturgicalBorderColor(seasonByDate[previewDate]),
                                onClick = { onOpenNovena(preview.slug) }
                            )
                        } else {
                            DayPreviewCard(
                                date = previewDate,
                                title = l10n.t("calendar.noNovenaStarting"),
                                subtitle = "",
                                imageUrl = null,
                                buttonLabel = null,
                                enabled = false,
                                showImage = false,
                                borderColor = liturgicalBorderColor(seasonByDate[previewDate]),
                                onClick = {}
                            )
                        }
                    }
                    CalendarMode.Week -> {
                        CalendarWeekGrid(
                            month = month,
                            selectedDay = selectedDay,
                            labelForDay = { day -> novenaByDate[month.atDay(day)]?.let { shortLabel(it.title) } ?: "·" },
                            borderColorForDay = { day -> liturgicalBorderColor(seasonByDate[month.atDay(day)]) },
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
                            borderColorForDay = { day -> liturgicalBorderColor(seasonByDate[month.atDay(day)]) },
                            onDaySelected = {
                                selectedDay = it
                                onModeChange(CalendarMode.Day)
                            }
                        )
                    }
                }
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
    val l10n = sanctuaryStrings()
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
            onFailure = { CalendarLoadState.Error(it.message ?: l10n.t("calendar.noLiturgicalBody")) }
        )
    }

    LaunchedEffect(selectedMonth, selectedYear) {
        selectedDay = selectedDay.coerceIn(1, month.lengthOfMonth())
    }

    CalendarSurface(
        title = if (mode == CalendarMode.Day) {
            l10n.formatMonthDayYear(LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())))
        } else {
            l10n.formatMonthYear(selectedMonth, selectedYear)
        },
        subtitle = l10n.t("calendar.subtitle.liturgical"),
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
        },
        footer = {
            SeasonLegend()
        }
    ) {
        when (val current = state) {
            CalendarLoadState.Loading -> InlineLoading(l10n.t("common.loading"))
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
                                        readingError = l10n.t("calendar.noLiturgicalBody")
                                    }
                                }
                            )
                        } else {
                            SectionHint(l10n.t("calendar.noLiturgicalFound"), l10n.t("calendar.noLiturgicalBody"))
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
                        CalendarMonthGrid(
                            month = month,
                            selectedDay = selectedDay,
                            labelForDay = { day ->
                                liturgicalByDate[month.atDay(day)]?.let(::shortLiturgicalLabel) ?: "·"
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
                }
            }
        }
        readingError?.let { Banner(it, isError = true) }
    }
}

@Composable
private fun DailyReadingsSheet(
    url: String,
    onDismiss: () -> Unit
) {
    val l10n = sanctuaryStrings()
    DetailSheetScaffold(
        title = l10n.t("calendar.dailyReadingsTitle"),
        subtitle = l10n.t("calendar.dailyReadingsSubtitle")
    ) {
        DailyReadingsWebView(url = url)
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
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val uri = request.url
                            val scheme = uri.scheme
                            if (scheme == "http" || scheme == "https") {
                                return false
                            }
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            return try {
                                context.startActivity(intent)
                                true
                            } catch (_: ActivityNotFoundException) {
                                true
                            }
                        }
                    }
                    webChromeClient = WebChromeClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    isVerticalScrollBarEnabled = true
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                    setOnTouchListener { view, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN,
                            MotionEvent.ACTION_MOVE -> view.parent?.requestDisallowInterceptTouchEvent(true)
                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL -> view.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        false
                    }
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
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val l10n = sanctuaryStrings()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
            FilterChipButton(title = l10n.t("common.today"), selected = false, onClick = onToday)
            CalendarMode.entries.forEach { entry ->
                FilterChipButton(
                    title = entry.label(l10n),
                    selected = mode == entry,
                    onClick = { onModeChange(entry) }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .calendarDaySwipe(
                    enabled = mode == CalendarMode.Day,
                    onPrevious = onPrev,
                    onNext = onNext
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = footer
        )
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
    buttonLabel: String?,
    enabled: Boolean = true,
    showImage: Boolean = true,
    borderColor: Color = liturgicalBorderColor(null),
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(26.dp)
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
        shape = cardShape,
        border = BorderStroke(4.dp, borderColor),
        enabled = enabled,
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
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = Color(0xFFD0DFEA), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                if (buttonLabel != null) {
                    Text(buttonLabel, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
            if (showImage) {
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
    val rowCount = ((offset + total + 6) / 7).coerceIn(4, 6)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val rowGap = if (maxHeight < 390.dp) 8.dp else 10.dp
        val headerHeight = 20.dp
        val availableForRows = maxHeight - headerHeight - rowGap * rowCount
        val cellHeight = (availableForRows / rowCount).coerceIn(54.dp, 72.dp)

        Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
            CalendarWeekHeaderRow()
            var dayNumber = 1
            repeat(rowCount) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(7) { column ->
                        val index = row * 7 + column
                        if (index < offset || dayNumber > total) {
                            Spacer(modifier = Modifier.weight(1f).height(cellHeight))
                        } else {
                            val currentDay = dayNumber
                            val today = LocalDate.now()
                            CalendarEntryCell(
                                day = currentDay,
                                label = labelForDay(currentDay),
                                borderColor = borderColorForDay(currentDay),
                                selected = currentDay == selectedDay,
                                isToday = month.atDay(currentDay) == today,
                                height = cellHeight,
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
private fun CalendarWeekHeaderRow() {
    val l10n = sanctuaryStrings()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        l10n.weekdaySymbolsShort().forEach { label ->
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
    val l10n = sanctuaryStrings()
    DayPreviewCard(
        date = date,
        title = detail.primaryRank,
        subtitle = detail.observances.firstOrNull().orEmpty().ifBlank { detail.season.replaceFirstChar { it.uppercase() } },
        imageUrl = null,
        buttonLabel = l10n.t("calendar.openDailyReadings"),
        borderColor = liturgicalBorderColor(detail.season),
        onClick = onOpenReadings
    )
}

private fun liturgicalBorderColor(season: String?): Color = when (season?.lowercase()) {
    "advent" -> Color(0xFF7858B9)
    "christmas" -> Color.White
    "lent" -> Color(0xFF9B5087)
    "easter" -> Color.White
    else -> Color(0xFF3C9B5F)
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
    val l10n = sanctuaryStrings()
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 380.dp
        val fontSize = if (compact) 10.sp else 12.sp
        val dotSize = if (compact) 6.dp else 7.dp
        val spacing = if (compact) 8.dp else 14.dp

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SeasonDot(l10n.t("season.advent"), Color(0xFF8B5CF6), fontSize, dotSize, spacing)
            SeasonDot(l10n.t("season.christmas"), Color(0xFFE7C76A), fontSize, dotSize, spacing)
            SeasonDot(l10n.t("season.lent"), Color(0xFFD16BA5), fontSize, dotSize, spacing)
            SeasonDot(l10n.t("season.easter"), Color(0xFFF5F5F5), fontSize, dotSize, spacing)
            SeasonDot(l10n.t("season.ordinary"), Color(0xFF6FB56B), fontSize, dotSize, spacing)
        }
    }
}

@Composable
private fun SeasonDot(
    label: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    dotSize: androidx.compose.ui.unit.Dp,
    spacing: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier.width(intrinsicSize = androidx.compose.foundation.layout.IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(spacing / 2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(dotSize).background(color, CircleShape))
        Text(
            text = label,
            color = Color(0xFFD0DFEA),
            fontSize = fontSize,
            maxLines = 1,
            softWrap = false
        )
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

private fun headerTitle(mode: CalendarMode, anchor: LocalDate, language: AppLanguage = AppLanguage.English): String {
    return when (mode) {
        CalendarMode.Day -> anchor.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", language.locale))
        CalendarMode.Week -> "${anchor.format(DateTimeFormatter.ofPattern("MMMM d", language.locale))} - ${anchor.plusDays(6).format(DateTimeFormatter.ofPattern("MMMM d, yyyy", language.locale))}"
        CalendarMode.Month -> YearMonth.from(anchor).format(DateTimeFormatter.ofPattern("MMMM yyyy", language.locale))
    }
}
