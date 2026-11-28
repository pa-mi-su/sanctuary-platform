package app.sanctuary.android

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.ui.platform.LocalView
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import android.annotation.SuppressLint
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.ui.platform.testTag
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
import androidx.compose.material3.SheetValue
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
import app.sanctuary.android.ui.theme.SanctuaryAdvent
import app.sanctuary.android.ui.theme.SanctuaryChristmas
import app.sanctuary.android.ui.theme.SanctuaryLent
import app.sanctuary.android.ui.theme.SanctuaryEaster
import app.sanctuary.android.ui.theme.SanctuaryOrdinary
import app.sanctuary.android.ui.theme.SanctuaryText
import app.sanctuary.android.ui.theme.SanctuaryMuted
import app.sanctuary.android.ui.theme.SanctuaryOnAccent
import app.sanctuary.android.ui.theme.SanctuaryError
import app.sanctuary.android.ui.theme.SanctuaryErrorSurface
import app.sanctuary.android.ui.theme.SanctuaryWarning
import app.sanctuary.android.ui.theme.SanctuaryHeroBase
import app.sanctuary.android.ui.theme.SanctuaryGlowGold
import app.sanctuary.android.ui.theme.SanctuaryBlue
import app.sanctuary.android.ui.theme.SanctuaryHomeSurface
import app.sanctuary.android.ui.theme.SanctuaryHomeSurfaceRaised
import app.sanctuary.android.ui.theme.SanctuaryHomeInk
import app.sanctuary.android.ui.theme.SanctuaryHomeMuted
import app.sanctuary.android.ui.theme.SanctuaryTheme
import app.sanctuary.android.ui.theme.SanctuaryGradientBottom
import app.sanctuary.android.ui.theme.SanctuaryGradientMid
import app.sanctuary.android.ui.theme.SanctuaryGradientTop
import app.sanctuary.android.ui.theme.SanctuaryCardElevated
import app.sanctuary.android.ui.theme.SanctuaryToday
import app.sanctuary.android.ui.theme.SanctuaryCardBorder
import app.sanctuary.android.ui.theme.SanctuaryCardSoft
import app.sanctuary.android.ui.theme.SanctuaryCard
import app.sanctuary.android.ui.theme.SanctuaryTabActive
import app.sanctuary.android.ui.theme.SanctuaryTabBackground
import app.sanctuary.android.ui.theme.SanctuaryTabBorder
import app.sanctuary.android.ui.theme.SanctuaryTabInactive
import app.sanctuary.android.ui.AppLanguage
import app.sanctuary.android.ui.LocalSanctuaryStrings
import app.sanctuary.android.ui.SanctuaryStrings
import app.sanctuary.android.ui.sanctuaryStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

