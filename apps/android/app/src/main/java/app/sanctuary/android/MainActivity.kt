package app.sanctuary.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sanctuary.android.data.NovenaSummary
import app.sanctuary.android.data.SaintSummary
import app.sanctuary.android.ui.theme.SanctuaryTheme

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
    Saints("Saints"),
    Me("Me")
}

@Composable
private fun SanctuaryApp(viewModel: MainViewModel) {
    val session by viewModel.session.collectAsState()
    val saints by viewModel.saints.collectAsState()
    val novenas by viewModel.novenas.collectAsState()
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
        when (session.status) {
            SessionStatus.Authenticated -> AuthenticatedShell(
                session = session,
                saints = saints,
                novenas = novenas,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onLogout = viewModel::logout,
                onSaintQueryChanged = viewModel::updateSaintQuery,
                onNovenaQueryChanged = viewModel::updateNovenaQuery,
                onReloadSaints = viewModel::loadSaints,
                onReloadNovenas = viewModel::loadNovenas
            )

            SessionStatus.Loading -> LoadingScreen()
            else -> AccountAccessScreen(session = session, onAction = viewModel)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF7AC8EA))
            Spacer(modifier = Modifier.height(18.dp))
            Text("Preparing Sanctuary…", color = Color.White)
        }
    }
}

