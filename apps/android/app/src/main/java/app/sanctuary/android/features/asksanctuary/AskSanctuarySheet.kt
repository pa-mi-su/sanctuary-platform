package app.sanctuary.android.features.asksanctuary

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sanctuary.android.AskSanctuaryUiState
import app.sanctuary.android.SessionStatus
import app.sanctuary.android.SessionUiState
import app.sanctuary.android.data.AskSanctuaryResponse
import app.sanctuary.android.data.AskSanctuaryScriptureReference
import app.sanctuary.android.ui.AppLanguage
import app.sanctuary.android.ui.sanctuaryStrings

@Composable
fun AskSanctuarySheet(
    session: SessionUiState,
    state: AskSanctuaryUiState,
    onMessageChanged: (String) -> Unit,
    onAcceptDisclaimer: () -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    accountContent: @Composable ColumnScope.() -> Unit
) {
    val l10n = sanctuaryStrings()
    val focusManager = LocalFocusManager.current
    val canSubmit = normalizedFeelingWords(state.message) != null && !state.isSubmitting
    val status = state.status

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = l10n.t("ask.title"),
            color = Color.White,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = l10n.t("ask.subtitle"),
            color = Color(0xFFD0DFEA),
            lineHeight = 22.sp
        )

        AskSanctuaryIntroCard()

        when {
            session.status != SessionStatus.Authenticated -> {
                AskSanctuaryNoticeCard(
                    title = l10n.t("ask.accountRequiredTitle"),
                    body = l10n.t("ask.accountRequiredBody")
                )
                accountContent()
            }

            state.isLoadingStatus && status == null -> {
                AskSanctuaryLoadingCard()
            }

            state.error != null && status == null -> {
                AskSanctuaryBanner(message = state.error, isError = true)
            }

            status?.available == false -> {
                AskSanctuaryNoticeCard(
                    title = l10n.t("ask.unavailableTitle"),
                    body = status.unavailableMessage?.takeIf { l10n.language == AppLanguage.English } ?: l10n.t("ask.unavailableBody")
                )
            }

            status?.disclaimerAccepted == false -> {
                AskSanctuaryDisclaimerCard(
                    isLoading = state.isLoadingStatus,
                    onAccept = onAcceptDisclaimer,
                    onCancel = onCancel
                )
            }

            else -> {
                AskSanctuaryPromptCard(
                    message = state.message,
                    isSubmitting = state.isSubmitting,
                    canSubmit = canSubmit,
                    onMessageChanged = onMessageChanged,
                    onSubmit = {
                        focusManager.clearFocus()
                        onSubmit()
                    },
                    onClear = {
                        focusManager.clearFocus()
                        onClear()
                    }
                )
                if (state.isSubmitting) {
                    AskSanctuaryLoadingCard()
                }
                state.error?.let {
                    AskSanctuaryBanner(message = it, isError = true)
                }
                state.response?.let {
                    AskSanctuaryResponseCard(response = it)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AskSanctuaryIntroCard() {
    val l10n = sanctuaryStrings()
    AskSanctuaryCard {
        Text(l10n.t("ask.eyebrow"), color = Color(0xFF9BEAFF), fontWeight = FontWeight.Bold)
        Text(
            l10n.t("ask.headline"),
            color = Color.White,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold
        )
        Text(l10n.t("ask.body"), color = Color(0xFFD0DFEA), lineHeight = 22.sp)
    }
}

@Composable
private fun AskSanctuaryNoticeCard(title: String, body: String) {
    AskSanctuaryCard {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(body, color = Color(0xFFD0DFEA), lineHeight = 22.sp)
    }
}

@Composable
private fun AskSanctuaryLoadingCard() {
    val l10n = sanctuaryStrings()
    AskSanctuaryCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(color = Color(0xFF9BEAFF))
            Text(l10n.t("ask.loading"), color = Color.White)
        }
    }
}

@Composable
private fun AskSanctuaryDisclaimerCard(
    isLoading: Boolean,
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    val l10n = sanctuaryStrings()
    AskSanctuaryCard {
        Text(l10n.t("ask.disclaimerTitle"), color = Color.White, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
        AskSanctuaryDisclaimerRow(l10n.t("ask.disclaimerPrivacyTitle"), l10n.t("ask.disclaimerPrivacyBody"))
        AskSanctuaryDisclaimerRow(l10n.t("ask.disclaimerFaithTitle"), l10n.t("ask.disclaimerFaithBody"))
        AskSanctuaryDisclaimerRow(l10n.t("ask.disclaimerCareTitle"), l10n.t("ask.disclaimerCareBody"))
        AskSanctuaryDisclaimerRow(l10n.t("ask.disclaimerSupportTitle"), l10n.t("ask.disclaimerSupportBody"))
        AskSanctuaryPrimaryButton(
            title = l10n.t("ask.disclaimerAccept"),
            isBusy = isLoading,
            enabled = !isLoading,
            onClick = onAccept
        )
        Button(
            onClick = onCancel,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color(0xFFD0DFEA)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(l10n.t("ask.disclaimerCancel"), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AskSanctuaryDisclaimerRow(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
        Text(body, color = Color(0xFFD0DFEA), lineHeight = 21.sp)
    }
}

@Composable
private fun AskSanctuaryPromptCard(
    message: String,
    isSubmitting: Boolean,
    canSubmit: Boolean,
    onMessageChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit
) {
    val l10n = sanctuaryStrings()
    val focusManager = LocalFocusManager.current
    AskSanctuaryCard {
        Text(l10n.t("ask.promptTitle"), color = Color.White, fontSize = 21.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = message,
            onValueChange = onMessageChanged,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            placeholder = { Text(l10n.t("ask.placeholder"), color = Color.White.copy(alpha = 0.45f)) },
            enabled = !isSubmitting,
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    focusManager.clearFocus()
                    if (canSubmit) onSubmit()
                }
            )
        )
        Text(l10n.t("ask.promptHelp"), color = Color.White.copy(alpha = 0.62f), fontSize = 13.sp, lineHeight = 17.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AskSanctuaryPrimaryButton(
                title = l10n.t("ask.submit"),
                isBusy = isSubmitting,
                enabled = canSubmit,
                modifier = Modifier.weight(1f),
                onClick = onSubmit
            )
            Button(
                onClick = onClear,
                enabled = !isSubmitting,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.10f),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = l10n.t("common.cancel"))
            }
        }
    }
}

@Composable
private fun AskSanctuaryResponseCard(response: AskSanctuaryResponse) {
    val l10n = sanctuaryStrings()
    AskSanctuaryCard {
        response.message?.takeIf { it.isNotBlank() }?.let {
            AskSanctuaryBanner(message = it, isError = response.status.equals("GUARDED", ignoreCase = true))
        }
        AskResponseSection(l10n.t("ask.theme"), response.theme)
        val scripture = listOfNotNull(
            response.oldTestament?.let { l10n.t("ask.oldTestament") to it },
            response.newTestament?.let { l10n.t("ask.newTestament") to it }
        )
        scripture.forEach { (label, reference) ->
            AskResponseSection(label, reference.displayText())
        }
        AskResponseSection(l10n.t("ask.saint"), response.saint)
        AskResponseSection(l10n.t("ask.prayer"), response.prayer)
        AskResponseSection(l10n.t("ask.reflection"), response.reflection)
        AskResponseSection(l10n.t("ask.action"), response.action)
    }
}

@Composable
private fun AskResponseSection(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Color(0xFF9BEAFF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(value, color = Color.White, lineHeight = 21.sp)
    }
}

@Composable
private fun AskSanctuaryCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC1A3448)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
private fun AskSanctuaryBanner(message: String, isError: Boolean) {
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
private fun AskSanctuaryPrimaryButton(
    title: String,
    isBusy: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isBusy,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5CAED4)),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            }
            Text(title, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun AskSanctuaryScriptureReference.displayText(): String =
    "$book $chapter:$verse"

private fun normalizedFeelingWords(message: String): String? {
    val trimmed = message.trim()
    if (trimmed.isBlank()) return null
    if (!Regex("^[\\p{L}\\s,'-]+$").matches(trimmed)) return null
    val blockedWords = setOf(
        "ass",
        "bullshit",
        "crap",
        "fart",
        "fuck",
        "fucked",
        "fucking",
        "nigger",
        "pee",
        "poo",
        "poop",
        "pooped",
        "pooping",
        "shit",
        "shitting"
    )
    val words = trimmed
        .replace(',', ' ')
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .map { it.lowercase() }

    return words.takeIf { values ->
        values.size == 1 && values.all { word -> word.length in 2..24 && !blockedWords.contains(word) }
    }?.first()
}
