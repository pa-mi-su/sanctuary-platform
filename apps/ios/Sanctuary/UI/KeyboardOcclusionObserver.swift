import SwiftUI
import UIKit

/// Reports when this window's bottom edge is covered by a docked software keyboard.
/// Floating keyboards and hardware keyboards leave bottom navigation available.
struct KeyboardOcclusionObserver: UIViewRepresentable {
    @Binding var coversBottom: Bool

    func makeUIView(context: Context) -> KeyboardOcclusionView {
        KeyboardOcclusionView { covered in
            // Keyboard notifications can arrive during a SwiftUI layout update.
            DispatchQueue.main.async { coversBottom = covered }
        }
    }

    func updateUIView(_ uiView: KeyboardOcclusionView, context: Context) {}
}

final class KeyboardOcclusionView: UIView {
    private let onChange: (Bool) -> Void
    private var reported = false

    init(onChange: @escaping (Bool) -> Void) {
        self.onChange = onChange
        super.init(frame: .zero)
        isUserInteractionEnabled = false
        accessibilityElementsHidden = true
        NotificationCenter.default.addObserver(self, selector: #selector(frameWillChange(_:)),
            name: UIResponder.keyboardWillChangeFrameNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillHide),
            name: UIResponder.keyboardWillHideNotification, object: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }
    deinit { NotificationCenter.default.removeObserver(self) }

    @objc private func frameWillChange(_ notification: Notification) {
        guard let window,
              notification.userInfo?[UIResponder.keyboardIsLocalUserInfoKey] as? Bool != false,
              let frame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect else { return }
        let localFrame = window.screen.coordinateSpace.convert(frame, to: window)
        report(KeyboardNavigationPolicy.coversBottom(windowBounds: window.bounds,
            safeAreaBottom: window.safeAreaInsets.bottom, keyboardFrame: localFrame))
    }

    @objc private func keyboardWillHide() { report(false) }

    private func report(_ covered: Bool) {
        guard covered != reported else { return }
        reported = covered
        onChange(covered)
    }
}