private fun Modifier.sanctuaryCardShadow(shape: RoundedCornerShape = RoundedCornerShape(24.dp)) =
    this.shadow(3.dp, shape, clip = false)

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
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            SanctuaryTheme {
                SanctuaryApp(viewModel)
            }
        }
        handleSharedContentIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.retrySessionOnResume()
        viewModel.syncPushDevice()
        AndroidReminderScheduler(this).resyncItems()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedContentIntent(intent)
    }

    private fun handleSharedContentIntent(intent: Intent?) {
        val uri = intent?.data ?: intent?.getStringExtra("destination")?.let(Uri::parse) ?: return
        if (!isSanctuaryNotificationLink(uri.toString(), BuildConfig.ENVIRONMENT)) return

        val segments = uri.pathSegments
        if (segments.isEmpty()) { viewModel.openHome(); return }
        if(segments[0]=="announcements"){viewModel.openAnnouncement(segments.getOrNull(1).orEmpty());return}
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

private sealed interface DeferredAccountAction {
    data class Favorite(
        val itemType: FavoriteItemType,
        val itemId: String,
        val displayName: String?,
        val slug: String?,
        val durationDays: Int?
    ) : DeferredAccountAction

    data class Reminder(val destination: String) : DeferredAccountAction
    data class StartNovena(val novenaId: String) : DeferredAccountAction
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

private enum class HomeAction(val titleKey: String, val subtitleKey: String, val icon: ImageVector) {
    Daily("home.daily", "home.dailySubtitle", Icons.Filled.WbSunny),
    Prayers("home.prayers", "home.prayersSubtitle", Icons.Filled.SelfImprovement),
    Patronage("home.patronage", "home.patronageSubtitle", Icons.Filled.People),
    Intentions("home.intentions", "home.intentionsSubtitle", Icons.Filled.Favorite),
    Rosary("home.rosary", "home.rosarySubtitle", Icons.Filled.SelfImprovement)
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
    val announcementRequest by viewModel.announcementRequest.collectAsState()
    val homeNavigation by viewModel.homeNavigation.collectAsState()
    LaunchedEffect(homeNavigation) { if (homeNavigation > 0) selectedTab = AppTab.Home }
    var showLanguagePicker by rememberSaveable { mutableStateOf(false) }

    CompositionLocalProvider(LocalSanctuaryStrings provides SanctuaryStrings(appLanguage)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            SanctuaryBackdrop()
            if (!session.isBootstrapping) {
                var invitationVisible by remember { mutableStateOf(true) }
                AnnouncementInvitation(onSync = viewModel::syncPushDevice, onVisibilityChanged = { invitationVisible = it })
                if (!invitationVisible) ReminderTimingRecovery(owner = session.profile?.userId)
            }
            if (session.isBootstrapping && session.status == SessionStatus.Loading && session.session == null) {
                BrandedLaunchScreen()
            } else {
                AuthenticatedShell(
                    requestedAnnouncement = announcementRequest,
                    consumeAnnouncement = viewModel::consumeAnnouncement,
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
    Box(Modifier.fillMaxSize().background(SanctuaryHomeSurface))
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
            Text(l10n.t("common.loading"), color = SanctuaryText)
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
    var step by remember(initialStep) {
        mutableStateOf(
            if (session.status == SessionStatus.AwaitingConfirmation) AuthStep.Confirm else initialStep
        )
    }
    var loginEmail by remember { mutableStateOf(session.pendingConfirmationEmail.orEmpty()) }
    var loginPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf(session.pendingConfirmationEmail.orEmpty()) }
    var registerPassword by remember { mutableStateOf("") }
    var registerPasswordConfirmation by remember { mutableStateOf("") }
    var confirmationCode by remember { mutableStateOf("") }
    var forgotEmail by remember { mutableStateOf(session.pendingPasswordResetEmail.orEmpty()) }
    var resetEmail by remember { mutableStateOf(session.pendingPasswordResetEmail.orEmpty()) }
    var resetCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var resetPasswordConfirmation by remember { mutableStateOf("") }

    fun clearForm() {
        loginEmail = ""; loginPassword = ""
        firstName = ""; lastName = ""; registerEmail = ""
        registerPassword = ""; registerPasswordConfirmation = ""; confirmationCode = ""
        forgotEmail = ""; resetEmail = ""; resetCode = ""
        newPassword = ""; resetPasswordConfirmation = ""
    }
    fun navigate(next: AuthStep) {
        clearForm()
        onAction.abandonAccountForm()
        step = next
    }
    DisposableEffect(Unit) {
        onDispose { onAction.abandonAccountForm() }
    }
    LaunchedEffect(step) {
        loginPassword = ""; if (step != AuthStep.Confirm) registerPassword = ""; registerPasswordConfirmation = ""
        newPassword = ""; resetPasswordConfirmation = ""; resetCode = ""; confirmationCode = ""
    }
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
    BackHandler(enabled = step != AuthStep.Landing && !isBusy) {
        navigate(when (step) {
            AuthStep.Confirm -> AuthStep.Register
            AuthStep.ResetPassword -> AuthStep.ForgotPassword
            AuthStep.ForgotPassword -> AuthStep.Login
            else -> AuthStep.Landing
        })
    }
    val l10n = sanctuaryStrings()
    val introTitle = if (embedded) l10n.t("auth.chooseWay") else l10n.t("auth.androidTitle")
    val introBody = if (embedded) {
        l10n.t("auth.chooseWayBody")
    } else {
        l10n.t("auth.androidIntro")
    }

    if (embedded) {
        MeAccountCard {
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
                onStepChange = { navigate(it) },
                onAction = onAction,
                showAccountEyebrow = true
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
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
                    onStepChange = { navigate(it) },
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
                text = l10n.t("auth.sanctuaryAccount"),
                color = SanctuaryTabActive,
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
            color = SanctuaryText,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (step == AuthStep.Landing) introBody else l10n.t("auth.accountBody"),
            color = SanctuaryMuted,
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
                    color = SanctuaryMuted
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
                    color = SanctuaryMuted
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
    requestedAnnouncement: String?,
    consumeAnnouncement: () -> Unit,
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
    fetchSaintsInRange: suspend (String, String) -> List<app.sanctuary.android.data.SaintDateGroup>,
    fetchNovenasInRange: suspend (String, String) -> List<app.sanctuary.android.data.NovenaCalendarDate>,
    fetchLiturgicalRange: suspend (String, String) -> List<app.sanctuary.android.data.LiturgicalDay>
) {
    val l10n = sanctuaryStrings()
    val context = LocalContext.current
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var settingsSection by rememberSaveable { mutableStateOf<String?>(null) }
    var showInbox by rememberSaveable { mutableStateOf(false) }
    var inboxRequestedID by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(requestedAnnouncement) { if(requestedAnnouncement!=null){inboxRequestedID=requestedAnnouncement.lowercase();showInbox=true;consumeAnnouncement()} }
    var inboxUnread by remember { mutableStateOf(false) }
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
    var resumeReminder by remember { mutableStateOf<String?>(null) }
    var deferredAccountAction by remember { mutableStateOf<DeferredAccountAction?>(null) }
    var accountReturnTab by rememberSaveable { mutableStateOf<AppTab?>(null) }
    var saintsCalendarMode by rememberSaveable { mutableStateOf(CalendarMode.Day) }
    var novenaDetailYear by rememberSaveable { mutableStateOf(LocalDate.now().year) }
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
    val contentScrollState = rememberLazyListState()
    val homeHeroHeight = with(LocalDensity.current) { (470.dp * this.fontScale.coerceAtLeast(1f) + 60.dp).roundToPx() }
    val lightHomeHeader = selectedTab == AppTab.Home && contentScrollState.firstVisibleItemScrollOffset >= homeHeroHeight
    SideEffect {
        (context as? android.app.Activity)?.let { activity ->
            androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .isAppearanceLightStatusBars = selectedTab != AppTab.Home || lightHomeHeader
        }
    }


    LaunchedEffect(session.status, novenaProgress.isLoading) {
        if (
            session.status == SessionStatus.Authenticated &&
            requestedAuthStep != null &&
            !novenaProgress.isLoading
        ) {
            val action = deferredAccountAction
            val returnTab = accountReturnTab ?: AppTab.Home
            requestedAuthStep = null
            deferredAccountAction = null
            accountReturnTab = null
            onTabSelected(returnTab)
            when (action) {
                is DeferredAccountAction.Favorite -> {
                    val isAlreadyFavorite = novenaProgress.favorites.any {
                        it.itemType == action.itemType && it.itemId == action.itemId
                    }
                    if (!isAlreadyFavorite) {
                        onToggleFavorite(
                            action.itemType,
                            action.itemId,
                            action.displayName,
                            action.slug,
                            action.durationDays
                        )
                    }
                }
                is DeferredAccountAction.Reminder -> { resumeReminder = action.destination }
                is DeferredAccountAction.StartNovena -> {
                    val alreadyStarted = novenaProgress.commitments.any {
                        it.novenaId == action.novenaId &&
                            (it.status == CommitmentStatus.Active || it.status == CommitmentStatus.Completed)
                    }
                    if (!alreadyStarted) {
                        onStartNovena(action.novenaId)
                    }
                }
                null -> Unit
            }
        }
    }

    fun requireAccount(action: DeferredAccountAction) {
        deferredAccountAction = action
        accountReturnTab = selectedTab
        showAccountRequiredPrompt = true
    }

    fun openAccountAccess(step: AuthStep) {
        showAccountRequiredPrompt = false
        requestedAuthStep = step
        onTabSelected(AppTab.Me)
    }

    val keyboardCoversBottom = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            // Navigation belongs at the device bottom, underneath the IME.
            if (!showSettings && !showInbox && !keyboardCoversBottom) Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SanctuaryHomeSurface
            ) {
                Column {
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(SanctuaryCardBorder))
                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = Color.Transparent,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        tonalElevation = 0.dp
                    ) {
                        AppTab.entries.filter { it != AppTab.Liturgical }.forEach { tab ->
                            NavigationBarItem(
                                modifier = Modifier.weight(1f),
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
                                    selectedIconColor = SanctuaryHomeInk,
                                    selectedTextColor = SanctuaryHomeInk,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = SanctuaryHomeMuted,
                                    unselectedTextColor = SanctuaryHomeMuted
                                ),
                                icon = {
                                    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                                        Icon(imageVector = tab.icon(), contentDescription = null, modifier = Modifier.size(26.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(if (selectedTab == AppTab.Home) { if (lightHomeHeader) SanctuaryHomeSurface else SanctuaryHeroBase } else Color.Transparent)) {
        LazyColumn(
            state = contentScrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .background(if (selectedTab == AppTab.Home) SanctuaryHomeSurface else Color.Transparent),
            contentPadding = if (selectedTab == AppTab.Home) PaddingValues(bottom = 96.dp) else PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                AppTab.Home -> {
                    item {
                        HomeLanding(isObscured = showPrayerSearch || showRosarySearch || showIntentionsSearch
                            || showPatronageSearch || dailyReadingsUrl != null || isLoadingDailyReadings
                            || showSettings || showInbox) { action ->
                            when (action) {
                                HomeAction.Daily -> {
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
                                HomeAction.Prayers -> {
                                    showPrayerSearch = true
                                    if (prayers.items.isEmpty() && !prayers.isLoading) {
                                        onReloadPrayers()
                                    }
                                }
                                HomeAction.Patronage -> {
                                    showPatronageSearch = true
                                    if (patronageTerms.terms.isEmpty() && !patronageTerms.isLoading) {
                                        onReloadPatronageTerms()
                                    }
                                }
                                HomeAction.Intentions -> {
                                    showIntentionsSearch = true
                                    if (intentionTerms.terms.isEmpty() && !intentionTerms.isLoading) {
                                        onReloadIntentionTerms()
                                    }
                                }
                                HomeAction.Rosary -> {
                                    showRosarySearch = true
                                    if (rosaries.items.isEmpty() && !rosaries.isLoading) {
                                        onReloadRosaries()
                                    }
                                }
                            }
                        }
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
                                onYearChanged = { novenaDetailYear = it },
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
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
                            MeHeaderAction(sanctuaryStrings().t("inbox.title"), Icons.Filled.NotificationsNone, inboxUnread) { inboxRequestedID=null; showInbox=true }
                            if (session.status == SessionStatus.Authenticated) {
                                Row(Modifier.weight(1f).padding(horizontal=10.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                    SanctuaryHaloAvatar(Modifier.size(44.dp))
                                    Text(session.profile?.displayName ?: session.session?.displayName ?: sanctuaryStrings().t("me.title"), color=SanctuaryText, fontSize=24.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(1f))
                                }
                            } else Text(sanctuaryStrings().t("me.title"), color=SanctuaryText, fontSize=32.sp, fontWeight=FontWeight.Bold)
                            MeHeaderAction(sanctuaryStrings().t("settings.title"), Icons.Filled.Settings) { settingsSection=null; showSettings=true }
                        }
                    }
                    if (session.status != SessionStatus.Authenticated) { item { MeIdentityHero() } }
                    if (session.status == SessionStatus.Authenticated) {
                        item {
                            MeScreen(
                                session = session,
                                progress = novenaProgress,
                                onOpenNovena = onOpenNovena,
                                onOpenSaint = onOpenSaint,
                                onOpenPrayer = onOpenPrayer,
                                onLogout = onLogout,
                                onDeleteAccount = onDeleteAccount
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

        androidx.lifecycle.compose.LifecycleResumeEffect(session.session?.email) {
            scope.launch { runCatching { announcementUnread(context,session.status == SessionStatus.Authenticated) }.onSuccess { inboxUnread=it } }
            onPauseOrDispose { }
        }
        LaunchedEffect(selectedTab,session.session?.email) {
            runCatching { announcementUnread(context,session.status == SessionStatus.Authenticated) }.onSuccess { inboxUnread=it }
        }
        SanctuarySlidePage(visible=showInbox, onBack={showInbox=false;inboxRequestedID=null}) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top=64.dp,bottom=32.dp)) {
                AnnouncementInboxSheet(initialID=inboxRequestedID,authenticated=session.status == SessionStatus.Authenticated,onUnread={inboxUnread=it},onOpen={url ->
                    showInbox=false;inboxRequestedID=null
                    context.startActivity(Intent(context,MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                })
            }
        }
        SanctuarySlidePage(visible=showSettings, onBack={if (settingsSection != null) settingsSection=null else showSettings=false}) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().imePadding(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 76.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { Text(sanctuaryStrings().t(settingsSection ?: "settings.title"), fontSize = 32.sp, color = SanctuaryText, fontWeight = FontWeight.Bold) }
                    if (settingsSection == null) {
                        if (session.status == SessionStatus.Authenticated) {
                            item { SettingsProfileCard(session) { settingsSection="settings.account" } }
                        } else {
                            item { SettingsNavigationRow(title=sanctuaryStrings().t("settings.account")) { settingsSection="settings.account" } }
                        }
                        listOf("settings.general", "notifications.title", "settings.support").forEach { section ->
                            item { SettingsNavigationRow(title = sanctuaryStrings().t(section)) { settingsSection=section } }
                        }
                        item { SettingsNavigationRow(title = sanctuaryStrings().t("home.about")) { showAbout = true } }
                        if (session.status == SessionStatus.Authenticated) {
                            item { SettingsNavigationRow(title=sanctuaryStrings().t("me.logout"), onClick=onLogout) }
                        }
                        item { Text(BuildConfig.VERSION_NAME, color=SanctuaryMuted, modifier=Modifier.fillMaxWidth(), textAlign=TextAlign.Center) }
                    } else when (settingsSection) {
                        "settings.general" -> item { LanguageSettingsButton(language = selectedLanguage, onClick = onShowLanguagePicker) }
                        "notifications.title" -> item { MeSectionCard(title = sanctuaryStrings().t("notifications.title")) { AnnouncementPreference() } }
                        "settings.support", "settings.legal" -> {
                            item { SettingsNavigationRow(sanctuaryStrings().t("about.emailSupport")) { openSupportEmail() } }
                            item { SettingsNavigationRow(sanctuaryStrings().t("about.link.support")) { aboutDocument=AboutDocument.Support } }
                            item { SettingsNavigationRow(sanctuaryStrings().t("about.link.privacy")) { aboutDocument=AboutDocument.Privacy } }
                        }
                        "settings.account" -> item {
                            if (session.status == SessionStatus.Authenticated) MeScreen(session, novenaProgress, onOpenNovena, onOpenSaint, onOpenPrayer, onLogout, onDeleteAccount, settingsMode = true)
                            else AccountAccessScreen(session=session, onAction=onAction, embedded=true, initialStep=AuthStep.Landing)
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
            SanctuaryModalSheet(onDismissRequest = { dailyReadingsUrl = null }, scrollContent = false, showBackButton = false) {
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
                    title = l10n.t("home.rosary"),
                    query = rosaries.query,
                    onQueryChanged = onRosaryQueryChanged,
                    onSubmit = onReloadRosaries,
                    isLoading = rosaries.isLoading,
                    error = rosaries.error,
                    emptyLabel = l10n.t("search.rosaryTitle"),
                    showSearch = false,
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

        if (requestedAuthStep == null && (saintDetail.isLoading || saintDetail.item != null || saintDetail.error != null)) {
            SanctuaryModalSheet(onDismissRequest = onCloseSaintDetail, heroContent = saintDetail.item != null) {
                when {
                    saintDetail.isLoading -> DetailLoadingSheet(l10n.t("common.loading"))
                    saintDetail.error != null -> DetailErrorSheet(saintDetail.error, onCloseSaintDetail)
                    saintDetail.item != null -> SaintDetailSheet(
                        detail = saintDetail.item,
                        session = session,
                        progress = novenaProgress,
                        onToggleFavorite = onToggleFavorite,
                        onRequireAccount = { detail -> requireAccount(detail) },
                        onDismiss = onCloseSaintDetail
                    )
                }
            }
        }

        if (requestedAuthStep == null && (novenaDetail.isLoading || novenaDetail.item != null || novenaDetail.error != null)) {
            SanctuaryModalSheet(onDismissRequest = onCloseNovenaDetail, heroContent = novenaDetail.item != null) {
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
                        onRequireAccount = { detail -> requireAccount(detail) },
                        resumeReminder = resumeReminder,
                        onReminderResumed = { resumeReminder = null },
                        displayYear = if (selectedTab == AppTab.Novenas) novenaDetailYear else LocalDate.now().year,
                        onDismiss = onCloseNovenaDetail
                    )
                }
            }
        }

        if (requestedAuthStep == null && (prayerDetail.isLoading || prayerDetail.item != null || prayerDetail.error != null)) {
            SanctuaryModalSheet(onDismissRequest = onClosePrayerDetail, heroContent = prayerDetail.item != null) {
                when {
                    prayerDetail.isLoading -> DetailLoadingSheet(l10n.t("common.loading"))
                    prayerDetail.error != null -> DetailErrorSheet(prayerDetail.error, onClosePrayerDetail)
                    prayerDetail.item != null -> PrayerDetailSheet(
                        detail = prayerDetail.item,
                        session = session,
                        progress = novenaProgress,
                        onToggleFavorite = onToggleFavorite,
                        onRequireAccount = { detail -> requireAccount(detail) },
                        resumeReminder = resumeReminder,
                        onReminderResumed = { resumeReminder = null },
                        onDismiss = onClosePrayerDetail
                    )
                }
            }
        }

        if (showAccountRequiredPrompt) {
            AlertDialog(
                onDismissRequest = {
                    showAccountRequiredPrompt = false
                    deferredAccountAction = null
                    accountReturnTab = null
                },
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
                        TextButton(onClick = {
                            showAccountRequiredPrompt = false
                            deferredAccountAction = null
                            accountReturnTab = null
                        }) {
                            Text(l10n.t("accountRequired.dismiss"))
                        }
                    }
                }
            )
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCard),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SanctuaryCardBorder),
        onClick = onClick,
        enabled = enabled
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(eyebrow.uppercase(), color = SanctuaryTabActive, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = SanctuaryText, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(body, color = SanctuaryMuted, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun AuthCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        border = BorderStroke(1.dp, SanctuaryCardBorder),
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
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        modifier = Modifier.sanctuaryCardShadow(RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) SanctuaryErrorSurface else SanctuaryCardSoft
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = if (isError) SanctuaryError else SanctuaryText,
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
    var passwordVisible by remember(value.isEmpty()) { mutableStateOf(false) }
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
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        onClick = onClick,
        enabled = enabled && !isBusy,
        colors = ButtonDefaults.buttonColors(containerColor = SanctuaryBlue),
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
                color = if (enabled && !isBusy) SanctuaryOnAccent else SanctuaryMuted,
                strokeWidth = 2.dp
            )
            }
            Text(title, color = if (enabled && !isBusy) SanctuaryOnAccent else SanctuaryMuted)
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
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        onClick = onClick,
        enabled = enabled && !isBusy,
        colors = ButtonDefaults.buttonColors(
            containerColor = SanctuaryCardSoft,
            contentColor = SanctuaryTabActive
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
                    color = SanctuaryTabActive,
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
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCardSoft),
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
                    color = SanctuaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = strengthLabel,
                    color = if (rules.all { it.met }) SanctuaryTabActive else SanctuaryMuted,
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
                        color = if (rule.met) SanctuaryTabActive else SanctuaryMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = rule.label,
                        color = if (rule.met) SanctuaryText else SanctuaryMuted
                    )
                }
            }

            Text(
                text = if (matches) l10n.t("auth.passwordsMatch") else confirmationWarning,
                color = if (matches) SanctuaryTabActive else SanctuaryMuted
            )
        }
    }
}

@Composable
private fun HomeCard(session: SessionUiState) {
    val l10n = sanctuaryStrings()
    val profile = session.profile
    Card(
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCard),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = profile?.displayName ?: session.session?.displayName ?: l10n.t("me.accountFallback"),
                color = SanctuaryText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = session.session?.email.orEmpty(),
                color = SanctuaryMuted
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
private fun SanctuaryHaloAvatar(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w=size.width; val h=size.height
        drawCircle(SanctuaryHomeSurfaceRaised, radius=w*.48f, center=Offset(w*.5f,h*.5f))
        drawOval(SanctuaryGlowGold, topLeft=Offset(w*.29f,h*.15f), size=Size(w*.42f,h*.10f), style=Stroke(width=w*.035f))
        drawOval(SanctuaryBlue, topLeft=Offset(w*.28f,h*.29f), size=Size(w*.44f,h*.39f))
        drawCircle(SanctuaryOnAccent, radius=w*.135f, center=Offset(w*.5f,h*.46f))
        drawCircle(SanctuaryHomeInk, radius=w*.016f, center=Offset(w*.455f,h*.45f))
        drawCircle(SanctuaryHomeInk, radius=w*.016f, center=Offset(w*.545f,h*.45f))
        drawOval(SanctuaryBlue, topLeft=Offset(w*.25f,h*.64f), size=Size(w*.5f,h*.23f))
        drawLine(SanctuaryChristmas,Offset(w*.5f,h*.68f),Offset(w*.5f,h*.79f),strokeWidth=w*.025f,cap=StrokeCap.Round)
        drawLine(SanctuaryChristmas,Offset(w*.46f,h*.72f),Offset(w*.54f,h*.72f),strokeWidth=w*.025f,cap=StrokeCap.Round)
    }
}

@Composable
private fun MeIdentityHero() {
    Column(Modifier.fillMaxWidth().padding(vertical=12.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SanctuaryHaloAvatar(Modifier.size(112.dp))
    }
}

@Composable
private fun SettingsProfileCard(session: SessionUiState, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SanctuaryCard).border(1.dp,SanctuaryCardBorder,RoundedCornerShape(20.dp)).clickable(onClick=onClick).padding(18.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        SanctuaryHaloAvatar(Modifier.size(60.dp))
        Column(Modifier.weight(1f), verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(session.profile?.displayName ?: session.session?.displayName ?: sanctuaryStrings().t("settings.account"), color=SanctuaryText, fontSize=22.sp, fontWeight=FontWeight.Bold)
            Text(session.profile?.email ?: session.session?.email.orEmpty(), color=SanctuaryMuted, fontSize=14.sp)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription=null, tint=SanctuaryTabActive)
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
    settingsMode: Boolean = false
) {
    val l10n = sanctuaryStrings()
    val context = LocalContext.current
    val profile = session.profile
    val favoriteNovenas = progress.favorites.filter { it.itemType == FavoriteItemType.Novena }
    val favoriteSaints = progress.favorites.filter { it.itemType == FavoriteItemType.Saint }
    val favoritePrayers = progress.favorites.filter { it.itemType == FavoriteItemType.Prayer }
    var showDeleteAccountDialog by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (settingsMode) {
        Card(
        border = BorderStroke(1.dp, SanctuaryCardBorder),
            colors = CardDefaults.cardColors(containerColor = SanctuaryCard),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(l10n.t("me.signedIn"), color = SanctuaryTabActive, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                            color = SanctuaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            profile?.displayName ?: session.session?.displayName ?: l10n.t("me.accountFallback"),
                            color = SanctuaryText,
                            fontSize = 30.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            profile?.email ?: session.session?.email.orEmpty(),
                            color = SanctuaryMuted
                        )

                    }
                }
                if(settingsMode) {
                Button(
                    border = BorderStroke(1.dp, SanctuaryCardBorder),
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = SanctuaryCardSoft),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(l10n.t("me.logout"), color = SanctuaryText)
                }
                }
            }
        }
        }


        if(!settingsMode) {
        ScheduledRemindersCard(onOpenNovena = onOpenNovena, onOpenPrayer = onOpenPrayer)

        MeSectionCard(title = l10n.t("me.inProgress")) {
            if (progress.commitments.none { it.status == CommitmentStatus.Active }) {
                Text(l10n.t("me.noInProgress"), color = SanctuaryMuted)
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
                Text(l10n.t("me.noneFavoriteNovenas"), color = SanctuaryMuted)
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

        MeSectionCard(title=l10n.t("me.completed")) {
            val completed=progress.commitments.filter { it.status == CommitmentStatus.Completed }
            if(completed.isEmpty())Text(l10n.t("me.noCompleted"),color=Color.LightGray)
            completed.forEach { item -> LinkedMeRow(title=progress.novenaTitles[item.novenaId] ?: item.novenaId,subtitle=null,onClick={onOpenNovena(item.novenaId)}) }
        }
        MeSectionCard(title = l10n.t("me.favoriteSaints")) {
            if (favoriteSaints.isEmpty()) {
                Text(l10n.t("me.noneFavoriteSaints"), color = SanctuaryMuted)
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
                Text(l10n.t("me.noneFavoritePrayers"), color = SanctuaryMuted)
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

        }
        if(settingsMode) {
        MeSectionCard(title = l10n.t("me.deleteAccount.title")) {
            Text(
                l10n.t("me.deleteAccount.body"),
                color = SanctuaryMuted,
                lineHeight = 20.sp
            )
            Button(
                border = BorderStroke(1.dp, SanctuaryCardBorder),
                onClick = { showDeleteAccountDialog = true },
                enabled = !session.isDeletingAccount,
                colors = ButtonDefaults.buttonColors(containerColor = SanctuaryError),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (session.isDeletingAccount) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 8.dp),
                        color = SanctuaryOnAccent,
                        strokeWidth = 2.dp
                    )
                }
                Text(
                    l10n.t(if (session.isDeletingAccount) "me.deleteAccount.deleting" else "me.deleteAccount.action"),
                    color = SanctuaryOnAccent,
                    fontWeight = FontWeight.Bold
                )
            }
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
                    Text(l10n.t("me.deleteAccount.confirmAction"), color = SanctuaryError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(l10n.t("common.cancel"))
                }
            },
            containerColor = SanctuaryCard,
            titleContentColor = SanctuaryText,
            textContentColor = SanctuaryMuted
        )
    }
}

@Composable
private fun MeSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SanctuaryCardBorder),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, color = SanctuaryText, fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().height(1.dp).background(SanctuaryCardBorder))
            content()
        }
    }
}

@Composable
private fun MeAccountCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SanctuaryCardBorder),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(Modifier.fillMaxWidth().padding(20.dp)) { content() }
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
            .background(if (checked) SanctuaryCardSoft else SanctuaryCardSoft)
            .border(1.dp, if (checked) SanctuaryTabActive else SanctuaryCardBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = SanctuaryText, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = SanctuaryMuted, lineHeight = 18.sp, fontSize = 13.sp)
        }
        Switch(
            modifier = Modifier.semantics { contentDescription = title },
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SanctuaryOnAccent,
                checkedTrackColor = SanctuaryBlue,
                checkedBorderColor = SanctuaryBlue,
                uncheckedThumbColor = SanctuaryOnAccent,
                uncheckedTrackColor = SanctuaryCardBorder,
                uncheckedBorderColor = SanctuaryCardBorder,
                disabledCheckedThumbColor = SanctuaryOnAccent.copy(alpha = 0.9f),
                disabledCheckedTrackColor = SanctuaryTabActive.copy(alpha = 0.55f),
                disabledUncheckedThumbColor = SanctuaryOnAccent.copy(alpha = 0.75f),
                disabledUncheckedTrackColor = SanctuaryOnAccent.copy(alpha = 0.10f)
            )
        )
    }
}

