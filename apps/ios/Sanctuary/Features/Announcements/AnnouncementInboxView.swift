import Combine
import SwiftUI

extension Notification.Name {
  static let announcementInboxChanged = Notification.Name("SanctuaryAnnouncementInboxChanged")
}

struct SanctuaryAnnouncement: Codable, Identifiable {
  let id: String
  let title: String
  let body: String
  let destination: String
  let published_at: Double
  var contentURL: URL? {
    guard let url = URL(string: destination), url.scheme == "https",
      url.host == "mydailysanctuary.com"
        || (PlatformEnvironment.current() == .dev && url.host == "dev.mydailysanctuary.com"),
      url.pathComponents.count == 3,
      ["prayers", "saints", "novenas"].contains(url.pathComponents[1])
    else { return nil }
    return url
  }
}
struct AnnouncementFeed: Decodable {
  let items: [SanctuaryAnnouncement]
  let hasMore: Bool
}
struct AnnouncementChange: Encodable {
  let action: String
  var ids: [String] = []
  var throughId: String? = nil
}
@MainActor final class AnnouncementInboxStore: ObservableObject {
  @Published var items: [SanctuaryAnnouncement] = []
  @Published var state = AnnouncementInboxState()
  @Published var busy = false
  @Published var error: String?
  @Published var more = false
  @Published var undoID: String?
  private var page = 0
  private var api: SanctuaryAPIClient?
  private var token: String?
  private var generation = 0
  private var key: String {
    "announcement-guest-" + String(describing: PlatformEnvironment.current())
  }
  private var receiptCache: AnnouncementReceiptCache { .init(key: key) }
  var visible: [SanctuaryAnnouncement] {
    items.filter { !state.clearedIds.contains($0.id) && $0.published_at > state.clearedBefore }
  }
  var unread: Bool { visible.contains { !state.readIds.contains($0.id) } }
  func load(api: SanctuaryAPIClient, token: String?, more: Bool = false) async {
    if busy && self.token == token { return }
    busy = true
    error = nil
    generation += 1
    let request = generation
    let receiptRevision = receiptCache.revision
    self.api = api
    self.token = token
    defer { if request == generation { busy = false } }
    do {
      if !more {
        page = 0
        items = []
        undoID = nil
        if let token {
          // Claim guest receipts for this identity before I/O; another account cannot inherit a failed transfer.
          let parts = token.split(separator: ".")
          var payload = parts.count > 1 ? String(parts[1]).replacingOccurrences(of: "-", with: "+").replacingOccurrences(of: "_", with: "/") : ""
          payload += String(repeating: "=", count: (4 - payload.count % 4) % 4)
          let subject = Data(base64Encoded: payload).flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] }?["sub"] as? String
          guard let subject else { throw URLError(.userAuthenticationRequired) }
          let pending = key + "-pending-" + subject
          if let fresh = UserDefaults.standard.data(forKey: key), let guest = try? JSONDecoder().decode(AnnouncementInboxState.self, from: fresh) {
            var combined = UserDefaults.standard.data(forKey: pending).flatMap { try? JSONDecoder().decode(AnnouncementInboxState.self, from: $0) } ?? AnnouncementInboxState()
            combined.readIds = Array(Set(combined.readIds + guest.readIds))
            combined.clearedIds = Array(Set(combined.clearedIds + guest.clearedIds))
            combined.clearedBefore = max(combined.clearedBefore, guest.clearedBefore)
            UserDefaults.standard.set(try JSONEncoder().encode(combined), forKey: pending)
            UserDefaults.standard.removeObject(forKey: key)
          }
          let guest = UserDefaults.standard.data(forKey: pending).flatMap { try? JSONDecoder().decode(AnnouncementInboxState.self, from: $0) }
          let loaded: AnnouncementInboxState
          if let guest, !guest.readIds.isEmpty || !guest.clearedIds.isEmpty || guest.clearedBefore > 0 {
            loaded = try await api.mergeGuestAnnouncements(guest, token: token)
            guard request == generation else { return }
            UserDefaults.standard.removeObject(forKey: pending)
          } else { loaded = try await api.announcementState(token: token) }
          guard request == generation else { return }
          state = loaded
          try receiptCache.rememberMember(loaded, expectedRevision: receiptRevision)
        } else {
          state = receiptCache.guestForDisplay
        }
      }
      let next = more ? page + 1 : 0
      let feed = try await api.announcements(page: next)
      guard request == generation else { return }
      items =
        more ? items + feed.items.filter { row in !items.contains { $0.id == row.id } } : feed.items
      page = next
      self.more = feed.hasMore
    } catch { if request == generation { self.error = error.localizedDescription } }
  }
  func change(
    _ action: String, id: String? = nil, through: SanctuaryAnnouncement? = nil, ids: [String]? = nil
  ) async {
    guard !busy, let api else { return }
    let request = generation
    let receiptRevision = receiptCache.revision
    busy = true
    error = nil
    defer { if request == generation { busy = false } }
    do {
      let change = AnnouncementChange(
        action: action, ids: ids ?? id.map { [$0] } ?? [],
        throughId: action == "clearAll"
          ? (through ?? items.max(by: { $0.published_at < $1.published_at }))?.id : nil)
      if let token {
        let loaded = try await api.changeAnnouncements(change, token: token)
        guard request == generation else { return }
        state = loaded
        try receiptCache.rememberMember(loaded, expectedRevision: action == "read" ? receiptRevision : nil)
      } else {
        state = try receiptCache.changeGuest(action, ids: change.ids,
          through: (through ?? items.max(by: { $0.published_at < $1.published_at }))?.published_at ?? 0)
      }
      if action == "clear" || action == "clearAll" {
        await PushNotifications.shared.acknowledgeAnnouncementAlerts()
      }
      undoID = action == "clear" ? id : nil
      NotificationCenter.default.post(name: .announcementInboxChanged, object: nil)
    } catch { if request == generation { self.error = error.localizedDescription } }
  }
}
struct AnnouncementInboxView: View {
  let environment: AppEnvironment
  @ObservedObject var store: AnnouncementInboxStore
  var initialID: String? = nil
  let open: (URL) -> Void
  @EnvironmentObject private var account: AccountSessionStore
  @EnvironmentObject private var localization: LocalizationManager
  @Environment(\.dismiss) private var dismiss
  @State private var confirmClear = false
  @State private var clearThrough: SanctuaryAnnouncement?
  @State private var forcedID: String?
  private var displayed: [SanctuaryAnnouncement] {
    let visible = store.visible
    guard let forcedID, !visible.contains(where: { $0.id == forcedID }),
      let row = store.items.first(where: { $0.id == forcedID })
    else { return visible }
    return [row] + visible
  }
  private func refresh(more: Bool = false) async {
    await store.load(
      api: environment.apiClient, token: await account.authorizationToken(), more: more)
    await markDisplayedRead()
    if store.error == nil { await PushNotifications.shared.acknowledgeAnnouncementAlerts() }
  }
  private func markDisplayedRead() async {
    let unread = displayed.filter { !store.state.readIds.contains($0.id) }.map(\.id)
    if !unread.isEmpty { await store.change("read", ids: Array(unread.prefix(100))) }
  }
  var body: some View {
    ZStack(alignment: .topLeading) {
      AppBackdrop()
      ScrollView {
        VStack(alignment: .leading, spacing: 18) {
          Text(localization.t("inbox.title"))
            .font(AppTheme.rounded(30, weight: .bold)).foregroundStyle(.white)
            .frame(maxWidth: .infinity, alignment: .center)
          HStack(spacing: 12) {
            Button(localization.t("inbox.refresh")) { Task { await refresh() } }
              .buttonStyle(SecondaryPillButtonStyle())
            Button(localization.t("inbox.clearAll")) {
              clearThrough = store.items.max(by: { $0.published_at < $1.published_at })
              confirmClear = true
            }.buttonStyle(SecondaryPillButtonStyle()).disabled(store.visible.isEmpty || store.busy)
          }
          if let error = store.error { Text(error).foregroundStyle(.red) }
          if store.busy { ProgressView().frame(maxWidth: .infinity) }
          if let id = store.undoID {
            Button(localization.t("inbox.undo")) { Task { await store.change("restore", id: id) } }
              .buttonStyle(SecondaryPillButtonStyle())
          }
          if displayed.isEmpty && !store.busy {
            Text(localization.t("inbox.empty")).foregroundStyle(AppTheme.subtitleText)
              .padding(20).frame(maxWidth: .infinity).appGlassCard(cornerRadius: 24)
          }
          ForEach(displayed) { row in
            VStack(alignment: .leading, spacing: 14) {
              Text(row.title).font(AppTheme.rounded(20, weight: .bold)).foregroundStyle(.white)
                .fixedSize(horizontal: false, vertical: true)
              Text(Date(timeIntervalSince1970: row.published_at / 1000), style: .date)
                .font(AppTheme.rounded(14, weight: .medium)).foregroundStyle(AppTheme.subtitleText)
              Text(row.body).font(AppTheme.rounded(17, weight: .medium))
                .foregroundStyle(AppTheme.cardText).fixedSize(horizontal: false, vertical: true)
              Divider().overlay(AppTheme.purpleOutline.opacity(0.4))
              HStack(spacing: 12) {
                if let url = row.contentURL {
                  Button(localization.t("inbox.open")) { open(url) }.buttonStyle(
                    PrimaryPillButtonStyle())
                }
                Button(localization.t("inbox.clear")) {
                  forcedID = nil
                  Task { await store.change("clear", id: row.id) }
                }.buttonStyle(SecondaryPillButtonStyle()).disabled(store.busy)
              }
            }
            .padding(20).frame(maxWidth: .infinity, alignment: .leading)
            .background(AppTheme.cardBackground).clipShape(RoundedRectangle(cornerRadius: 24))
            .overlay(
              RoundedRectangle(cornerRadius: 24).stroke(
                AppTheme.purpleOutline.opacity(0.6), lineWidth: 1))
          }
          if store.more {
            Button(localization.t("inbox.more")) { Task { await refresh(more: true) } }
              .buttonStyle(SecondaryPillButtonStyle()).disabled(store.busy)
          }
        }.frame(maxWidth: 680).padding(.horizontal, 18).padding(.top, 76).padding(.bottom, 48)
          .frame(maxWidth: .infinity)
      }
      Button {
        dismiss()
      } label: {
        Image(systemName: "arrow.left").font(.system(size: 20, weight: .medium))
          .foregroundStyle(.white).frame(width: 42, height: 42)
          .background(AppTheme.cardBackground).clipShape(Circle())
          .overlay(Circle().stroke(AppTheme.tabBorder, lineWidth: 1))
      }.accessibilityLabel(localization.t("common.close")).padding(.leading, 18).padding(.top, 12)
    }.task {
      await store.load(api: environment.apiClient, token: await account.authorizationToken())
      if let id = initialID, UUID(uuidString: id) != nil {
        do {
          if !store.items.contains(where: { $0.id == id }) {
            store.items.insert(try await environment.apiClient.announcement(id: id), at: 0)
          }
          forcedID = id
          await store.change("read", id: id)
        } catch { store.error = error.localizedDescription }
      }
      await markDisplayedRead()
      if store.error == nil { await PushNotifications.shared.acknowledgeAnnouncementAlerts() }
    }
    .alert(localization.t("inbox.confirmClear"), isPresented: $confirmClear) {
      Button(localization.t("inbox.clearAll"), role: .destructive) {
        forcedID = nil
        Task { await store.change("clearAll", through: clearThrough) }
      }
      Button(localization.t("common.cancel"), role: .cancel) {}
    }
  }
}