@Composable
private fun AccountAccessScreen(
    session: SessionUiState,
    onAction: MainViewModel
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
    var confirmationCode by rememberSaveable { mutableStateOf("") }
    var forgotEmail by rememberSaveable { mutableStateOf(session.pendingPasswordResetEmail.orEmpty()) }
    var resetEmail by rememberSaveable { mutableStateOf(session.pendingPasswordResetEmail.orEmpty()) }
    var resetCode by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Sanctuary account",
                color = Color(0xFF7AC8EA),
                style = MaterialTheme.typography.labelLarge
            )
        }

        item {
            Text(
                text = when (step) {
                    AuthStep.Landing -> "Sanctuary for Android"
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
        }

        item {
            Text(
                text = "This first Android build is focused on auth parity and live Sanctuary content, so we can distribute something real to the dev track today.",
                color = Color(0xFFD0DFEA),
                lineHeight = 22.sp
            )
        }

        if (!session.message.isNullOrBlank()) {
            item {
                Banner(message = session.message, isError = session.isErrorMessage)
            }
        }

        item {
            when (step) {
                AuthStep.Landing -> ChoiceStack(
                    isBusy = isBusy,
                    onLogin = { step = AuthStep.Login },
                    onRegister = { step = AuthStep.Register }
                )

                AuthStep.Login -> AuthCard {
                    TextButton(onClick = { step = AuthStep.Landing }, enabled = !isBusy) {
                        Text("Back")
                    }
                    TextFieldBlock("Email", loginEmail) { loginEmail = it }
                    TextFieldBlock("Password", loginPassword, secure = true) { loginPassword = it }
                    PrimaryButton("Login", isBusy) {
                        onAction.login(loginEmail.trim(), loginPassword)
                    }
                    TextButton(onClick = { step = AuthStep.ForgotPassword }, enabled = !isBusy) {
                        Text("Forgot password?")
                    }
                }

                AuthStep.Register -> AuthCard {
                    TextButton(onClick = { step = AuthStep.Landing }, enabled = !isBusy) {
                        Text("Back")
                    }
                    TextFieldBlock("First name", firstName) { firstName = it }
                    TextFieldBlock("Last name", lastName) { lastName = it }
                    TextFieldBlock("Email", registerEmail) { registerEmail = it }
                    TextFieldBlock("Password", registerPassword, secure = true) { registerPassword = it }
                    PrimaryButton("Create account", isBusy) {
                        onAction.register(
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            email = registerEmail.trim(),
                            password = registerPassword
                        )
                    }
                }

                AuthStep.Confirm -> AuthCard {
                    TextButton(onClick = { step = AuthStep.Login }, enabled = !isBusy) {
                        Text("Back")
                    }
                    Text(
                        text = "We sent a confirmation code to ${session.pendingConfirmationEmail ?: registerEmail.trim()}.",
                        color = Color(0xFFD0DFEA)
                    )
                    TextFieldBlock("Verification code", confirmationCode) { confirmationCode = it }
                    PrimaryButton("Confirm account", isBusy) {
                        onAction.confirmRegistration(confirmationCode.trim())
                    }
                    TextButton(onClick = onAction::resendConfirmation, enabled = !isBusy) {
                        Text("Send a new code")
                    }
                }

                AuthStep.ForgotPassword -> AuthCard {
                    TextButton(onClick = { step = AuthStep.Login }, enabled = !isBusy) {
                        Text("Back")
                    }
                    TextFieldBlock("Email", forgotEmail) { forgotEmail = it }
                    PrimaryButton("Send reset code", isBusy) {
                        onAction.forgotPassword(forgotEmail.trim())
                    }
                }

                AuthStep.ResetPassword -> AuthCard {
                    TextButton(onClick = { step = AuthStep.Login }, enabled = !isBusy) {
                        Text("Back")
                    }
                    TextFieldBlock("Email", resetEmail) { resetEmail = it }
                    TextFieldBlock("Reset code", resetCode) { resetCode = it }
                    TextFieldBlock("New password", newPassword, secure = true) { newPassword = it }
                    PrimaryButton("Reset password", isBusy) {
                        onAction.resetPassword(resetEmail.trim(), resetCode.trim(), newPassword)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedShell(
    session: SessionUiState,
    saints: ContentListUiState<SaintSummary>,
    novenas: ContentListUiState<NovenaSummary>,
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onLogout: () -> Unit,
    onSaintQueryChanged: (String) -> Unit,
    onNovenaQueryChanged: (String) -> Unit,
    onReloadSaints: () -> Unit,
    onReloadNovenas: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1B2E3C)
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
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (selectedTab == tab) Color(0xFF7AC8EA) else Color(0xFFBCC9D6),
                                        CircleShape
                                    )
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
            item {
                Text(
                    text = when (selectedTab) {
                        AppTab.Home -> "Sanctuary Dev"
                        AppTab.Novenas -> "Novenas"
                        AppTab.Saints -> "Saints"
                        AppTab.Me -> "Me"
                    },
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            when (selectedTab) {
                AppTab.Home -> {
                    item {
                        HomeCard(session)
                    }
                    item {
                        SectionHint(
                            title = "What is real already",
                            body = "Auth refresh, session restore, live saints, and live novenas are wired to the same backend the iOS app uses."
                        )
                    }
                }

                AppTab.Novenas -> {
                    item {
                        SearchCard(
                            title = "Search novenas",
                            query = novenas.query,
                            onQueryChanged = onNovenaQueryChanged,
                            onSubmit = onReloadNovenas
                        )
                    }
                    item {
                        if (novenas.isLoading) {
                            InlineLoading("Loading novenas…")
                        } else if (novenas.error != null) {
                            Banner(novenas.error, isError = true)
                        } else {
                            Text("${novenas.items.size} results", color = Color(0xFFD0DFEA))
                        }
                    }
                    items(novenas.items) { item ->
                        ContentCard(
                            title = item.title,
                            subtitle = item.description,
                            detail = "${item.durationDays}-day novena"
                        )
                    }
                }

                AppTab.Saints -> {
                    item {
                        SearchCard(
                            title = "Search saints",
                            query = saints.query,
                            onQueryChanged = onSaintQueryChanged,
                            onSubmit = onReloadSaints
                        )
                    }
                    item {
                        if (saints.isLoading) {
                            InlineLoading("Loading saints…")
                        } else if (saints.error != null) {
                            Banner(saints.error, isError = true)
                        } else {
                            Text("${saints.items.size} results", color = Color(0xFFD0DFEA))
                        }
                    }
                    items(saints.items) { item ->
                        ContentCard(
                            title = item.name,
                            subtitle = item.summary ?: "Featured in Sanctuary",
                            detail = item.feastLabel
                        )
                    }
                }

                AppTab.Me -> {
                    item {
                        HomeCard(session)
                    }
                    item {
                        PrimaryButton("Logout", false, onClick = onLogout)
                    }
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
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        visualTransformation = if (secure) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun PrimaryButton(
    title: String,
    isBusy: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isBusy,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5CAED4)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(title, color = Color.White)
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
    detail: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xB323394C)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    }
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
