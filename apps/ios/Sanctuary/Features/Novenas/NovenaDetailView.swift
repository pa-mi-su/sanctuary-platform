import Foundation
import SwiftUI

struct NovenaDetailView: View {
    let contentRepository: any ContentRepository
    let novena: Novena
    var displayYear: Int? = nil
    var allowsRelatedNavigation: Bool = true
    var onClose: (() -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var localization: LocalizationManager
    @EnvironmentObject private var progressStore: UserProgressStore

    @State private var selectedDay = 1
    @State private var isFavorite = false
    @State private var hydratedNovena: Novena?
    @State private var showCompletionModal = false
    @State private var servingWindow: NovenaServingWindowInfo?

    private var locale: ContentLocale { localization.language.contentLocale }
    private var effectiveNovena: Novena { hydratedNovena ?? novena }

    private var title: String {
        effectiveNovena.titleByLocale[locale] ?? effectiveNovena.titleByLocale[.en] ?? effectiveNovena.slug
    }

    private var description: String {
        effectiveNovena.descriptionByLocale[locale] ?? effectiveNovena.descriptionByLocale[.en] ?? ""
    }

    private var orderedDays: [NovenaDay] {
        effectiveNovena.days.sorted { $0.dayNumber < $1.dayNumber }
    }

    private var selectedDayContent: String {
        guard let day = orderedDays.first(where: { $0.dayNumber == selectedDay }) else {
            return ""
        }
        return day.bodyByLocale[locale] ?? day.bodyByLocale[.en] ?? ""
    }

    private var selectedDayTitle: String {
        guard let day = orderedDays.first(where: { $0.dayNumber == selectedDay }) else { return "" }
        return day.titleByLocale[locale] ?? day.titleByLocale[.en] ?? ""
    }

    private var selectedDayScripture: String {
        guard let day = orderedDays.first(where: { $0.dayNumber == selectedDay }) else { return "" }
        return day.scriptureByLocale[locale] ?? day.scriptureByLocale[.en] ?? ""
    }

    private var selectedDayPrayer: String {
        guard let day = orderedDays.first(where: { $0.dayNumber == selectedDay }) else { return "" }
        return day.prayerByLocale[locale] ?? day.prayerByLocale[.en] ?? ""
    }

    private var selectedDayReflection: String {
        guard let day = orderedDays.first(where: { $0.dayNumber == selectedDay }) else { return "" }
        return day.reflectionByLocale[locale] ?? day.reflectionByLocale[.en] ?? ""
    }

    private var novenaStartDateString: String? {
        servingWindow.map { localization.formatMonthDay($0.startDate) }
    }

    private var novenaEndDateString: String? {
        servingWindow.map { localization.formatMonthDay($0.feastDate) }
    }

    private var currentCommitment: UserNovenaCommitment? {
        progressStore.activeCommitment(for: effectiveNovena.id)
    }

    private var latestCommitment: UserNovenaCommitment? {
        progressStore.commitments
            .filter { $0.novenaID == effectiveNovena.id }
            .sorted { $0.updatedAt > $1.updatedAt }
            .first
    }

    private var completionButtonTitle: String {
        if let active = currentCommitment {
            return "\(localization.t("novena.completeDay")) \(active.currentDay)"
        }
        if latestCommitment?.status == .completed {
            return localization.t("novena.completed")
        }
        return "\(localization.t("novena.completeDay")) 1"
    }

    private var canStartNovena: Bool {
        progressStore.isAuthenticated && currentCommitment == nil && latestCommitment?.status != .completed
    }

    private var hasActiveNovena: Bool {
        currentCommitment != nil
    }

    private func handleBack() {
        dismiss()
        onClose?()
    }

    var body: some View {
        ZStack {
            AppBackdrop()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 14) {
                    HStack(spacing: 16) {
                        Button {
                            handleBack()
                        } label: {
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
                        .highPriorityGesture(TapGesture().onEnded { handleBack() })
                        Text(title)
                            .font(.system(size: 20, weight: .bold))
                            .foregroundStyle(.white)
                            .lineLimit(2)
                    }
                    .padding(.top, 8)
                    .zIndex(10)

                    if let imageURL = effectiveNovena.imageURL {
                        RemoteHeroImage(url: imageURL)
                    }

                    VStack(alignment: .leading, spacing: 14) {
                        Text(title)
                            .font(AppTheme.rounded(48, weight: .bold))
                            .minimumScaleFactor(0.58)
                            .foregroundStyle(.white)

                        HStack(spacing: 10) {
                            Button {
                                Task {
                                    await progressStore.toggleFavorite(itemType: .novena, itemID: effectiveNovena.id)
                                    isFavorite = progressStore.isFavorite(itemType: .novena, itemID: effectiveNovena.id)
                                }
                            } label: {
                                HStack(spacing: 10) {
                                    Image(systemName: isFavorite ? "heart.fill" : "heart")
                                    Text(isFavorite ? localization.t("detail.savedFavorites") : localization.t("detail.addFavorites"))
                                }
                                .font(AppTheme.rounded(16, weight: .semibold))
                                .foregroundStyle(.white)
                                .padding(.horizontal, 18)
                                .padding(.vertical, 12)
                                .background(isFavorite ? AnyShapeStyle(AppTheme.primaryButtonGradient) : AnyShapeStyle(AppTheme.cardBackgroundSoft))
                                .overlay(
                                    Capsule()
                                        .stroke(Color.white.opacity(0.12), lineWidth: 1)
                                )
                                .clipShape(Capsule())
                            }
                            .buttonStyle(.plain)
                            .animation(.spring(response: 0.32, dampingFraction: 0.82), value: isFavorite)

                            Spacer()
                        }

                        VStack(alignment: .leading, spacing: 10) {
                            if let novenaStartDateString {
                                detailMetaChip(icon: "calendar.badge.clock", text: "\(localization.t("detail.novenaStartDate")): \(novenaStartDateString)")
                            }

                            if let novenaEndDateString {
                                detailMetaChip(icon: "calendar", text: "\(localization.t("detail.novenaEndDate")): \(novenaEndDateString)")
                            }

                            if !description.isEmpty {
                                Text(description)
                                    .font(AppTheme.rounded(18, weight: .medium))
                                    .foregroundStyle(.white.opacity(0.95))
                                    .padding(.top, 2)
                            }
                        }
                    }
                    .padding(20)
                    .appGlassCard(cornerRadius: 28)

                    Text(localization.t("novena.chooseDay"))
                        .font(AppTheme.rounded(40, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.top, 4)

                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 95), spacing: 10)], spacing: 10) {
                        ForEach(orderedDays, id: \.dayNumber) { day in
                            let active = day.dayNumber == selectedDay
                            Button {
                                selectedDay = day.dayNumber
                            } label: {
                                VStack(spacing: 4) {
                                    Text("\(localization.t("novena.dayLabel"))")
                                        .font(AppTheme.rounded(12, weight: .medium))
                                        .foregroundStyle(.white.opacity(0.72))
                                    Text("\(day.dayNumber)")
                                        .font(AppTheme.rounded(22, weight: .bold))
                                        .foregroundStyle(.white)
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(active ? AnyShapeStyle(AppTheme.primaryButtonGradient) : AnyShapeStyle(AppTheme.cardBackgroundSoft))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                                        .stroke(Color.white.opacity(active ? 0.2 : 0.1), lineWidth: 1)
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                            }
                            .buttonStyle(.plain)
                            .scaleEffect(active ? 1 : 0.985)
                            .animation(.spring(response: 0.28, dampingFraction: 0.84), value: selectedDay)
                        }
                    }
                    .padding(14)
                    .appGlassCard(cornerRadius: 28)

                    if !progressStore.isAuthenticated {
                        Text(localization.t("novena.loginPrompt"))
                            .font(AppTheme.rounded(17, weight: .semibold))
                            .foregroundStyle(.white.opacity(0.88))
                            .padding(.horizontal, 18)
                            .padding(.vertical, 14)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(AppTheme.cardBackgroundSoft)
                            .overlay(
                                RoundedRectangle(cornerRadius: 18, style: .continuous)
                                    .stroke(Color.white.opacity(0.1), lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    } else if hasActiveNovena {
                        Button(localization.t("novena.stop")) {
                            Task {
                                await progressStore.stopNovena(novenaID: effectiveNovena.id)
                                selectedDay = 1
                            }
                        }
                        .buttonStyle(SecondaryPillButtonStyle())
                        .padding(.top, 4)
                    } else if canStartNovena {
                        Button(localization.t("novena.start")) {
                            Task {
                                await progressStore.startNovena(novenaID: effectiveNovena.id)
                                selectedDay = 1
                            }
                        }
                        .buttonStyle(PrimaryPillButtonStyle())
                        .padding(.top, 4)
                    }

                    Divider()
                        .background(Color.white.opacity(0.35))
                        .padding(.top, 6)

                    DetailCard(title: "\(localization.t("novena.dayLabel")) \(selectedDay)") {
                        if selectedDayContent.isEmpty && selectedDayScripture.isEmpty && selectedDayPrayer.isEmpty && selectedDayReflection.isEmpty {
                            Text(localization.t("novena.noDayContent"))
                                .font(.system(size: 17, weight: .medium))
                                .foregroundStyle(AppTheme.cardText.opacity(0.84))
                        } else {
                            VStack(alignment: .leading, spacing: 12) {
                                if !selectedDayTitle.isEmpty {
                                    Text(selectedDayTitle)
                                        .font(AppTheme.rounded(21, weight: .bold))
                                        .foregroundStyle(AppTheme.cardText)
                                }
                                if !selectedDayScripture.isEmpty {
                                    Text(localization.t("novena.scripture"))
                                        .font(AppTheme.rounded(17, weight: .bold))
                                        .foregroundStyle(AppTheme.cardText)
                                    Text(selectedDayScripture)
                                        .font(AppTheme.rounded(17, weight: .medium))
                                        .foregroundStyle(AppTheme.cardText.opacity(0.9))
                                }
                                if !selectedDayPrayer.isEmpty {
                                    Text(localization.t("novena.prayer"))
                                        .font(AppTheme.rounded(17, weight: .bold))
                                        .foregroundStyle(AppTheme.cardText)
                                    Text(selectedDayPrayer)
                                        .font(AppTheme.rounded(17, weight: .medium))
                                        .foregroundStyle(AppTheme.cardText.opacity(0.9))
                                }
                                if !selectedDayReflection.isEmpty {
                                    Text(localization.t("novena.reflection"))
                                        .font(AppTheme.rounded(17, weight: .bold))
                                        .foregroundStyle(AppTheme.cardText)
                                    Text(selectedDayReflection)
                                        .font(AppTheme.rounded(17, weight: .medium))
                                        .foregroundStyle(AppTheme.cardText.opacity(0.9))
                                }
                                if selectedDayScripture.isEmpty && selectedDayPrayer.isEmpty && selectedDayReflection.isEmpty && !selectedDayContent.isEmpty {
                                    Text(selectedDayContent)
                                        .font(AppTheme.rounded(17, weight: .medium))
                                        .foregroundStyle(AppTheme.cardText.opacity(0.9))
                                }
                            }
                        }
                    }

                    if progressStore.isAuthenticated && (currentCommitment != nil || latestCommitment?.status == .completed) {
                        Button(completionButtonTitle) {
                            Task {
                                guard let active = currentCommitment else { return }
                                let total = max(1, effectiveNovena.durationDays)
                                let justCompletedFinalDay = active.currentDay >= total
                                await progressStore.completeCurrentDay(
                                    novenaID: effectiveNovena.id,
                                    totalDays: total
                                )
                                if justCompletedFinalDay {
                                    selectedDay = total
                                    showCompletionModal = true
                                } else if let next = progressStore.activeCommitment(for: effectiveNovena.id)?.currentDay {
                                    selectedDay = min(max(1, next), total)
                                } else {
                                    selectedDay = total
                                }
                            }
                        }
                        .buttonStyle(PrimaryPillButtonStyle())
                        .disabled(latestCommitment?.status == .completed)
                        .padding(.top, 4)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 26)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            isFavorite = progressStore.isFavorite(itemType: .novena, itemID: novena.id)
            if let day = currentCommitment?.currentDay {
                selectedDay = min(max(1, day), max(1, effectiveNovena.durationDays))
            }
            if selectedDay < 1 {
                selectedDay = orderedDays.first?.dayNumber ?? 1
            }
        }
        .task {
            let id = novena.id
            async let loadedNovena = contentRepository.fetchNovena(slug: id, locale: locale)
            async let loadedWindow = contentRepository.fetchNovenaServingWindow(
                novenaID: id,
                year: displayYear ?? Calendar.current.component(.year, from: Date())
            )

            if let loadedNovena = try? await loadedNovena {
                hydratedNovena = loadedNovena
            }
            servingWindow = try? await loadedWindow
        }
        .alert(localization.t("novena.completedTitle"), isPresented: $showCompletionModal) {
            Button(localization.t("common.done")) {
                selectedDay = 1
                Task(priority: .userInitiated) {
                    await progressStore.stopNovena(novenaID: effectiveNovena.id)
                }
            }
        } message: {
            Text("\(localization.t("novena.completedMessagePrefix")) \(title) \(localization.t("novena.completedMessageSuffix"))")
        }
    }

    private func detailMetaChip(icon: String, text: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(AppTheme.glowGold)
            Text(text)
                .font(AppTheme.rounded(15, weight: .medium))
                .foregroundStyle(.white.opacity(0.9))
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(AppTheme.cardBackgroundSoft)
        .overlay(
            Capsule()
                .stroke(Color.white.opacity(0.1), lineWidth: 1)
        )
        .clipShape(Capsule())
    }
}

private struct DetailCard<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.system(size: 22, weight: .heavy))
                .foregroundStyle(AppTheme.cardText)

