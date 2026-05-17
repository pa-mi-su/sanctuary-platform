import SwiftUI

struct FloatingBackButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "arrow.left")
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 52, height: 52)
                .background(AppTheme.cardBackgroundSoft)
                .overlay(Circle().stroke(Color.white.opacity(0.12), lineWidth: 1))
                .clipShape(Circle())
                .shadow(color: .black.opacity(0.22), radius: 14, x: 0, y: 8)
        }
        .buttonStyle(.plain)
        .contentShape(Circle())
    }
}

extension View {
    func leftEdgeSwipeBack(_ action: @escaping () -> Void) -> some View {
        simultaneousGesture(
            DragGesture(minimumDistance: 22, coordinateSpace: .local)
                .onEnded { value in
                    guard value.startLocation.x <= 32,
                          value.translation.width >= 80,
                          abs(value.translation.height) < 70 else {
                        return
                    }
                    action()
                }
        )
    }

    func swipeDownToDismiss(_ action: @escaping () -> Void) -> some View {
        simultaneousGesture(
            DragGesture(minimumDistance: 28, coordinateSpace: .local)
                .onEnded { value in
                    guard value.translation.height >= 120,
                          abs(value.translation.width) < 90 else {
                        return
                    }
                    action()
                }
        )
    }
}
