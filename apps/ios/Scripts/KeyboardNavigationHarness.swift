import SwiftUI
import UIKit

// Standalone simulator-only regression app. This file is outside the app target.
@main
struct KeyboardNavigationHarness: App {
    var body: some Scene { WindowGroup { KeyboardRegressionForm() } }
}

private struct NavigationBoundsKey: PreferenceKey {
    static var defaultValue = CGRect.zero
    static func reduce(value: inout CGRect, nextValue: () -> CGRect) { value = nextValue() }
}

private struct KeyboardRegressionForm: View {
    enum Field: Hashable { case email, password }
    @State private var covered = false
    @State private var email = ""
    @State private var password = ""
    @State private var revealed = false
    @State private var restorePassword = false
    @State private var keyboard = CGRect.zero
    @State private var navigation = CGRect.zero
    @FocusState private var focus: Field?

    var body: some View {
        KeyboardAwareScrollView {
            VStack(spacing: 20) {
                Text("Keyboard regression form").font(.title)
                Color.clear.frame(height: 500)
                TextField("Email", text: $email)
                    .textContentType(.emailAddress).keyboardType(.emailAddress)
                    .focused($focus, equals: .email).submitLabel(.next)
                    .onSubmit { focus = .password }
                    .id("email")
                    .preference(key: FocusedInputPreferenceKey.self, value: focus == .email ? "email" : nil)
                Group {
                    if revealed { TextField("Password", text: $password).task { await restoreFocus() } }
                    else { SecureField("Password", text: $password).task { await restoreFocus() } }
                }
                .focused($focus, equals: .password)
                .textContentType(.password)
                .submitLabel(.done)
                .onSubmit { focus = nil }
                .id("password")
                .preference(key: FocusedInputPreferenceKey.self, value: focus == .password ? "password" : nil)
                Color.clear.frame(height: 200)
            }.padding(24).textFieldStyle(.roundedBorder)
        }
        .scrollDismissesKeyboard(.interactively)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if !covered {
                Text("Home    Novenas    Saints    Me").frame(maxWidth: .infinity, minHeight: 66)
                    .background(Color.green.opacity(0.15))
                    .background(GeometryReader { geo in Color.clear.preference(key: NavigationBoundsKey.self, value: geo.frame(in: .global)) })
            }
        }
        .background { KeyboardOcclusionObserver(coversBottom: $covered) }
        .onPreferenceChange(NavigationBoundsKey.self) { if !$0.isEmpty { navigation = $0 } }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardDidChangeFrameNotification)) { note in
            guard let window = activeWindow, let frame = note.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect else { return }
            keyboard = window.screen.coordinateSpace.convert(frame, to: window)
        }
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) { Spacer(); Button("Done") { focus = nil } }
        }
        .task { await verify() }
    }

    private var activeWindow: UIWindow? {
        UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows).first { $0.isKeyWindow }
    }
    private func firstResponder(_ view: UIView) -> UIView? {
        if view.isFirstResponder { return view }
        for child in view.subviews { if let result = firstResponder(child) { return result } }
        return nil
    }
    private func focusedFieldIsVisible() -> Bool {
        guard let window = activeWindow, let field = firstResponder(window) else { return false }
        let rect = field.convert(field.bounds, to: window)
        return rect.minY >= window.safeAreaInsets.top - 2 && rect.maxY <= keyboard.minY + 2
    }
    private func restoreFocus() async {
        guard restorePassword else { return }
        await Task.yield()
        guard !Task.isCancelled, restorePassword else { return }
        focus = .password
        restorePassword = false
    }
    private func geometry() -> [String: Any] {
        guard let window = activeWindow else { return [:] }
        let field = firstResponder(window)
        return ["field": field.map { NSCoder.string(for: $0.convert($0.bounds, to: window)) } ?? "none",
                "class": field.map { String(describing: type(of: $0)) } ?? "none",
                "keyboard": NSCoder.string(for: keyboard), "focus": String(describing: focus), "covered": covered]
    }
    private func settle() async { try? await Task.sleep(nanoseconds: 1_200_000_000) }
    private func verify() async {
        await settle()
        let baseline = navigation
        var checks: [String: Bool] = ["navigation starts at device bottom": !covered && !baseline.isEmpty]
        email = "qa@example.invalid"
        password = "LocalOnly123!"
        focus = .email
        await settle()
        let emailGeometry = geometry()
        checks["docked email keyboard does not lift navigation"] = covered
        checks["focused email is visible above keyboard"] = focusedFieldIsVisible()
        focus = .password
        await settle()
        let passwordGeometry = geometry()
        checks["secure password keyboard does not lift navigation"] = covered
        checks["focused password is visible above keyboard"] = focusedFieldIsVisible()
        restorePassword = true
        revealed = true
        await settle()
        let revealGeometry = geometry()
        checks["password reveal retains focus and keyboard"] = focus == .password && covered
        checks["revealed password remains visible"] = focusedFieldIsVisible()
        focus = nil
        await settle()
        checks["navigation returns to original bottom after dismissal"] = !covered && abs(navigation.maxY - baseline.maxY) < 2
        checks["typing and keyboard changes preserve draft values"] = email == "qa@example.invalid" && password == "LocalOnly123!"
        let result: [String: Any] = ["passed": checks.values.allSatisfy { $0 }, "checks": checks,
            "email": emailGeometry, "password": passwordGeometry, "revealed": revealGeometry,
            "width": activeWindow?.bounds.width ?? 0, "height": activeWindow?.bounds.height ?? 0]
        let directory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        if let data = try? JSONSerialization.data(withJSONObject: result, options: [.prettyPrinted, .sortedKeys]) {
            try? data.write(to: directory.appendingPathComponent("keyboard-results.json"), options: .atomic)
        }
    }
}
