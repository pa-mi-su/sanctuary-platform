import SwiftUI
import Combine

private enum AccountAccessStep {
    case landing
    case login
    case register
    case confirm
    case forgotPassword
    case resetPassword
}

struct AccountAccessView: View {
    @EnvironmentObject private var localization: LocalizationManager
    @EnvironmentObject private var accountStore: AccountSessionStore

    @State private var step: AccountAccessStep = .landing
    @State private var loginEmail = ""
    @State private var loginPassword = ""
    @State private var registerFirstName = ""
    @State private var registerLastName = ""
    @State private var registerEmail = ""
    @State private var registerPassword = ""
    @State private var registerPasswordConfirmation = ""
    @State private var confirmationCode = ""
    @State private var forgotPasswordEmail = ""
    @State private var resetPasswordCode = ""
    @State private var resetPassword = ""
    @State private var resetPasswordConfirmation = ""

    private var isBusy: Bool {
        accountStore.status == .loading
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text(copy("Sanctuary account", "Cuenta de Sanctuary", "Konto Sanctuary"))
                .font(AppTheme.rounded(16, weight: .bold))
                .foregroundStyle(AppTheme.tabActive)

            Text(heading)
                .font(AppTheme.rounded(36, weight: .bold))
                .foregroundStyle(.white)

            Text(supportingCopy)
                .font(AppTheme.rounded(18, weight: .medium))
                .foregroundStyle(AppTheme.subtitleText)

            if let message = accountStore.message, !message.isEmpty {
                Text(message)
                    .font(AppTheme.rounded(15, weight: .semibold))
                    .foregroundStyle(accountStore.isErrorMessage ? Color(red: 1, green: 0.86, blue: 0.86) : AppTheme.tabActive)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .fill(accountStore.isErrorMessage ? Color.red.opacity(0.16) : AppTheme.cardBackgroundSoft)
                    )
            }

            if step == .landing {
                VStack(spacing: 12) {
                    authChoiceButton(
                        eyebrow: copy("Returning to Sanctuary", "Volver a Sanctuary", "Powrót do Sanctuary"),
                        title: copy("Login", "Iniciar sesión", "Logowanie"),
                        body: copy(
                            "Sign in to your saved saints, novenas, and progress.",
                            "Inicia sesión para volver a tus santos, novenas y progreso.",
                            "Zaloguj sie do zapisanych swietych, nowenn i postepow."
                        )
                    ) {
                        step = .login
                    }

                    authChoiceButton(
                        eyebrow: copy("New to Sanctuary", "Nuevo en Sanctuary", "Nowe konto Sanctuary"),
                        title: copy("Register", "Registrarse", "Rejestracja"),
                        body: copy(
                            "Create a free account to sync your prayer life across devices.",
                            "Crea una cuenta gratuita para sincronizar tu vida de oración entre dispositivos.",
                            "Utwórz darmowe konto, aby synchronizować życie modlitwy między urządzeniami."
                        )
                    ) {
                        step = .register
                    }
                }
            } else {
                Button {
                    goBack()
                } label: {
                    Text("← \(copy("Back", "Atrás", "Powrót"))")
                        .font(AppTheme.rounded(15, weight: .semibold))
                        .foregroundStyle(AppTheme.tabActive)
                }
                .buttonStyle(.plain)
                .disabled(isBusy)

                formBody
            }