            Divider().background(AppTheme.cardText.opacity(0.2))

            content
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppTheme.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

private struct RemoteHeroImage: View {
    let url: URL

    var body: some View {
        GeometryReader { proxy in
            let containerSize = proxy.size
            let outerShape = RoundedRectangle(cornerRadius: 20, style: .continuous)
            let innerShape = RoundedRectangle(cornerRadius: 14, style: .continuous)

            ZStack {
                outerShape
                    .fill(Color.white.opacity(0.12))

                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty:
                        ProgressView().tint(.white)
                    case .success(let image):
                        ZStack {
                            image
                                .resizable()
                                .scaledToFill()
                                .frame(width: containerSize.width, height: containerSize.height)
                                .clipped()
                                .blur(radius: 22)
                                .saturation(0.7)
                                .opacity(0.82)

                            image
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(
                                    width: max(containerSize.width - 20, 0),
                                    height: max(containerSize.height - 20, 0)
                                )
                                .frame(width: max(containerSize.width - 20, 0), height: max(containerSize.height - 20, 0))
                                .clipped()
                                .clipShape(innerShape)
                                .overlay(
                                    innerShape
                                        .stroke(Color.white.opacity(0.34), lineWidth: 1)
                                )
                        }
                        .frame(width: containerSize.width, height: containerSize.height)
                        .clipped()
                    case .failure:
                        Color.gray.opacity(0.25)
                    @unknown default:
                        Color.gray.opacity(0.25)
                    }
                }
                .frame(width: containerSize.width, height: containerSize.height)
            }
            .frame(width: containerSize.width, height: containerSize.height)
            .clipShape(outerShape)
            .overlay(
                outerShape
                    .stroke(Color.white.opacity(0.24), lineWidth: 1.5)
            )
        }
        .frame(maxWidth: .infinity)
        .frame(height: 260)
        .clipped()
        .allowsHitTesting(false)
    }
}

