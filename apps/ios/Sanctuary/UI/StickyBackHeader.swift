import SwiftUI

struct FloatingBackButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "arrow.left")
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(AppTheme.cardText)
                .frame(width: 52, height: 52)
                .background(AppTheme.cardBackgroundSoft)
                .overlay(Circle().stroke(AppTheme.cardText.opacity(0.12), lineWidth: 1))
                .clipShape(Circle())
                .shadow(color: .black.opacity(0.08), radius: 8, x: 0, y: 4)
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
}
