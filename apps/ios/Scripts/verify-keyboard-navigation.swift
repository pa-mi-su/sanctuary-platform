import Foundation

@main
struct VerifyKeyboardNavigation {
    static func main() {
        let phone = CGRect(x: 0, y: 0, width: 440, height: 956)
        let cases: [(String, CGRect, CGFloat, CGRect, Bool)] = [
            ("docked phone keyboard", phone, 34, CGRect(x: 0, y: 600, width: 440, height: 356), true),
            ("dismissed keyboard", phone, 34, CGRect(x: 0, y: 956, width: 440, height: 356), false),
            ("home indicator only", phone, 34, CGRect(x: 0, y: 922, width: 440, height: 34), false),
            ("hardware keyboard", phone, 34, CGRect(x: 0, y: 956, width: 440, height: 0), false),
            ("floating tablet keyboard", CGRect(x: 0, y: 0, width: 1024, height: 1366), 20,
                CGRect(x: 500, y: 700, width: 400, height: 300), false),
            ("landscape keyboard", CGRect(x: 0, y: 0, width: 956, height: 440), 21,
                CGRect(x: 0, y: 220, width: 956, height: 220), true),
            ("keyboard outside this window", phone, 34, CGRect(x: 500, y: 600, width: 440, height: 356), false),
            ("small phone without home inset", CGRect(x: 0, y: 0, width: 375, height: 667), 0,
                CGRect(x: 0, y: 410, width: 375, height: 257), true)
        ]
        for (name, bounds, inset, keyboard, expected) in cases {
            precondition(KeyboardNavigationPolicy.coversBottom(windowBounds: bounds,
                safeAreaBottom: inset, keyboardFrame: keyboard) == expected, name)
        }
        print("Keyboard navigation policy passed \(cases.count) docked, floating, hardware and window-boundary cases.")
    }
}
