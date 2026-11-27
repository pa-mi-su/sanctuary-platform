import SwiftUI
import UIKit

struct FocusedInputPreferenceKey: PreferenceKey {
    static var defaultValue: String? = nil
    static func reduce(value: inout String?, nextValue: () -> String?) {
        if let focused = nextValue() { value = focused }
    }
}

/// Preserves system keyboard avoidance and reveals the focused form row after
/// keyboard layout settles, including fields that began below the fold.
struct KeyboardAwareScrollView<Content: View>: View {
    @ViewBuilder let content: Content
    @State private var focusedInput: String?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView(showsIndicators: false) { content }
                .scrollDismissesKeyboard(.interactively)
                .onPreferenceChange(FocusedInputPreferenceKey.self) { id in
                    focusedInput = id
                    reveal(id, using: proxy)
                }
                .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardDidShowNotification)) { _ in
                    reveal(focusedInput, using: proxy)
                }
        }
    }

    private func reveal(_ id: String?, using proxy: ScrollViewProxy) {
        guard let id else { return }
        DispatchQueue.main.async {
            withAnimation(reduceMotion ? nil : .easeOut(duration: 0.2)) {
                proxy.scrollTo(id, anchor: .center)
            }
        }
    }
}