struct NovenaDetailView_Previews: PreviewProvider {
    private static let previewNovena = Novena(
        id: "st-joseph-novena",
        slug: "st-joseph-novena",
        titleByLocale: [.en: "Saint Joseph Novena"],
        descriptionByLocale: [.en: "A simple novena asking Saint Joseph to guide families, work, and discernment."],
        durationDays: 9,
        tags: ["family", "work"],
        imageURL: nil,
        days: [
            NovenaDay(
                dayNumber: 1,
                titleByLocale: [.en: "Day One"],
                scriptureByLocale: [.en: "Matthew 1:20-21"],
                prayerByLocale: [.en: "Saint Joseph, guardian of the Redeemer, pray for us."],
                reflectionByLocale: [.en: "Ask for Joseph's quiet trust."],
                bodyByLocale: [.en: "Matthew 1:20-21\n\nSaint Joseph, guardian of the Redeemer, pray for us.\n\nAsk for Joseph's quiet trust."]
            )
        ]
    )

    static var previews: some View {
        NovenaDetailView(
            contentRepository: PreviewContentRepository(novenas: [previewNovena]),
            novena: previewNovena
        )
            .environmentObject(LocalizationManager())
            .environmentObject(
                UserProgressStore(userProgressRepository: LocalUserProgressRepository())
            )
    }
}