@Composable
private fun SettingsNavigationRow(title: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, SanctuaryCardBorder),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCardSoft)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, Modifier.weight(1f), color = SanctuaryText, fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SanctuaryTabActive, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun LinkedMeRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Card(
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCardSoft),
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
                    color = SanctuaryText,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.let {
                    Text(it, color = SanctuaryMuted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SanctuaryText.copy(alpha = 0.85f))
        }
    }
}

private fun initialsFor(name: String): String {
    val parts = name.split(" ").filter { it.isNotBlank() }
    if (parts.isEmpty()) return "S"
    return parts.take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifBlank { "S" }
}

private val homeFeaturedActions = listOf(
    HomeAction.Daily, HomeAction.Prayers, HomeAction.Patronage,
    HomeAction.Intentions, HomeAction.Rosary
)

@Composable
private fun HomeLanding(isObscured: Boolean, onOpen: (HomeAction) -> Unit) {
    val l10n = sanctuaryStrings()
    val pageCount = homeFeaturedActions.size + 1
    // Start on a welcome page near the midpoint, leaving room to swipe either way.
    val initialPage = (Int.MAX_VALUE / 2 / pageCount) * pageCount
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })
    val scope = rememberCoroutineScope()
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val context = LocalContext.current
    val view = LocalView.current
    var windowFocused by remember { mutableStateOf(view.hasWindowFocus()) }
    var paused by rememberSaveable { mutableStateOf(false) }
    var manualSelection by remember { mutableStateOf(0) }
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val selectedPage = pagerState.currentPage % pageCount
    DisposableEffect(view) {
        val listener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { windowFocused = it }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose { if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnWindowFocusChangeListener(listener) }
    }
    LaunchedEffect(pagerState, isDragged, paused, isObscured, windowFocused, manualSelection) {
        val accessibility = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        if (!paused && !isObscured && !isDragged && windowFocused
            && !accessibility.isTouchExplorationEnabled && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                while (true) {
                    delay(7_000)
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().background(SanctuaryHomeSurface), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
            Column(Modifier.background(SanctuaryHeroBase)) {
                Box {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().height((470 * fontScale).dp)
                    ) { index ->
                        val logicalPage = index % pageCount
                        if (logicalPage == 0) {
                            HomeWelcomeSlide()
                        } else {
                            val action = homeFeaturedActions[logicalPage - 1]
                            Box(Modifier.fillMaxSize().clickable(role = Role.Button, onClickLabel = l10n.t("home.open")) { onOpen(action) }) {
                                HomeEditorialBackground(action.editorialAsset())
                                Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 90.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Spacer(Modifier.weight(1f))
                                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                                            tint = SanctuaryHeroBase, modifier = Modifier.size(60.dp)
                                                .background(SanctuaryOnAccent, CircleShape).padding(18.dp))
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Text(l10n.t(action.subtitleKey), color = SanctuaryOnAccent, fontSize = 12.sp,
                                        modifier = Modifier.background(SanctuaryOnAccent.copy(alpha = 0.18f), RoundedCornerShape(5.dp))
                                            .padding(horizontal = 9.dp, vertical = 5.dp))
                                    Text(l10n.t(action.titleKey), color = SanctuaryOnAccent,
                                        fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                                    Text(l10n.t("home.open"), color = SanctuaryOnAccent.copy(alpha = 0.85f), fontSize = 16.sp)
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(l10n.t("tab.home"), color = SanctuaryOnAccent, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Image(painterResource(R.drawable.brand_logo), "Sanctuary",
                            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    repeat(pageCount) { index ->
                        val isSelected = selectedPage == index
                        Box(
                            Modifier.size(width = 44.dp, height = 48.dp)
                                .semantics {
                                    contentDescription = if (index == 0) l10n.t("home.welcome") else l10n.t(homeFeaturedActions[index - 1].titleKey)
                                    selected = isSelected
                                    stateDescription = "${index + 1} / $pageCount"
                                }
                                .clickable(role = Role.Tab) {
                                    manualSelection += 1
                                    scope.launch { pagerState.scrollToPage(pagerState.currentPage - selectedPage + index) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.size(width = if (isSelected) 20.dp else 5.dp, height = 5.dp)
                                .background(SanctuaryOnAccent.copy(alpha = if (isSelected) 0.95f else 0.3f), CircleShape))
                        }
                    }
                    IconButton(onClick = { paused = !paused }) {
                        Icon(if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            l10n.t(if (paused) "home.resumeCarousel" else "home.pauseCarousel"),
                            tint = SanctuaryOnAccent.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(36.dp).background(Brush.verticalGradient(
                listOf(SanctuaryHeroBase, SanctuaryHomeSurface))))
            Column(Modifier.background(SanctuaryHomeSurface).padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    homeFeaturedActions.filter { it != HomeAction.Rosary }.forEach { action ->
                        Column(Modifier.weight(1f).clickable(role = Role.Button) { onOpen(action) },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Box(Modifier.fillMaxWidth().height(62.dp).background(SanctuaryHomeSurfaceRaised, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center) {
                                Icon(action.icon, null, tint = SanctuaryHomeInk, modifier = Modifier.size(24.dp))
                            }
                            Text(l10n.t(action.titleKey), color = SanctuaryHomeInk, fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("MATTHEW 11:28", color = SanctuaryHomeMuted, fontSize = 12.sp, letterSpacing = 1.2.sp)
                    Text(l10n.t("home.verse"), color = SanctuaryHomeInk, fontSize = 24.sp, lineHeight = 32.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, textAlign = TextAlign.Center)
                }
                HomeDailyPrayerList(l10n.t("home.explore"), listOf(HomeAction.Daily, HomeAction.Prayers, HomeAction.Rosary), onOpen)
                HomeFeatureShelf(l10n.t("home.guidance"), listOf(HomeAction.Patronage, HomeAction.Intentions), onOpen)
            }
        }
    }
}

private fun HomeAction.editorialAsset(): String = when (this) {
    HomeAction.Rosary -> "rosary"
    HomeAction.Patronage -> "welcome"
    else -> "scripture"
}

@Composable
private fun HomeEditorialArtwork(asset: String, modifier: Modifier = Modifier) {
    AsyncImage(model = "file:///android_asset/home_editorial/$asset.png", contentDescription = null,
        contentScale = ContentScale.Crop, modifier = modifier)
}

@Composable
private fun HomeEditorialBackground(asset: String) {
    Box(Modifier.fillMaxSize()) {
        HomeEditorialArtwork(asset, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.25f), 0.38f to Color.Transparent,
            0.73f to SanctuaryHeroBase.copy(alpha = 0.8f), 1f to SanctuaryHeroBase)))
    }
}

@Composable
private fun HomeWelcomeSlide() {
    val l10n = sanctuaryStrings()
    Box(Modifier.fillMaxSize()) {
        HomeEditorialBackground("welcome")
        Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom)) {
            Text(l10n.t("home.heroEyebrow"), color = SanctuaryOnAccent, fontSize = 12.sp,
                modifier = Modifier.background(SanctuaryOnAccent.copy(alpha = 0.18f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp))
            Text(l10n.t("home.welcome"), color = SanctuaryOnAccent, fontSize = 28.sp, lineHeight = 34.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            Text(l10n.t("home.welcomeBody"), color = SanctuaryOnAccent.copy(alpha = 0.85f), fontSize = 16.sp, lineHeight = 23.sp)
        }
    }
}

@Composable
private fun HomeDailyPrayerList(title: String, actions: List<HomeAction>, onOpen: (HomeAction) -> Unit) {
    val l10n = sanctuaryStrings()
    Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, color = SanctuaryHomeInk, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() })
        actions.forEach { action ->
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpen(action) },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HomeEditorialArtwork(action.editorialAsset(), Modifier.size(68.dp).clip(RoundedCornerShape(10.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(l10n.t(action.titleKey), color = SanctuaryHomeInk, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(l10n.t(action.subtitleKey), color = SanctuaryHomeMuted, fontSize = 14.sp)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = SanctuaryHomeMuted, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun HomeFeatureShelf(title: String, actions: List<HomeAction>, onOpen: (HomeAction) -> Unit) {
    val l10n = sanctuaryStrings()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, color = SanctuaryHomeInk, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() })
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.forEach { action ->
                Column(Modifier.weight(1f).clickable(role = Role.Button) { onOpen(action) },
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeEditorialArtwork(action.editorialAsset(), Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(12.dp)))
                    Text(l10n.t(action.titleKey), color = SanctuaryHomeInk, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(l10n.t(action.subtitleKey), color = SanctuaryHomeMuted, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
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
            Text(l10n.t("about.brand"), color = SanctuaryTabActive, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(l10n.t("about.title"), color = SanctuaryText, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
            Text(
                l10n.t("about.subtitle"),
                color = SanctuaryMuted,
                lineHeight = 21.sp
            )
        }

        AboutInfoCard(title = l10n.t("about.desktopVersion")) {
            Text(
                l10n.t("about.desktopBody"),
                color = SanctuaryMuted,
                lineHeight = 21.sp
            )
            PrimarySheetButton(title = l10n.t("about.link.desktop"), onClick = onOpenDesktop)
        }

        AboutInfoCard(title = l10n.t("about.versionTitle")) {
            Text(
                "${l10n.t("about.versionLabel")}: ${BuildConfig.VERSION_NAME}",
                color = SanctuaryMuted,
                lineHeight = 21.sp
            )
            Text(
                "${l10n.t("about.buildLabel")}: ${BuildConfig.VERSION_CODE}",
                color = SanctuaryMuted,
                lineHeight = 21.sp
            )
            if (BuildConfig.ENVIRONMENT != "prod") {
                Text(
                    "${l10n.t("about.environmentLabel")}: ${BuildConfig.ENVIRONMENT.uppercase()}",
                    color = SanctuaryMuted,
                    lineHeight = 21.sp
                )
            }
        }

        AboutInfoCard(title = l10n.t("about.whatsInApp")) {
            Text("• ${l10n.t("about.whatsInApp.liturgical")}", color = SanctuaryMuted, lineHeight = 21.sp)
            Text("• ${l10n.t("about.whatsInApp.saints")}", color = SanctuaryMuted, lineHeight = 21.sp)
            Text("• ${l10n.t("about.whatsInApp.novenas")}", color = SanctuaryMuted, lineHeight = 21.sp)
        }

        AboutInfoCard(title = l10n.t("about.references")) {
            Text(l10n.t("about.referencesBody"), color = SanctuaryMuted, lineHeight = 21.sp)
            Text("• ${l10n.t("about.reference.usccb")}", color = SanctuaryMuted)
            Text("• ${l10n.t("about.reference.wikipedia")}", color = SanctuaryMuted)
            PrimarySheetButton(title = l10n.t("about.link.usccb"), onClick = onOpenUsccb)
            PrimarySheetButton(title = l10n.t("about.link.wikipedia"), onClick = onOpenWikipedia)
        }

        AboutInfoCard(title = l10n.t("about.contact")) {
            Text(
                l10n.t("about.contactBody"),
                color = SanctuaryMuted,
                lineHeight = 21.sp
            )
            PrimarySheetButton(title = l10n.t("about.emailSupport"), onClick = onEmailSupport)
            SecondarySheetButton(title = l10n.t("about.link.support"), onClick = onOpenSupport)
            SecondarySheetButton(title = l10n.t("about.link.privacy"), onClick = onOpenPrivacy)
        }

        Text(
            l10n.t("about.copyright"),
            color = SanctuaryMuted,
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
                Text(body, color = SanctuaryMuted, lineHeight = 21.sp)
            }
        }
        PrimarySheetButton(title = l10n.t("about.emailSupport"), onClick = onEmailSupport)
        Text(
            l10n.t("about.copyright"),
            color = SanctuaryMuted,
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
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCard),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Text(title, color = SanctuaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                content()
            }
        )
    }
}

@Composable
private fun PrimarySheetButton(title: String, onClick: () -> Unit) {
    Button(
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp), clip = false)
            .background(
                brush = Brush.linearGradient(
                    listOf(SanctuaryBlue, SanctuaryBlue)
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
    ) {
        Text(title, color = SanctuaryOnAccent, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecondarySheetButton(title: String, onClick: () -> Unit) {
    Button(
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .border(1.dp, SanctuaryCardBorder.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
    ) {
        Text(title, color = SanctuaryText, fontWeight = FontWeight.SemiBold)
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
private fun MeHeaderAction(title: String, icon: ImageVector, unread: Boolean = false, onClick: () -> Unit) {
    Box {
        Surface(onClick=onClick, modifier=Modifier.size(56.dp), shape=RoundedCornerShape(18.dp),
            color=SanctuaryCard, border=BorderStroke(1.5.dp,SanctuaryTabActive.copy(alpha=0.7f))) {
            Box(contentAlignment=Alignment.Center) { Icon(icon, contentDescription=title, tint=SanctuaryTabActive, modifier=Modifier.size(28.dp)) }
        }
        if(unread) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(12.dp).background(SanctuaryError,CircleShape).border(2.dp,SanctuaryCard,CircleShape))
    }
}

@Composable
private fun SanctuarySlidePage(visible: Boolean, onBack: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(enabled=visible, onBack=onBack)
    AnimatedVisibility(visible=visible, enter=slideInHorizontally(tween(280)) { it }, exit=slideOutHorizontally(tween(280)) { it }) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(SanctuaryGradientTop,SanctuaryGradientMid,SanctuaryGradientBottom))).statusBarsPadding().navigationBarsPadding()) {
            content()
            Surface(onClick=onBack, modifier=Modifier.align(Alignment.TopStart).padding(start=18.dp,top=12.dp).size(42.dp),
                color=SanctuaryCard, shape=CircleShape, border=BorderStroke(1.dp,SanctuaryCardBorder)) {
                Box(contentAlignment=Alignment.Center) { Icon(Icons.Filled.ArrowBack,contentDescription="Back",tint=SanctuaryText,modifier=Modifier.size(22.dp)) }
            }
        }
    }
}

@Composable
private fun NavigationCardAction(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCard), border = BorderStroke(1.dp, SanctuaryCardBorder)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = SanctuaryTabActive, modifier = Modifier.size(25.dp))
            Text(title, Modifier.weight(1f), color = SanctuaryText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SanctuaryTabActive, modifier = Modifier.size(20.dp))
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
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(4.dp, shape, clip = false),
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
                            SanctuaryCardSoft,
                            SanctuaryCardSoft,
                            SanctuaryCardSoft
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
                        tint = SanctuaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    color = SanctuaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = SanctuaryMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LanguageSettingsButton(language: AppLanguage, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCardSoft),
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Filled.Language, contentDescription = null, tint = SanctuaryTabActive, modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(sanctuaryStrings().t("home.language"), color = SanctuaryText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(language.displayName, color = SanctuaryMuted, fontSize = 16.sp)
            }
            Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = SanctuaryText)
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = l10n.t("home.chooseLanguage"),
            color = SanctuaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        AppLanguage.entries.forEach { language ->
            Card(
        border = BorderStroke(1.dp, SanctuaryCardBorder),
                colors = CardDefaults.cardColors(
                    containerColor = if (language == current) SanctuaryCardSoft else SanctuaryCard
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
                    Text(language.displayName, color = SanctuaryText, fontWeight = FontWeight.SemiBold)
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
private fun LoadingCard() {
    val l10n = sanctuaryStrings()
    Card(
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        modifier = Modifier.shadow(3.dp, RoundedCornerShape(24.dp), clip = false),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCard),
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
                color = SanctuaryTabActive,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(l10n.t("common.loading"), color = SanctuaryText)
        }
    }
}


@Composable
private fun ProfileMetric(label: String, value: Any) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = SanctuaryMuted)
        Text(value.toString(), color = SanctuaryText, fontWeight = FontWeight.SemiBold)
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
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        modifier = Modifier.sanctuaryCardShadow(),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCardElevated),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = SanctuaryText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        modifier = Modifier.sanctuaryCardShadow(RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCard),
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
                Text(title, color = SanctuaryText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                detail?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = SanctuaryTabActive, style = MaterialTheme.typography.labelLarge)
                }
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = SanctuaryMuted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.SouthEast,
                contentDescription = null,
                tint = SanctuaryMuted
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
                    .border(2.dp, SanctuaryCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun DetailSheetScaffold(
    title: String,
    subtitle: String? = null,
    showHeading: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showHeading) {
            Text(title, color = SanctuaryText, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 39.sp)
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = SanctuaryTabActive)
        }
        content()
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SanctuaryModalSheet(
    onDismissRequest: () -> Unit,
    heroContent: Boolean = false,
    scrollContent: Boolean = true,
    showBackButton: Boolean = true,
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
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .then(if (scrollContent) Modifier.verticalScroll(rememberScrollState())
                        .padding(top = if (heroContent) 16.dp else 64.dp, bottom = 32.dp) else Modifier),
                verticalArrangement = Arrangement.Top
            ) {
                content()
            }
            if (showBackButton) Surface(
        border = BorderStroke(1.dp, SanctuaryCardBorder),
                color = SanctuaryCardSoft,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 12.dp, start = 18.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SanctuaryText,
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
    onRequireAccount: (DeferredAccountAction) -> Unit,
    onDismiss: () -> Unit
) {
    val l10n = sanctuaryStrings()
    val context = LocalContext.current
    val isFavorite = progress.favorites.any { it.itemType == FavoriteItemType.Saint && it.itemId == detail.id }
    DetailSheetScaffold(
        title = detail.name,
        subtitle = detail.feastLabel,
        showHeading = false
    ) {
        if (!detail.imageUrl.isNullOrBlank()) ContentHeroImage(detail.imageUrl, detail.name)
        DetailSectionCard(title = detail.name, prominentTitle = true) {
            Text(detail.feastLabel, color = SanctuaryTabActive, fontSize = 14.sp)

        Button(
            border = BorderStroke(1.dp, SanctuaryCardBorder),
            onClick = {
                if (session.status == SessionStatus.Authenticated) {
                    onToggleFavorite(FavoriteItemType.Saint, detail.id, detail.name, detail.slug, null)
                } else {
                    onRequireAccount(
                        DeferredAccountAction.Favorite(
                            FavoriteItemType.Saint,
                            detail.id,
                            detail.name,
                            detail.slug,
                            null
                        )
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFavorite) SanctuaryBlue else SanctuaryCardSoft,
                contentColor = if (isFavorite) SanctuaryOnAccent else SanctuaryText
            ),
            shape = RoundedCornerShape(99.dp),
            modifier = Modifier.wrapContentWidth(Alignment.Start)
        ) {
            Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = null, tint = if (isFavorite) SanctuaryOnAccent else SanctuaryText)
            Spacer(Modifier.width(8.dp))
            Text(if (isFavorite) l10n.t("detail.favorite.saved") else l10n.t("detail.favorite.add"))
        }
        Button(
            border = BorderStroke(1.dp, SanctuaryCardBorder),
            onClick = {
                shareContent(
                    context = context,
                    title = detail.name,
                    message = "Look at this saint in Sanctuary: ${detail.name}",
                    url = sharedContentUrl(SharedContentKind.Saint, detail.slug)
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = SanctuaryCardSoft,
                contentColor = SanctuaryText
            ),
            shape = RoundedCornerShape(99.dp),
            modifier = Modifier.wrapContentWidth(Alignment.Start)
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null,
                tint = SanctuaryText
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share")
        }
        if (detail.patronages.isNotEmpty()) {
            DetailSectionCard(title = l10n.t("detail.patronages")) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    }
        detail.patronages.forEach { patronage ->
                        Text("• $patronage", color = SanctuaryMuted, lineHeight = 20.sp)
                    }
                }
            }
        }
        detail.summary?.takeIf { it.isNotBlank() }?.let { summary ->
            DetailSectionCard(title = l10n.t("detail.summary")) {
                Text(summary, color = SanctuaryMuted, lineHeight = 22.sp)
            }
        }
        detail.biography?.takeIf { it.isNotBlank() }?.let { biography ->
            DetailSectionCard(title = l10n.t("detail.biography")) {
                Text(biography, color = SanctuaryMuted, lineHeight = 22.sp)
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
    onRequireAccount: (DeferredAccountAction) -> Unit,
    resumeReminder: String?,
    onReminderResumed: () -> Unit,
    displayYear: Int,
    onDismiss: () -> Unit
) {
    val l10n = sanctuaryStrings()
    val context = LocalContext.current
    val window by produceState<app.sanctuary.android.data.NovenaWindowResponse?>(null, detail.id, displayYear) {
        value = runCatching { app.sanctuary.android.data.SanctuaryApiFactory.create { null }.novenaWindow(detail.id, displayYear) }.getOrNull()
    }
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
        subtitle = "${detail.durationDays}-day novena",
        showHeading = false
    ) {
        if (!detail.imageUrl.isNullOrBlank()) ContentHeroImage(detail.imageUrl, detail.title)
        DetailSectionCard(title = detail.title, prominentTitle = true) {
        progress.error?.let { Banner(it, true) }



        Button(
            border = BorderStroke(1.dp, SanctuaryCardBorder),
            onClick = {
                if (session.status == SessionStatus.Authenticated) {
                    onToggleFavorite(FavoriteItemType.Novena, detail.id, detail.title, detail.slug, detail.durationDays)
                } else {
                    onRequireAccount(
                        DeferredAccountAction.Favorite(
                            FavoriteItemType.Novena,
                            detail.id,
                            detail.title,
                            detail.slug,
                            detail.durationDays
                        )
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFavorite) SanctuaryBlue else SanctuaryCardSoft,
                contentColor = if (isFavorite) SanctuaryOnAccent else SanctuaryText
            ),
            shape = RoundedCornerShape(99.dp),
            modifier = Modifier.wrapContentWidth(Alignment.Start)
        ) {
            Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = null, tint = if (isFavorite) SanctuaryOnAccent else SanctuaryText)
            Spacer(Modifier.width(8.dp))
            Text(if (isFavorite) l10n.t("detail.favorite.saved") else l10n.t("detail.favorite.add"))
        }
        ContentReminderControl(destination = sharedContentUrl(SharedContentKind.Novena, detail.slug), title = detail.title,
                authenticated = session.status == SessionStatus.Authenticated,
                onRequireAccount = { onRequireAccount(DeferredAccountAction.Reminder(sharedContentUrl(SharedContentKind.Novena, detail.slug))) },
                resume = resumeReminder == sharedContentUrl(SharedContentKind.Novena, detail.slug), onResumed = onReminderResumed)
        Button(
            border = BorderStroke(1.dp, SanctuaryCardBorder),
            onClick = {
                shareContent(
                    context = context,
                    title = detail.title,
                    message = "Pray this novena with me in Sanctuary: ${detail.title}",
                    url = sharedContentUrl(SharedContentKind.Novena, detail.slug)
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = SanctuaryCardSoft,
                contentColor = SanctuaryText
            ),
            shape = RoundedCornerShape(99.dp),
            modifier = Modifier.wrapContentWidth(Alignment.Start)
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null,
                tint = SanctuaryText
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share")
        }

        DetailMetaChip(l10n.t("detail.duration") + ": " + detail.durationDays + " " + l10n.t("detail.days"))
        window?.let { dates ->
            runCatching { LocalDate.parse(dates.startDate) }.getOrNull()?.let { date -> DetailMetaChip(l10n.t("detail.startDate") + ": " + l10n.formatMonthDayYear(date)) }
            runCatching { LocalDate.parse(dates.feastDate) }.getOrNull()?.let { date -> DetailMetaChip(l10n.t("detail.endDate") + ": " + l10n.formatMonthDayYear(date)) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            detail.description.takeIf { it.isNotBlank() }?.let {
                Text(it, color = SanctuaryText, lineHeight = 24.sp)
            }
        }
        }
        if (detail.intentions.isNotEmpty()) {
            DetailSectionCard(title = l10n.t("detail.intentions")) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    detail.intentions.forEach { intention ->
                        Text("• $intention", color = SanctuaryMuted, lineHeight = 20.sp)
                    }
                }
            }
        }

        DetailSectionCard(title = l10n.t("calendar.chooseDay")) {
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
                    border = BorderStroke(1.dp, SanctuaryCardBorder),
                    onClick = { selectedDay = day.dayNumber },
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) SanctuaryBlue else SanctuaryCardSoft,
                        contentColor = if (active) SanctuaryOnAccent else SanctuaryText
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(l10n.t("calendar.dayLabel"), fontSize = 12.sp, color = if (active) SanctuaryOnAccent.copy(alpha = 0.85f) else SanctuaryMuted)
                        Text("${day.dayNumber}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }
        }

        }
        when {
            session.status != SessionStatus.Authenticated -> {
                PrimaryButton(
                    l10n.t("detail.startNovena"),
                    false,
                    onClick = { onRequireAccount(DeferredAccountAction.StartNovena(detail.id)) }
                )
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
                Text(l10n.t("detail.noDayContent"), color = SanctuaryMuted, lineHeight = 22.sp)
            } else if (!selectedDayDetail.hasVisibleDayContent()) {
                Text(l10n.t("detail.noDayContent"), color = SanctuaryMuted, lineHeight = 22.sp)
            } else {
                selectedDayDetail.title?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = SanctuaryText, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp)
                }
                selectedDayDetail.scripture?.takeIf { it.isNotBlank() }?.let {
                    DetailSectionLabel(l10n.t("detail.scripture"))
                    Text(it, color = SanctuaryMuted, lineHeight = 22.sp)
                }
                selectedDayDetail.prayer?.takeIf { it.isNotBlank() }?.let {
                    DetailSectionLabel(l10n.t("detail.prayer"))
                    Text(it, color = SanctuaryMuted, lineHeight = 22.sp)
                }
                selectedDayDetail.reflection?.takeIf { it.isNotBlank() }?.let {
                    DetailSectionLabel(l10n.t("detail.reflection"))
                    Text(it, color = SanctuaryMuted, lineHeight = 22.sp)
                }
                selectedDayDetail.body?.takeIf { selectedDayDetail.hasFallbackBodyContent() && it.isNotBlank() }?.let {
                    Text(it, color = SanctuaryMuted, lineHeight = 22.sp)
                }
            }
        }

        if (session.status == SessionStatus.Authenticated && (activeCommitment != null || latestCommitment?.status == CommitmentStatus.Completed)) {
            Button(
                border = BorderStroke(1.dp, SanctuaryCardBorder),
                onClick = { onCompleteDay(detail.id, detail.durationDays) },
                enabled = latestCommitment?.status != CommitmentStatus.Completed && detail.id !in progress.pendingNovenaCompletions,
                colors = ButtonDefaults.buttonColors(containerColor = SanctuaryBlue),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(completionLabel, color = SanctuaryOnAccent)
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
    onRequireAccount: (DeferredAccountAction) -> Unit,
    resumeReminder: String?,
    onReminderResumed: () -> Unit,
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
        subtitle = visiblePrayerCategory(detail.category),
        showHeading = false
    ) {
        if (!detail.imageUrl.isNullOrBlank()) Card(
        border = BorderStroke(1.dp, SanctuaryCardBorder),colors = CardDefaults.cardColors(containerColor = SanctuaryCard), shape = RoundedCornerShape(24.dp)) {
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
        border = BorderStroke(1.dp, SanctuaryCardBorder),
                    color = SanctuaryCardSoft,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SouthEast,
                        contentDescription = null,
                        tint = SanctuaryText,
                        modifier = Modifier
                            .size(34.dp)
                            .padding(8.dp)
                    )
                }
            }
        }
        }
        DetailSectionCard(title = detail.title, prominentTitle = true) {
            Button(
                border = BorderStroke(1.dp, SanctuaryCardBorder),
                onClick = {
                    if (session.status == SessionStatus.Authenticated) {
                        onToggleFavorite(FavoriteItemType.Prayer, detail.id, detail.title, detail.slug, null)
                    } else {
                        onRequireAccount(
                            DeferredAccountAction.Favorite(
                                FavoriteItemType.Prayer,
                                detail.id,
                                detail.title,
                                detail.slug,
                                null
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFavorite) SanctuaryBlue else SanctuaryCardSoft,
                    contentColor = if (isFavorite) SanctuaryOnAccent else SanctuaryText
                ),
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier.wrapContentWidth(Alignment.Start)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) SanctuaryOnAccent else SanctuaryText
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFavorite) l10n.t("detail.favorite.saved") else l10n.t("detail.favorite.add"))
            }
            ContentReminderControl(destination = sharedContentUrl(SharedContentKind.Prayer, detail.slug), title = detail.title,
                authenticated = session.status == SessionStatus.Authenticated,
                onRequireAccount = { onRequireAccount(DeferredAccountAction.Reminder(sharedContentUrl(SharedContentKind.Prayer, detail.slug))) },
                resume = resumeReminder == sharedContentUrl(SharedContentKind.Prayer, detail.slug), onResumed = onReminderResumed)
            Button(
                border = BorderStroke(1.dp, SanctuaryCardBorder),
                onClick = {
                    shareContent(
                        context = context,
                        title = detail.title,
                        message = "Pray this with me in Sanctuary: ${detail.title}",
                        url = sharedContentUrl(SharedContentKind.Prayer, detail.slug)
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SanctuaryCardSoft,
                    contentColor = SanctuaryText
                ),
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier.wrapContentWidth(Alignment.Start)
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    tint = SanctuaryText
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share")
            }
            detail.note?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = SanctuaryMuted, lineHeight = 22.sp)
            }
        }
        DetailSectionCard(title = l10n.t("detail.prayer")) {
            Text(bodyText, color = SanctuaryMuted, lineHeight = 24.sp)
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
                .background(SanctuaryHeroBase)
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
        border = BorderStroke(1.dp, SanctuaryCardBorder),
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
    prominentTitle: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        border = BorderStroke(1.dp, SanctuaryCardBorder),
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
            Text(title, color = SanctuaryText, fontWeight = FontWeight.Bold, fontSize = if (prominentTitle) 40.sp else 22.sp, lineHeight = if (prominentTitle) 44.sp else 28.sp)
            if (!prominentTitle) Box(Modifier.fillMaxWidth().height(1.dp).background(SanctuaryCardBorder))
            content()
        }
    }
}

@Composable
private fun DetailSectionLabel(text: String) {
    Text(
        text = text,
        color = SanctuaryTabActive,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    )
}

private fun AppTab.icon(): ImageVector = when (this) {
    AppTab.Home -> Icons.Filled.Home
    AppTab.Novenas -> Icons.Filled.MenuBook
    AppTab.Liturgical -> Icons.Filled.CalendarMonth
    AppTab.Saints -> Icons.Filled.People
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
            color = SanctuaryTabActive,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(message, color = SanctuaryMuted)
    }
}

