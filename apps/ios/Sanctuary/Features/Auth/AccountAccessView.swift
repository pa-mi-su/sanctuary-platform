import SwiftUI
import Combine

private enum AccountAccessStep {
    case landing
    case login
    case register
    case confirm
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
                Button(copy("Login", "Iniciar sesión", "Zaloguj się")) {
                    Task {
                        await accountStore.login(email: loginEmail.trimmed, password: loginPassword)
                    }
                }
                .buttonStyle(PrimaryPillButtonStyle())
                .disabled(accountStore.status == .loading || loginEmail.trimmed.isEmpty || loginPassword.isEmpty)
            }

        case .register:
            VStack(alignment: .leading, spacing: 14) {
                authTextField(title: copy("First name", "Nombre", "Imię"), text: $registerFirstName, textContentType: .givenName)
                authTextField(title: copy("Last name", "Apellido", "Nazwisko"), text: $registerLastName, textContentType: .familyName)
                authTextField(title: copy("Email", "Correo", "Email"), text: $registerEmail, keyboardType: .emailAddress, textContentType: .emailAddress)
                authSecureField(title: copy("Password", "Contraseña", "Hasło"), text: $registerPassword, textContentType: .newPassword)
                authSecureField(title: copy("Confirm password", "Confirmar contraseña", "Potwierdź hasło"), text: $registerPasswordConfirmation, textContentType: .newPassword)

                passwordPanel

                Button(copy("Create account", "Crear cuenta", "Utwórz konto")) {
                    Task {
                        await accountStore.register(
                            firstName: registerFirstName.trimmed,
                            lastName: registerLastName.trimmed,
                            email: registerEmail.trimmed,
                            password: registerPassword
                        )
                    }
                }
                .buttonStyle(PrimaryPillButtonStyle())
                .disabled(accountStore.status == .loading || !canSubmitRegistration)
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

                Button(copy("Confirm account", "Confirmar cuenta", "Potwierdź konto")) {
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
                }
                .buttonStyle(PrimaryPillButtonStyle())
                .disabled(accountStore.status == .loading || confirmationCode.trimmed.isEmpty)

                Button(copy("Send a new code", "Enviar un código nuevo", "Wyślij nowy kod")) {
                    Task {
                        await accountStore.resendConfirmation()
                    }
                }
                .buttonStyle(SecondaryPillButtonStyle())
                .disabled(accountStore.status == .loading)
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
        case .landing:
            return copy(
                "Choose login if you already belong here, or register if this is the beginning of your Sanctuary.",
                "Elige iniciar sesión si ya perteneces aquí, o regístrate si este es el comienzo de tu Sanctuary.",
                "Wybierz logowanie, jeśli już tu należysz, albo rejestrację, jeśli to początek twojego Sanctuary."
            )
        }
    }

    private var passwordRules: [(label: String, met: Bool)] {
        [
            (copy("At least 8 characters", "Al menos 8 caracteres", "Co najmniej 8 znaków"), registerPassword.count >= 8),
            (copy("One uppercase letter", "Una letra mayúscula", "Jedna wielka litera"), registerPassword.range(of: "[A-Z]", options: .regularExpression) != nil),
            (copy("One lowercase letter", "Una letra minúscula", "Jedna mała litera"), registerPassword.range(of: "[a-z]", options: .regularExpression) != nil),
            (copy("One number", "Un número", "Jedna cyfra"), registerPassword.range(of: "\\d", options: .regularExpression) != nil),
            (copy("One special character", "Un carácter especial", "Jeden znak specjalny"), registerPassword.range(of: "[^A-Za-z0-9]", options: .regularExpression) != nil),
        ]
    }

    private var passwordsMatch: Bool {
        !registerPasswordConfirmation.isEmpty && registerPassword == registerPasswordConfirmation
    }

    private var passwordStrengthLabel: String {
        let metCount = passwordRules.filter(\.met).count
        if metCount == passwordRules.count {
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

    private var passwordPanel: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(copy("Password strength", "Seguridad de contraseña", "Siła hasła"))
                    .font(AppTheme.rounded(15, weight: .bold))
                    .foregroundStyle(.white)
                Spacer()
                Text(passwordStrengthLabel)
                    .font(AppTheme.rounded(14, weight: .semibold))
                    .foregroundStyle(passwordRules.allSatisfy(\.met) ? AppTheme.tabActive : AppTheme.subtitleText)
            }

            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(passwordRules.enumerated()), id: \.offset) { _, rule in
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
                passwordsMatch
                    ? copy("Passwords match.", "Las contraseñas coinciden.", "Hasła są zgodne.")
                    : copy("Passwords must match before you can create the account.", "Las contraseñas deben coincidir antes de crear la cuenta.", "Hasła muszą się zgadzać przed utworzeniem konta.")
            )
            .font(AppTheme.rounded(14, weight: .medium))
            .foregroundStyle(passwordsMatch ? AppTheme.tabActive : AppTheme.subtitleText)
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