            VStack(alignment: .leading, spacing: 10) {
                benefitLine(copy("Track novenas in progress", "Rastrea novenas en curso", "Śledź nowenny w toku"))
                benefitLine(copy("Save favorite saints and novenas", "Guarda santos y novenas favoritas", "Zapisuj ulubionych świętych i nowenny"))
                benefitLine(copy("Resume later on web, iOS, and Android", "Continúa luego en web, iOS y Android", "Wracaj później na web, iOS i Android"))
            }
            .padding(.top, 6)
        }
        .padding(18)
        .appGlassCard(cornerRadius: 28)
        .onReceive(accountStore.$pendingConfirmationEmail) { newValue in
            if newValue != nil {
                step = .confirm
            }
        }
    }

    @ViewBuilder
    private var formBody: some View {
        switch step {
        case .login:
            VStack(alignment: .leading, spacing: 14) {
                authTextField(title: copy("Email", "Correo", "Email"), text: $loginEmail, keyboardType: .emailAddress, textContentType: .emailAddress)
                authSecureField(title: copy("Password", "Contraseña", "Hasło"), text: $loginPassword, textContentType: .password)
                Button(copy("Forgot password?", "¿Olvidaste tu contraseña?", "Nie pamiętasz hasła?")) {
                    forgotPasswordEmail = loginEmail.trimmed
                    step = .forgotPassword
                    accountStore.clearTransientMessage()
                }
                .buttonStyle(.plain)
                .font(AppTheme.rounded(15, weight: .semibold))
                .foregroundStyle(AppTheme.tabActive)
                .disabled(isBusy)

                Button {
                    guard !isBusy else { return }
                    Task {
                        await accountStore.login(email: loginEmail.trimmed, password: loginPassword)
                    }
                } label: {
                    authActionLabel(
                        idleTitle: copy("Login", "Iniciar sesión", "Zaloguj się"),
                        busyTitle: copy("Signing in…", "Entrando…", "Logowanie…")
                    )
                }
                .buttonStyle(PrimaryPillButtonStyle())
                .disabled(isBusy || loginEmail.trimmed.isEmpty || loginPassword.isEmpty)
            }

        case .register:
            VStack(alignment: .leading, spacing: 14) {
                authTextField(title: copy("First name", "Nombre", "Imię"), text: $registerFirstName, textContentType: .givenName)
                authTextField(title: copy("Last name", "Apellido", "Nazwisko"), text: $registerLastName, textContentType: .familyName)
                authTextField(title: copy("Email", "Correo", "Email"), text: $registerEmail, keyboardType: .emailAddress, textContentType: .emailAddress)
                authSecureField(title: copy("Password", "Contraseña", "Hasło"), text: $registerPassword, textContentType: .newPassword)
                authSecureField(title: copy("Confirm password", "Confirmar contraseña", "Potwierdź hasło"), text: $registerPasswordConfirmation, textContentType: .newPassword)

                passwordPanel

                Button {
                    guard !isBusy else { return }
                    Task {
                        await accountStore.register(
                            firstName: registerFirstName.trimmed,
                            lastName: registerLastName.trimmed,
                            email: registerEmail.trimmed,
                            password: registerPassword
                        )
                    }
                } label: {
                    authActionLabel(
                        idleTitle: copy("Create account", "Crear cuenta", "Utwórz konto"),
                        busyTitle: copy("Creating account…", "Creando cuenta…", "Tworzenie konta…")
                    )
                }
                .buttonStyle(PrimaryPillButtonStyle())
                .disabled(isBusy || !canSubmitRegistration)
            }

        case .confirm:
            VStack(alignment: .leading, spacing: 14) {
                Text(
                    "\(copy("We sent a confirmation code to", "Enviamos un código de confirmación a", "Wysłaliśmy kod potwierdzający na")) \(accountStore.pendingConfirmationEmail ?? registerEmail.trimmed)."
                )
                .font(AppTheme.rounded(16, weight: .medium))
                .foregroundStyle(AppTheme.subtitleText)

                authTextField(
                    title: copy("Verification code", "Código de verificación", "Kod weryfikacyjny"),
                    text: $confirmationCode,
                    keyboardType: .numberPad,
                    textContentType: .oneTimeCode
                )

                Button {
                    guard !isBusy else { return }
                    Task {
                        let email = accountStore.pendingConfirmationEmail ?? registerEmail.trimmed
                        let password = registerPassword
                        let confirmed = await accountStore.confirmRegistration(code: confirmationCode.trimmed)

                        guard confirmed else { return }

                        confirmationCode = ""

                        if !email.isEmpty && !password.isEmpty {
                            await accountStore.login(email: email, password: password)

                            if accountStore.isAuthenticated {
                                step = .landing
                                loginEmail = ""
                                loginPassword = ""
                                registerFirstName = ""
                                registerLastName = ""
                                registerEmail = ""
                                registerPassword = ""
                                registerPasswordConfirmation = ""
                            } else {
                                loginEmail = email
                                loginPassword = ""
                                accountStore.setConfirmedPrompt()
                                step = .login
                            }
                        } else {
                            loginEmail = email
                            loginPassword = ""
                            accountStore.setConfirmedPrompt()
                            step = .login
                        }
                    }
                } label: {
                    authActionLabel(
                        idleTitle: copy("Confirm account", "Confirmar cuenta", "Potwierdź konto"),
                        busyTitle: copy("Confirming…", "Confirmando…", "Potwierdzanie…")
                    )
                }
                .buttonStyle(PrimaryPillButtonStyle())
                .disabled(isBusy || confirmationCode.trimmed.isEmpty)

                Button {
                    guard !isBusy else { return }
                    Task {
                        await accountStore.resendConfirmation()
                    }
                } label: {
                    authSecondaryActionLabel(
                        idleTitle: copy("Send a new code", "Enviar un código nuevo", "Wyślij nowy kod"),
                        busyTitle: copy("Sending…", "Enviando…", "Wysyłanie…")
                    )
                }
                .buttonStyle(SecondaryPillButtonStyle())
                .disabled(isBusy)
            }

        case .forgotPassword:
            VStack(alignment: .leading, spacing: 14) {
                authTextField(title: copy("Email", "Correo", "Email"), text: $forgotPasswordEmail, keyboardType: .emailAddress, textContentType: .emailAddress)

                Button {
                    guard !isBusy else { return }
                    Task {
                        let email = forgotPasswordEmail.trimmed
                        await accountStore.forgotPassword(email: email)
                        if accountStore.isErrorMessage == false {
                            forgotPasswordEmail = email
                            resetPasswordCode = ""
                            resetPassword = ""
                            resetPasswordConfirmation = ""
                            step = .resetPassword
                        }
                    }
                } label: {
                    authActionLabel(
                        idleTitle: copy("Send reset code", "Enviar código", "Wyślij kod"),
                        busyTitle: copy("Sending code…", "Enviando código…", "Wysyłanie kodu…")
                    )
                }
                .buttonStyle(PrimaryPillButtonStyle())
                .disabled(isBusy || forgotPasswordEmail.trimmed.isEmpty)
            }

        case .resetPassword:
            VStack(alignment: .leading, spacing: 14) {
                Text(
                    "\(copy("We sent a reset code to", "Enviamos un código de restablecimiento a", "Wysłaliśmy kod resetujący na")) \(accountStore.pendingPasswordResetEmail ?? forgotPasswordEmail.trimmed)."
                )
                .font(AppTheme.rounded(16, weight: .medium))
                .foregroundStyle(AppTheme.subtitleText)

                authTextField(
                    title: copy("Reset code", "Código de restablecimiento", "Kod resetujący"),
                    text: $resetPasswordCode,
                    keyboardType: .numberPad,
                    textContentType: .oneTimeCode
                )

                authSecureField(title: copy("New password", "Nueva contraseña", "Nowe hasło"), text: $resetPassword, textContentType: .newPassword)
                authSecureField(title: copy("Confirm new password", "Confirmar nueva contraseña", "Potwierdź nowe hasło"), text: $resetPasswordConfirmation, textContentType: .newPassword)

                passwordPanel(
                    password: resetPassword,
                    confirmation: resetPasswordConfirmation,
                    confirmationWarning: copy(
                        "Passwords must match before you can save the new password.",
                        "Las contraseñas deben coincidir antes de guardar la nueva contraseña.",
                        "Hasła muszą się zgadzać przed zapisaniem nowego hasła."
                    )
                )

                Button {
                    guard !isBusy else { return }
                    Task {
                        let email = accountStore.pendingPasswordResetEmail ?? forgotPasswordEmail.trimmed
                        let reset = await accountStore.resetPassword(
                            email: email,
                            code: resetPasswordCode.trimmed,
                            newPassword: resetPassword
                        )

                        guard reset else { return }

                        loginEmail = email
                        loginPassword = ""
                        resetPasswordCode = ""
                        resetPassword = ""
                        resetPasswordConfirmation = ""
                        step = .login
                    }
                } label: {
                    authActionLabel(
                        idleTitle: copy("Save new password", "Guardar nueva contraseña", "Zapisz nowe hasło"),
                        busyTitle: copy("Updating password…", "Actualizando contraseña…", "Aktualizowanie hasła…")
                    )
                }
                .buttonStyle(PrimaryPillButtonStyle())
                .disabled(isBusy || resetPasswordCode.trimmed.isEmpty || !isPasswordReady(resetPassword) || !passwordsMatch(resetPassword, resetPasswordConfirmation))

                Button {
                    guard !isBusy else { return }
                    Task {
                        let email = accountStore.pendingPasswordResetEmail ?? forgotPasswordEmail.trimmed
                        await accountStore.forgotPassword(email: email)
                    }
                } label: {
                    authSecondaryActionLabel(
                        idleTitle: copy("Send a new reset code", "Enviar un nuevo código", "Wyślij nowy kod resetujący"),
                        busyTitle: copy("Sending…", "Enviando…", "Wysyłanie…")
                    )
                }
                .buttonStyle(SecondaryPillButtonStyle())
                .disabled(isBusy)
            }

        case .landing:
            EmptyView()
        }
    }

    private var heading: String {
        switch step {
        case .login:
            return copy("Welcome back", "Bienvenido de nuevo", "Witaj ponownie")
        case .register:
            return copy("Create your Sanctuary account", "Crea tu cuenta de Sanctuary", "Utwórz konto Sanctuary")
        case .confirm:
            return copy("Confirm your account", "Confirma tu cuenta", "Potwierdź konto")
        case .forgotPassword:
            return copy("Reset your password calmly", "Restablece tu contraseña con calma", "Spokojnie zresetuj hasło")
        case .resetPassword:
            return copy("Choose a secure new password", "Elige una nueva contraseña segura", "Wybierz bezpieczne nowe hasło")
        case .landing:
            return copy("Choose your way in", "Elige cómo entrar", "Wybierz drogę wejścia")
        }
    }

    private var supportingCopy: String {
        switch step {
        case .login:
            return copy(
                "Everything you have saved in Sanctuary should feel close, calm, and ready to continue.",
                "Todo lo que guardaste en Sanctuary debe sentirse cerca, en calma y listo para continuar.",
                "Wszystko, co zapisano w Sanctuary, powinno być blisko, spokojne i gotowe do kontynuacji."
            )
        case .register:
            return copy(
                "A real account gives you a real home for your favorites, novena progress, and future reminders.",
                "Una cuenta real te da un hogar verdadero para tus favoritos, el progreso de novenas y futuros recordatorios.",
                "Prawdziwe konto daje prawdziwy dom dla ulubionych, postępów nowenn i przyszłych przypomnień."
            )
        case .confirm:
            return copy(
                "One more step and your Sanctuary account is ready.",
                "Un paso más y tu cuenta de Sanctuary estará lista.",
                "Jeszcze jeden krok i konto Sanctuary będzie gotowe."
            )
        case .forgotPassword:
            return copy(
                "We will send a reset code so you can get back into Sanctuary without losing your place.",
                "Te enviaremos un código para que vuelvas a Sanctuary sin perder tu lugar.",
                "Wyślemy kod resetujący, abyś mógł wrócić do Sanctuary bez utraty swojego miejsca."
            )
        case .resetPassword:
            return copy(
                "Choose a strong password and we will bring you back into Sanctuary cleanly.",
                "Elige una contraseña segura y te llevaremos de vuelta a Sanctuary sin fricción.",
                "Wybierz silne hasło, a płynnie wrócisz do Sanctuary."
            )
        case .landing:
            return copy(
                "Choose login if you already belong here, or register if this is the beginning of your Sanctuary.",
                "Elige iniciar sesión si ya perteneces aquí, o regístrate si este es el comienzo de tu Sanctuary.",
                "Wybierz logowanie, jeśli już tu należysz, albo rejestrację, jeśli to początek twojego Sanctuary."
            )
        }
    }

    private var passwordRules: [(label: String, met: Bool)] {
        passwordRules(for: registerPassword)
    }

    private var resetPasswordRules: [(label: String, met: Bool)] {
        passwordRules(for: resetPassword)
    }

    private func passwordRules(for password: String) -> [(label: String, met: Bool)] {
        [
            (copy("At least 8 characters", "Al menos 8 caracteres", "Co najmniej 8 znaków"), password.count >= 8),
            (copy("One uppercase letter", "Una letra mayúscula", "Jedna wielka litera"), password.range(of: "[A-Z]", options: .regularExpression) != nil),
            (copy("One lowercase letter", "Una letra minúscula", "Jedna mała litera"), password.range(of: "[a-z]", options: .regularExpression) != nil),
            (copy("One number", "Un número", "Jedna cyfra"), password.range(of: "\\d", options: .regularExpression) != nil),
            (copy("One special character", "Un carácter especial", "Jeden znak specjalny"), password.range(of: "[^A-Za-z0-9]", options: .regularExpression) != nil),
        ]
    }

    private var passwordsMatch: Bool {
        !registerPasswordConfirmation.isEmpty && registerPassword == registerPasswordConfirmation
    }

    private func passwordsMatch(_ password: String, _ confirmation: String) -> Bool {
        !confirmation.isEmpty && password == confirmation
    }

    private var passwordStrengthLabel: String {
        passwordStrengthLabel(for: passwordRules)
    }

    private var resetPasswordStrengthLabel: String {
        passwordStrengthLabel(for: resetPasswordRules)
    }

    private func passwordStrengthLabel(for rules: [(label: String, met: Bool)]) -> String {
        let metCount = rules.filter(\.met).count
        if metCount == rules.count {
            return copy("Ready", "Lista", "Gotowe")
        }
        if metCount >= 4 {
            return copy("Almost there", "Casi lista", "Prawie gotowe")
        }
        if metCount >= 2 {
            return copy("Needs work", "Necesita trabajo", "Wymaga poprawy")
        }
        return copy("Too weak", "Demasiado débil", "Za słabe")
    }

    private var canSubmitRegistration: Bool {
        !registerFirstName.trimmed.isEmpty &&
        !registerLastName.trimmed.isEmpty &&
        !registerEmail.trimmed.isEmpty &&
        passwordRules.allSatisfy(\.met) &&
        passwordsMatch
    }

    private func isPasswordReady(_ password: String) -> Bool {
        passwordRules(for: password).allSatisfy(\.met)
    }

    private var passwordPanel: some View {
        passwordPanel(
            password: registerPassword,
            confirmation: registerPasswordConfirmation,
            confirmationWarning: copy(
                "Passwords must match before you can create the account.",
                "Las contraseñas deben coincidir antes de crear la cuenta.",
                "Hasła muszą się zgadzać przed utworzeniem konta."
            )
        )
    }

    private func passwordPanel(
        password: String,
        confirmation: String,
        confirmationWarning: String
    ) -> some View {
        let rules = passwordRules(for: password)
        let matches = passwordsMatch(password, confirmation)

        return VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(copy("Password strength", "Seguridad de contraseña", "Siła hasła"))
                    .font(AppTheme.rounded(15, weight: .bold))
                    .foregroundStyle(.white)
                Spacer()
                Text(passwordStrengthLabel(for: rules))
                    .font(AppTheme.rounded(14, weight: .semibold))
                    .foregroundStyle(rules.allSatisfy(\.met) ? AppTheme.tabActive : AppTheme.subtitleText)
            }

            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(rules.enumerated()), id: \.offset) { _, rule in
                    HStack(spacing: 8) {
                        Text(rule.met ? "✓" : "•")
                            .font(AppTheme.rounded(14, weight: .bold))
                            .foregroundStyle(rule.met ? AppTheme.tabActive : AppTheme.subtitleText)
                        Text(rule.label)
                            .font(AppTheme.rounded(14, weight: .medium))
                            .foregroundStyle(rule.met ? .white : AppTheme.subtitleText)
                    }
                }
            }

            Text(
                matches
                    ? copy("Passwords match.", "Las contraseñas coinciden.", "Hasła są zgodne.")
                    : confirmationWarning
            )
            .font(AppTheme.rounded(14, weight: .medium))
            .foregroundStyle(matches ? AppTheme.tabActive : AppTheme.subtitleText)
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(AppTheme.cardBackgroundSoft)
                .overlay(
                    RoundedRectangle(cornerRadius: 22, style: .continuous)
                        .stroke(Color.white.opacity(0.12), lineWidth: 1)
                )
        )
    }

    private func authChoiceButton(
        eyebrow: String,
        title: String,
        body: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 8) {
                Text(eyebrow.uppercased())
                    .font(AppTheme.rounded(12, weight: .bold))
                    .foregroundStyle(AppTheme.tabActive)
                Text(title)
                    .font(AppTheme.rounded(24, weight: .bold))
                    .foregroundStyle(.white)
                Text(body)
                    .font(AppTheme.rounded(16, weight: .medium))
                    .foregroundStyle(AppTheme.subtitleText)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .appGlassCard(cornerRadius: 22)
        }
        .buttonStyle(.plain)
        .disabled(isBusy)
    }

    private func authActionLabel(idleTitle: String, busyTitle: String) -> some View {
        HStack(spacing: 10) {
            if isBusy {
                ProgressView()
                    .progressViewStyle(.circular)
                    .tint(.white)
                    .scaleEffect(0.9)
            }
            Text(isBusy ? busyTitle : idleTitle)
        }
        .frame(maxWidth: .infinity)
    }

    private func authSecondaryActionLabel(idleTitle: String, busyTitle: String) -> some View {
        HStack(spacing: 10) {
            if isBusy {
                ProgressView()
                    .progressViewStyle(.circular)
                    .tint(AppTheme.tabActive)
                    .scaleEffect(0.9)
            }
            Text(isBusy ? busyTitle : idleTitle)
        }
        .frame(maxWidth: .infinity)
    }

    private func authTextField(
        title: String,
        text: Binding<String>,
        keyboardType: UIKeyboardType = .default,
        textContentType: UITextContentType? = nil
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(AppTheme.rounded(15, weight: .bold))
                .foregroundStyle(.white)
            TextField("", text: text)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .keyboardType(keyboardType)
                .textContentType(textContentType)
                .font(AppTheme.rounded(16, weight: .medium))
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(AppTheme.cardBackgroundSoft)
                        .overlay(
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .stroke(Color.white.opacity(0.12), lineWidth: 1)
                        )
                )
        }
    }

    private func authSecureField(
        title: String,
        text: Binding<String>,
        textContentType: UITextContentType
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(AppTheme.rounded(15, weight: .bold))
                .foregroundStyle(.white)
            SecureField("", text: text)
                .textContentType(textContentType)
                .font(AppTheme.rounded(16, weight: .medium))
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(AppTheme.cardBackgroundSoft)
                        .overlay(
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .stroke(Color.white.opacity(0.12), lineWidth: 1)
                        )
                )
        }
    }

    private func benefitLine(_ text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Circle()
                .fill(AppTheme.tabActive)
                .frame(width: 6, height: 6)
                .padding(.top, 8)
            Text(text)
                .font(AppTheme.rounded(16, weight: .medium))
                .foregroundStyle(AppTheme.subtitleText)
        }
    }

    private func goBack() {
        accountStore.clearTransientMessage()
        if step == .confirm {
            step = .register
        } else if step == .resetPassword {
            step = .forgotPassword
        } else if step == .forgotPassword {
            step = .login
        } else {
            step = .landing
        }
    }

    private func copy(_ english: String, _ spanish: String, _ polish: String) -> String {
        switch localization.language {
        case .en:
            return english
        case .es:
            return spanish
        case .pl:
            return polish
        }
    }
}

private extension String {
    var trimmed: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
