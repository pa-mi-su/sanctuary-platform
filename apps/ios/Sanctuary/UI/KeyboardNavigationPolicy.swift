import Foundation
import CoreGraphics

enum KeyboardNavigationPolicy {
    static func coversBottom(windowBounds: CGRect, safeAreaBottom: CGFloat, keyboardFrame: CGRect) -> Bool {
        let overlap = windowBounds.intersection(keyboardFrame)
        return !overlap.isNull && overlap.height > safeAreaBottom
            && keyboardFrame.maxY >= windowBounds.maxY
    }
}
