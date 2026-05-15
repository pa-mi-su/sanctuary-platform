import SwiftUI

struct StickyBackHeader: View {
    let title: String
    let action: () -> Void

    var body: some View {
        HStack(spacing: 14) {
            Button(action: action) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 52, height: 52)
                    .background(AppTheme.cardBackgroundSoft)
                    .overlay(Circle().stroke(Color.white.opacity(0.12), lineWidth: 1))
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .contentShape(Circle())

            Text(title)
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(.white)
                .lineLimit(2)

            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .padding(.bottom, 10)
        .background(.ultraThinMaterial.opacity(0.75))
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
