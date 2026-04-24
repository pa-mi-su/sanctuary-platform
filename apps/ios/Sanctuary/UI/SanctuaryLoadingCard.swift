import SwiftUI

struct SanctuaryLoadingCard: View {
    let title: String
    let detail: String?
    @State private var animatePulse = false

    var body: some View {
        VStack(spacing: 14) {
            ZStack {
                Circle()
                    .fill(AppTheme.glowBlue.opacity(0.18))
                    .frame(width: 78, height: 78)
                    .blur(radius: 10)

                Circle()
                    .stroke(Color.white.opacity(0.1), lineWidth: 1)
                    .frame(width: 72, height: 72)

                Circle()
                    .fill(AppTheme.glowGold.opacity(0.16))
                    .frame(width: animatePulse ? 70 : 46, height: animatePulse ? 70 : 46)
                    .blur(radius: animatePulse ? 12 : 6)
                    .scaleEffect(animatePulse ? 1.04 : 0.92)

                SanctuaryCrossShape()
                    .fill(
                        LinearGradient(
                            colors: [AppTheme.glowGold, Color.white.opacity(0.92)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .frame(width: 26, height: 40)
                    .shadow(color: AppTheme.glowGold.opacity(0.35), radius: 10, x: 0, y: 0)
                    .scaleEffect(animatePulse ? 1.04 : 0.96)
            }
            .frame(width: 72, height: 72)
            .onAppear {
                withAnimation(.easeInOut(duration: 1.1).repeatForever(autoreverses: true)) {
                    animatePulse = true
                }
            }

            VStack(spacing: 6) {
                Text(title)
                    .font(AppTheme.rounded(18, weight: .bold))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)

                if let detail, !detail.isEmpty {
                    Text(detail)
                        .font(AppTheme.rounded(15, weight: .medium))
                        .foregroundStyle(AppTheme.subtitleText)
                        .multilineTextAlignment(.center)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 18)
        .padding(.vertical, 22)
        .appGlassCard(cornerRadius: 24)
    }
}

struct SanctuaryCrossShape: Shape {
    func path(in rect: CGRect) -> Path {
        let verticalWidth = rect.width * 0.3
        let horizontalWidth = rect.width * 0.82
        let horizontalHeight = rect.height * 0.18
        let horizontalY = rect.height * 0.28

        var path = Path()
        path.addRoundedRect(
            in: CGRect(
                x: (rect.width - verticalWidth) / 2,
                y: 0,
                width: verticalWidth,
                height: rect.height
            ),
            cornerSize: CGSize(width: verticalWidth / 2, height: verticalWidth / 2)
        )
        path.addRoundedRect(
            in: CGRect(
                x: (rect.width - horizontalWidth) / 2,
                y: horizontalY,
                width: horizontalWidth,
                height: horizontalHeight
            ),
            cornerSize: CGSize(width: horizontalHeight / 2, height: horizontalHeight / 2)
        )

        return path
    }
}