@Composable
private fun SectionHint(title: String, body: String) {
    Card(
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        modifier = Modifier.sanctuaryCardShadow(RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCard),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = SanctuaryText, fontWeight = FontWeight.SemiBold)
            Text(body, color = SanctuaryMuted, lineHeight = 22.sp)
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
                        listOf(SanctuaryCardSoft, SanctuaryCardSoft)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = null,
                tint = SanctuaryMuted,
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
            state.selectedTerm == null && state.terms.isEmpty() -> Text(title, color = SanctuaryMuted)
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
            mode == TermSearchMode.Intentions && state.novenas.isEmpty() -> Text(title, color = SanctuaryMuted)
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
            state.saints.isEmpty() -> Text(title, color = SanctuaryMuted)
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
    showSearch: Boolean = true,
    itemContent: @Composable (T) -> Unit
) {
    val l10n = sanctuaryStrings()
    LaunchedEffect(query) {
        delay(250)
        onSubmit()
    }

    DetailSheetScaffold(title = title) {
        if (showSearch) {
        SearchCard(
            title = title,
            query = query,
            onQueryChanged = onQueryChanged,
            onSubmit = onSubmit
        )
        Text(
            text = "${items.size} ${l10n.t("search.results")}",
            color = SanctuaryText.copy(alpha = 0.92f),
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp
        )
        }
        when {
            isLoading -> InlineLoading(l10n.t("inline.loading"))
            error != null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Banner(error, isError = true)
                TextButton(onClick = onSubmit) { Text(l10n.t("inbox.refresh")) }
            }
            items.isEmpty() -> Text(emptyLabel, color = SanctuaryMuted)
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
        selectedDate = LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())),
        onDateSelected = { date -> selectedYear = date.year; selectedMonth = date.monthValue; selectedDay = date.dayOfMonth },
        footer = {
            CalendarSearchButton(label = l10n.t("calendar.searchSaints"), onClick = onSearch)
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
    onYearChanged: (Int) -> Unit,
    fetchNovenasInRange: suspend (String, String) -> List<app.sanctuary.android.data.NovenaCalendarDate>,
    fetchLiturgicalRange: suspend (String, String) -> List<app.sanctuary.android.data.LiturgicalDay>
) {
    val l10n = sanctuaryStrings()
    val today = LocalDate.now()
    var selectedDay by rememberSaveable { mutableStateOf(today.dayOfMonth) }
    var selectedMonth by rememberSaveable { mutableStateOf(today.monthValue) }
    var selectedYear by rememberSaveable { mutableStateOf(today.year) }
    LaunchedEffect(selectedYear) { onYearChanged(selectedYear) }
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
        selectedDate = LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())),
        onDateSelected = { date -> selectedYear = date.year; selectedMonth = date.monthValue; selectedDay = date.dayOfMonth },
        footer = {
            CalendarSearchButton(label = l10n.t("calendar.searchNovenas"), onClick = onSearch)
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
        selectedDate = LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, month.lengthOfMonth())),
        onDateSelected = { date -> selectedYear = date.year; selectedMonth = date.monthValue; selectedDay = date.dayOfMonth },
        footer = {
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
    Column(Modifier.fillMaxSize().padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, l10n.t("common.close"), tint = SanctuaryText) }
            Text(l10n.t("calendar.dailyReadingsTitle"), color = SanctuaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        DailyReadingsWebView(url = url, modifier = Modifier.weight(1f))
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DailyReadingsWebView(url: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCard),
        shape = RoundedCornerShape(24.dp)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#D8E3D9"))
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, finishedUrl: String) {
                            super.onPageFinished(view, finishedUrl)
                            val script = context.assets.open("readings-theme.js").bufferedReader().use { it.readText() }
                            view.evaluateJavascript(script, null)
                        }

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
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val l10n = sanctuaryStrings()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CalendarNavButton("‹", onPrev)
            Column(Modifier.weight(1f).clickable {
                android.app.DatePickerDialog(context, { _, year, monthIndex, day ->
                    onDateSelected(LocalDate.of(year, monthIndex + 1, day))
                }, selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth).show()
            }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(subtitle.substringBefore("•").trim(), color = SanctuaryMuted, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = SanctuaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Icon(Icons.Filled.ExpandMore, null, tint = SanctuaryText, modifier = Modifier.size(18.dp))
                }
            }
            CalendarNavButton("›", onNext)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onToday) { Text(l10n.t("common.today"), color = SanctuaryTabActive) }
            CalendarMode.entries.forEach { entry ->
                FilterChipButton(entry.label(l10n), mode == entry) { onModeChange(entry) }
            }
        }
        SeasonLegend()
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), content = footer)
        Column(Modifier.fillMaxWidth().calendarDaySwipe(enabled = mode == CalendarMode.Day,
            onPrevious = onPrev, onNext = onNext), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
private fun CalendarNavButton(
    symbol: String,
    onClick: () -> Unit
) {
    Card(
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCardSoft),
        shape = RoundedCornerShape(999.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (symbol == "‹") Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight, contentDescription = if (symbol == "‹") "Previous date" else "Next date", tint = SanctuaryText, modifier = Modifier.size(26.dp))
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
        border = BorderStroke(1.dp, SanctuaryCardBorder),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) SanctuaryBlue else SanctuaryCardSoft
        ),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            color = if (selected) SanctuaryOnAccent else SanctuaryText,
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
    Card(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SanctuaryCard), shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, borderColor)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 144.dp).padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showImage && !imageUrl.isNullOrBlank()) {
                ThumbnailImage(imageUrl, title, Modifier.size(width = 84.dp, height = 112.dp), RoundedCornerShape(12.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(date.dayOfMonth.toString(), color = SanctuaryText, fontSize = 34.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontWeight = FontWeight.SemiBold)
                    Box(Modifier.size(7.dp).background(borderColor, CircleShape))
                }
                Text(title, color = SanctuaryText, fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold)
                if (buttonLabel != null) Text(buttonLabel, color = SanctuaryTabActive, fontSize = 13.sp)
                else if (subtitle.isNotBlank()) Text(subtitle, color = SanctuaryMuted, fontSize = 13.sp)
            }
            if (enabled) Icon(Icons.Filled.ChevronRight, null, tint = SanctuaryMuted)
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
    Box(modifier = Modifier.fillMaxWidth()) {
        val rowGap = 6.dp
        val cellHeight = 60.dp

        Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
            CalendarWeekHeaderRow()
            var dayNumber = 1
            repeat(rowCount) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
    Surface(onClick = onClick, modifier = modifier.height(height).border(0.5.dp, SanctuaryCardBorder, RoundedCornerShape(12.dp)).semantics { contentDescription = "$day, $label" },
        color = if (selected) SanctuaryBlue else SanctuaryCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isToday) 3.dp else 2.dp, if (isToday) SanctuaryToday else borderColor)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day.toString(), color = if (selected) SanctuaryOnAccent else SanctuaryText,
                fontSize = 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
            Box(Modifier.size(5.dp).background(borderColor, CircleShape))
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
                color = SanctuaryMuted,
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
    "advent" -> SanctuaryAdvent
    "christmas" -> SanctuaryChristmas
    "lent" -> SanctuaryLent
    "easter" -> SanctuaryEaster
    else -> SanctuaryOrdinary
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SeasonLegend() {
    val l10n = sanctuaryStrings()
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth().background(SanctuaryCard, RoundedCornerShape(16.dp))
            .border(1.dp, SanctuaryCardBorder, RoundedCornerShape(16.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SeasonDot(l10n.t("season.advent"), SanctuaryAdvent, 12.sp, 9.dp, 8.dp)
        SeasonDot(l10n.t("season.christmas"), SanctuaryChristmas, 12.sp, 9.dp, 8.dp)
        SeasonDot(l10n.t("season.lent"), SanctuaryLent, 12.sp, 9.dp, 8.dp)
        SeasonDot(l10n.t("season.easter"), SanctuaryEaster, 12.sp, 9.dp, 8.dp)
        SeasonDot(l10n.t("season.ordinary"), SanctuaryOrdinary, 12.sp, 9.dp, 8.dp)
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
        modifier = Modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing / 2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(dotSize).background(color, CircleShape).border(0.5.dp, SanctuaryMuted, CircleShape))
        Text(
            text = label,
            color = SanctuaryMuted,
            fontSize = fontSize,
            maxLines = 2,
            softWrap = true
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnouncementInvitation(onSync: () -> Unit, onVisibilityChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val strings = sanctuaryStrings()
    val restored = remember { AndroidPushNotifications.consent(context) }
    var visible by remember { mutableStateOf(restored.needsInvitation || restored.needsPermissionRecovery(AndroidPushNotifications.allowed(context))) }
    LaunchedEffect(visible) { onVisibilityChanged(visible) }
    var needsSettings by remember { mutableStateOf(false) }
    val problem by AndroidPushNotifications.registrationProblem.collectAsState()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { if (AndroidPushNotifications.setEnabled(context, true)) { visible = false; onSync() } }
        else needsSettings = true
    }
    LaunchedEffect(Unit) {
        if (restored.needsPermissionRecovery(AndroidPushNotifications.allowed(context))) {
            if (!AndroidPushNotifications.runtimePermissionGranted(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else needsSettings=true
        }
    }
    androidx.lifecycle.compose.LifecycleResumeEffect(needsSettings,visible) {
        if (visible && needsSettings && AndroidPushNotifications.allowed(context) && AndroidPushNotifications.setEnabled(context,true)) { visible=false; onSync() }
        onPauseOrDispose { }
    }
    if (visible) ModalBottomSheet(
        onDismissRequest = {},
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden }),
        containerColor = SanctuaryCard, contentColor = SanctuaryText
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Icon(Icons.Filled.NotificationsNone, contentDescription = null, tint = SanctuaryTabActive, modifier = Modifier.size(44.dp))
            Text(strings.t("alerts.invitation.title"), fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(strings.t("alerts.invitation.body"), fontSize = 17.sp, textAlign = TextAlign.Center, color = SanctuaryTabInactive)
            Button(modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), onClick = {
                if (needsSettings) context.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,context.packageName))
                else if (!AndroidPushNotifications.runtimePermissionGranted(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else if (!AndroidPushNotifications.allowed(context)) needsSettings=true
                else if (AndroidPushNotifications.setEnabled(context, true)) { visible = false; onSync() }
            }) { Text(strings.t(if(needsSettings) "notifications.settings" else "alerts.invitation.enable")) }
            OutlinedButton(modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), onClick = {
                if (AndroidPushNotifications.setEnabled(context, false)) { visible = false; onSync() }
            }) { Text(strings.t("alerts.invitation.later")) }
            problem?.let { Text(it, fontSize = 14.sp) }
        }
    }
}

@Composable
private fun AnnouncementPreference() {
    val l10n = sanctuaryStrings()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(AndroidPushNotifications.enabled(context)) }
    val registrationProblem by AndroidPushNotifications.registrationProblem.collectAsState()
    fun sync() {
        scope.launch {
            runCatching { AndroidPushNotifications.sync(context, app.sanctuary.android.data.SessionRepository(context, app.sanctuary.android.data.SanctuaryApiFactory.create { null })) }

        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // System denial does not overwrite the separate app preference.
        enabled = AndroidPushNotifications.enabled(context)
        sync()
    }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) { sync(); kotlinx.coroutines.awaitCancellation() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ReminderToggleRow(title = l10n.t("announcements.title"), subtitle = l10n.t("announcements.body"), checked = enabled, enabled = true, onCheckedChange = { value ->
            if (value && !AndroidPushNotifications.runtimePermissionGranted(context)) {
                if (AndroidPushNotifications.setEnabled(context,true)) { enabled=true; permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
            }
            else {
                enabled = value
                AndroidPushNotifications.setEnabled(context,value)
                sync()
            }
        })
        registrationProblem?.let { Text(it, color = SanctuaryTabInactive, fontSize = 14.sp) }
        NotificationActionCard(
            title = l10n.t("notifications.settings"), subtitle = l10n.t("notifications.settingsBody"),
            icon = Icons.Filled.OpenInNew, trailingIcon = Icons.Filled.OpenInNew
        ) {
            val intent = if (Build.VERSION.SDK_INT >= 26) Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            else Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
            context.startActivity(intent)
        }
        PreciseReminderSettings()
    }
}

@Composable
private fun PreciseReminderSettings() {
    val context = LocalContext.current
    val l10n = sanctuaryStrings()
    val scheduler = remember(context) { AndroidReminderScheduler(context) }
    var allowed by remember { mutableStateOf(scheduler.preciseTimingAllowed()) }
    androidx.lifecycle.compose.LifecycleResumeEffect(scheduler) {
        allowed = scheduler.preciseTimingAllowed()
        onPauseOrDispose { }
    }
    NotificationActionCard(
        title = l10n.t("itemReminder.preciseSettings"),
        subtitle = l10n.t(if (allowed) "itemReminder.preciseOn" else "itemReminder.preciseOff"),
        icon = Icons.Filled.NotificationsActive,
        trailingIcon = Icons.Filled.OpenInNew
    ) { context.startActivity(reminderSettingsIntent(context, scheduler, ReminderSetupIssue.PreciseTiming)) }
}


@Composable
private fun NotificationActionCard(title: String, subtitle: String, icon: ImageVector, trailingIcon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(SanctuaryCardSoft).border(1.dp, SanctuaryCardBorder, RoundedCornerShape(18.dp))
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = SanctuaryTabActive, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = SanctuaryText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = SanctuaryMuted, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Icon(trailingIcon, contentDescription = null, tint = SanctuaryText, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ContentReminderControl(destination: String, title: String, authenticated: Boolean, onRequireAccount: () -> Unit, resume: Boolean, onResumed: () -> Unit) {
    val context = LocalContext.current
    val l10n = sanctuaryStrings()
    val scope = rememberCoroutineScope()
    val busy by AccountReminderSync.busy.collectAsState()

    val scheduler = remember(context) { AndroidReminderScheduler(context) }
    val currentAuthenticated by androidx.compose.runtime.rememberUpdatedState(authenticated)
    var time by remember(destination) { mutableStateOf(scheduler.itemTime(destination)) }
    var problem by remember { mutableStateOf<String?>(null) }
    var showOptions by remember { mutableStateOf(false) }
    DisposableEffect(scheduler, destination) {
        val unsubscribe = scheduler.observeItems { time = scheduler.itemTime(destination) }
        onDispose { unsubscribe() }
    }
    var explainTiming by remember { mutableStateOf(false) }
    fun chooseTime() {
        if (!currentAuthenticated) { onRequireAccount(); return }
        if (!scheduler.notificationsAllowed()) { problem = "itemReminder.permission"; return }
        if (!scheduler.preciseTimingAllowed()) { explainTiming = true; return }
        val current = time ?: (8 to 0)
        android.app.TimePickerDialog(context, { _, hour, minute ->
            if (!currentAuthenticated) { onRequireAccount(); return@TimePickerDialog }
            scope.launch {
                runCatching { AccountReminderSync.save(destination, title, hour, minute) }
                    .onSuccess { problem = null }
                    .onFailure { problem = it.message ?: l10n.t("itemReminder.error") }
            }
        }, current.first, current.second, android.text.format.DateFormat.is24HourFormat(context)).apply {
            setTitle(l10n.t("itemReminder.title"))
            setButton(android.content.DialogInterface.BUTTON_POSITIVE, l10n.t("itemReminder.save"), this)
            setButton(android.content.DialogInterface.BUTTON_NEGATIVE, l10n.t("common.cancel"), this)
        }.show()
    }
    val timingSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        scheduler.resyncItems()
        scope.launch { AccountReminderSync.refresh() }
        if (scheduler.preciseTimingAllowed()) chooseTime() else problem = "itemReminder.exactRequired"
    }
    if (explainTiming) AlertDialog(onDismissRequest = { explainTiming = false },
        title = { Text(l10n.t("itemReminder.title")) },
        text = { Text(l10n.t("itemReminder.exactRequired")) },
        confirmButton = { TextButton(onClick = {
            explainTiming = false
            if (Build.VERSION.SDK_INT >= 31) timingSettings.launch(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }) { Text(l10n.t("reminders.manage.settings")) } },
        dismissButton = { TextButton(onClick = { explainTiming = false }) { Text(l10n.t("common.cancel")) } })
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!currentAuthenticated) onRequireAccount()
        else if (granted) chooseTime() else problem = "itemReminder.permission"
    }
    LaunchedEffect(authenticated, resume) {
        if (authenticated && resume) {
            onResumed()
            if (!scheduler.notificationsAllowed() && Build.VERSION.SDK_INT >= 33 && !AndroidPushNotifications.runtimePermissionGranted(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else chooseTime()
        }
    }
    OutlinedButton(onClick = {
        if (!currentAuthenticated) onRequireAccount()
        else if (time != null) showOptions = true
        else if (!scheduler.notificationsAllowed()) {
            if (Build.VERSION.SDK_INT >= 33 && !AndroidPushNotifications.runtimePermissionGranted(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else problem = "itemReminder.permission"
        } else chooseTime()
    }, enabled = !busy, modifier = Modifier.wrapContentWidth(Alignment.Start), shape = RoundedCornerShape(99.dp)) {
        Icon(if (time == null) Icons.Filled.NotificationsNone else Icons.Filled.NotificationsActive, contentDescription = null, tint = SanctuaryText)
        Spacer(Modifier.width(8.dp))
        Text(l10n.t(if (time == null) "itemReminder.add" else "itemReminder.edit"), color = SanctuaryText)
    }
    if (showOptions) AlertDialog(
        onDismissRequest = { showOptions = false },
        title = { Text(l10n.t("itemReminder.title")) },
        text = { Text(title + "\n" + l10n.t("itemReminder.body") + "\n" + time?.let { "%02d:%02d".format(it.first, it.second) }.orEmpty()) },
        confirmButton = { Button(
        border = BorderStroke(1.dp, SanctuaryCardBorder),onClick = { showOptions = false; chooseTime() }, colors = ButtonDefaults.buttonColors(containerColor = SanctuaryBlue)) { Text(l10n.t("reminders.manage.change"), color = SanctuaryOnAccent) } },
        dismissButton = { OutlinedButton(onClick = { scope.launch {
                runCatching { AccountReminderSync.remove(destination) }.onFailure { problem = it.message }
                showOptions = false
            } }) {
            Icon(Icons.Filled.Delete, contentDescription = null, tint = SanctuaryError)
            Spacer(Modifier.width(6.dp))
            Text(l10n.t("itemReminder.remove"), color = SanctuaryError)
        } }
    )
    if (problem != null) AlertDialog(onDismissRequest = { problem = null },
        title = { Text(l10n.t("itemReminder.title")) }, text = { Text(l10n.t(problem.orEmpty())) },
        confirmButton = { TextButton(onClick = {
            if (problem == "itemReminder.permission") context.startActivity(scheduler.notificationSettingsIntent())
            problem = null
        }) { Text(if (problem == "itemReminder.permission") l10n.t("reminders.manage.settings") else "OK") } })
}

private fun reminderSettingsIntent(context: Context, scheduler: AndroidReminderScheduler, issue: ReminderSetupIssue): Intent = when (issue) {
    ReminderSetupIssue.Notifications -> scheduler.notificationSettingsIntent()
    ReminderSetupIssue.PreciseTiming -> if (Build.VERSION.SDK_INT >= 31)
        Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        else Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    ReminderSetupIssue.BackgroundRestricted -> Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
}

@Composable
private fun ReminderTimingSetupCard(issue: ReminderSetupIssue) {
    val context = LocalContext.current
    val l10n = sanctuaryStrings()
    val scheduler = remember(context) { AndroidReminderScheduler(context) }
    Column(Modifier.fillMaxWidth().background(SanctuaryCardSoft, RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(l10n.t(issue.messageKey), color = SanctuaryWarning)
        Button(onClick = { context.startActivity(reminderSettingsIntent(context, scheduler, issue)) },
            modifier = Modifier.fillMaxWidth()) { Text(l10n.t("reminders.manage.settings")) }
    }
}

@Composable
private fun ReminderTimingRecovery(owner: String?) {
    val context = LocalContext.current
    val scheduler = remember(context) { AndroidReminderScheduler(context) }
    val onboarding = remember(context) { ReminderPermissionOnboarding(context) }
    val l10n = sanctuaryStrings()
    var issue by remember(owner) { mutableStateOf(scheduler.setupIssue()) }
    var introduction by remember(owner) { mutableStateOf(onboarding.shouldOffer(scheduler.preciseTimingAllowed())) }
    var dismissed by remember(owner) { mutableStateOf(false) }
    DisposableEffect(scheduler, owner) {
        val unsubscribe = scheduler.observeItems { issue = scheduler.setupIssue() }
        onDispose { unsubscribe() }
    }
    androidx.lifecycle.compose.LifecycleResumeEffect(owner) {
        scheduler.resyncItems()
        issue = scheduler.setupIssue()
        introduction = onboarding.shouldOffer(scheduler.preciseTimingAllowed())
        // Returning from Settings must re-evaluate, including a declined request.
        dismissed = false
        onPauseOrDispose { }
    }
    val currentIssue = issue ?: if (introduction) ReminderSetupIssue.PreciseTiming else null
    LaunchedEffect(currentIssue) { dismissed = false }
    fun dismissSetup() {
        if (introduction && currentIssue == ReminderSetupIssue.PreciseTiming) {
            onboarding.acknowledge()
            introduction = false
        }
        dismissed = true
    }
    if (currentIssue != null && !dismissed) {
        AlertDialog(onDismissRequest = ::dismissSetup,
            title = { Text(l10n.t(if (currentIssue == ReminderSetupIssue.PreciseTiming) "itemReminder.precise" else "reminders.manage.setup")) },
            text = { Text(l10n.t(if (currentIssue == ReminderSetupIssue.PreciseTiming && !scheduler.hasOwnedReminders())
                "itemReminder.exactIntroduction" else currentIssue.messageKey)) },
            confirmButton = { Button(onClick = {
                dismissSetup()
                context.startActivity(reminderSettingsIntent(context, scheduler, currentIssue))
            }) { Text(l10n.t("reminders.manage.settings")) } },
            dismissButton = { TextButton(onClick = ::dismissSetup) { Text(l10n.t("common.close")) } })
    }
}

@Composable
private fun ScheduledRemindersCard(onOpenNovena: (String) -> Unit, onOpenPrayer: (String) -> Unit) {
    val context = LocalContext.current
    val l10n = sanctuaryStrings()
    val scope = rememberCoroutineScope()
    val issue by AccountReminderSync.problem.collectAsState()
    val busy by AccountReminderSync.busy.collectAsState()
    var problem by remember { mutableStateOf<String?>(null) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while (true) {
                AccountReminderSync.refresh()
                delay(30_000)
            }
        }
    }
    val scheduler = remember(context) { AndroidReminderScheduler(context) }
    var reminders by remember { mutableStateOf(scheduler.itemReminders()) }
    var setupIssue by remember { mutableStateOf(scheduler.setupIssue()) }
    var legacyNotice by remember { mutableStateOf(scheduler.legacyNotice()) }
    DisposableEffect(scheduler) {
        val unsubscribe = scheduler.observeItems {
            reminders = scheduler.itemReminders()
            setupIssue = scheduler.setupIssue()
        }
        onDispose { unsubscribe() }
    }
    androidx.lifecycle.compose.LifecycleResumeEffect(scheduler) {
        setupIssue = scheduler.setupIssue()
        onPauseOrDispose { }
    }
    var images by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    // Keep network work outside BoxWithConstraints' measurement subcomposition.
    // Resizing or scrolling a card must not cancel an image fetch and retain a blank state.
    LaunchedEffect(reminders.map { it.destination }) {
        val api = app.sanctuary.android.data.SanctuaryApiFactory.create { null }
        val fetched = reminders.filter { it.destination !in images }.map { reminder ->
            async {
                val content = AccountReminderSync.contentKey(reminder.destination)
                val image = try {
                    if (content.first == "novenas") api.getNovenaDetail(content.second).imageUrl
                    else api.getPrayerDetail(content.second).imageUrl
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) { null }
                reminder.destination to image
            }
        }.awaitAll().toMap()
        // Publish once: concurrent completions must not overwrite each other's entries.
        images = images + fetched
    }

    var confirmingClear by remember { mutableStateOf(false) }
    MeSectionCard(title = l10n.t("reminders.manage.title")) {
        if (legacyNotice) {
            Text(l10n.t("reminders.manage.legacy"), color = SanctuaryMuted)
            OutlinedButton(onClick = { scheduler.dismissLegacyNotice(); legacyNotice = false }) { Text(l10n.t("common.close"), color = SanctuaryText) }
        }
        val timingRequired = setupIssue != null
        setupIssue?.let { ReminderTimingSetupCard(it) }
        (problem ?: issue)?.takeUnless { it == setupIssue?.messageKey }?.let { Text(l10n.t(it), color = SanctuaryWarning) }
        if (reminders.isEmpty()) Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = SanctuaryMuted, modifier = Modifier.size(20.dp))
            Text(l10n.t("reminders.manage.empty"), color = SanctuaryMuted, fontSize = 14.sp, lineHeight = 20.sp)
        }
        reminders.forEach { reminder ->
            key(reminder.destination) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val compactActions = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f
                    val actions: @Composable () -> Unit = {
                        IconButton(modifier = Modifier.size(48.dp).testTag("edit:${reminder.destination}").background(SanctuaryCardSoft, RoundedCornerShape(22.dp)).border(1.dp, SanctuaryCardBorder, RoundedCornerShape(22.dp)), enabled = !busy, onClick = {
                            if (!scheduler.preciseTimingAllowed()) { problem = "itemReminder.exactRequired"; return@IconButton }
                            android.app.TimePickerDialog(context, { _, hour, minute -> scope.launch {
                                runCatching { AccountReminderSync.save(reminder.destination, reminder.title, hour, minute) }
                                    .onSuccess { problem = null }.onFailure { problem = it.message }
                            } }, reminder.hour, reminder.minute, android.text.format.DateFormat.is24HourFormat(context)).apply {
                                setTitle(l10n.t("itemReminder.edit"))
                            }.show()
                        }) { Icon(Icons.Filled.Edit, contentDescription = l10n.t("itemReminder.edit"), tint = SanctuaryTabActive) }
                        IconButton(modifier = Modifier.size(48.dp).testTag("delete:${reminder.destination}"), enabled = !busy, onClick = { scope.launch {
                            runCatching { AccountReminderSync.remove(reminder.destination) }.onSuccess { problem = null }.onFailure { problem = it.message }
                        } }) {
                            Icon(Icons.Filled.Delete, contentDescription = l10n.t("itemReminder.remove") + ": " + reminder.title, tint = SanctuaryError)
                        }
                    }
                    Column(modifier = Modifier.fillMaxWidth().testTag("reminder:${reminder.destination}").clip(RoundedCornerShape(16.dp)).background(SanctuaryCardSoft).border(1.dp, SanctuaryCardBorder, RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val image = images[reminder.destination]
                            val key = AccountReminderSync.contentKey(reminder.destination)
                            val open = { if (key.first == "novenas") onOpenNovena(key.second) else onOpenPrayer(key.second) }
                            Box(Modifier.size(56.dp, 64.dp).testTag("thumbnail:${reminder.destination}").clip(RoundedCornerShape(12.dp)).background(SanctuaryCardSoft).border(1.dp, SanctuaryCardBorder, RoundedCornerShape(12.dp)).clickable(onClick = open), contentAlignment = Alignment.Center) {
                                if (image != null) AsyncImage(model = resolveImageUrl(image), contentDescription = reminder.title,
                                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().testTag("image:${reminder.destination}"))
                                else Icon(Icons.Filled.MenuBook, contentDescription = reminder.title, tint = SanctuaryTabActive, modifier = Modifier.size(24.dp))
                            }
                            Column(Modifier.weight(1f).testTag("text:${reminder.destination}").clickable(onClick = open), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(reminder.title, color = SanctuaryText, fontWeight = FontWeight.SemiBold)
                                val calendar = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, reminder.hour); set(java.util.Calendar.MINUTE, reminder.minute) }
                                Text((if (timingRequired) l10n.t("reminders.manage.waiting") + " · " else "") + l10n.t("reminders.manage.daily") + " · " + android.text.format.DateFormat.getTimeFormat(context).format(calendar.time), color = SanctuaryMuted, fontSize = 13.sp)
                            }
                            if (!compactActions) Row { actions() }
                        }
                        if (compactActions) Row(Modifier.align(Alignment.End)) { actions() }
                    }
                }
            }
        }
        if (reminders.isNotEmpty()) OutlinedButton(onClick = { confirmingClear = true }, modifier = Modifier.fillMaxWidth()) {
            Text(l10n.t("reminders.manage.clear"), color = SanctuaryError)
        }
    }
    if (confirmingClear) AlertDialog(onDismissRequest = { confirmingClear = false },
        title = { Text(l10n.t("reminders.manage.confirm")) }, text = { Text(l10n.t("reminders.manage.body")) },
        confirmButton = { Button(
        border = BorderStroke(1.dp, SanctuaryCardBorder),onClick = { scope.launch {
            runCatching { AccountReminderSync.clear() }.onSuccess { problem = null }.onFailure { problem = it.message }
            confirmingClear = false
        } }, colors = ButtonDefaults.buttonColors(containerColor = SanctuaryError)) { Text(l10n.t("reminders.manage.clear"), color = SanctuaryOnAccent) } },
        dismissButton = { TextButton(onClick = { confirmingClear = false }) { Text(l10n.t("common.cancel")) } })
}


@Composable
private fun ContentHeroImage(url: String?, title: String) {
    Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, SanctuaryCardBorder), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SanctuaryCardSoft)) {
        Box(Modifier.fillMaxWidth().padding(8.dp)) {
            ThumbnailImage(url, title, Modifier.fillMaxWidth().aspectRatio(1.4f), RoundedCornerShape(18.dp))
        }
    }
}


@Composable
private fun DetailMetaChip(text: String) {
    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(SanctuaryCardSoft).border(1.dp, SanctuaryCardBorder, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = SanctuaryMuted, modifier = Modifier.size(16.dp))
        Text(text, color = SanctuaryMuted, fontSize = 14.sp)
    }
}
