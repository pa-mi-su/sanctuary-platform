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
}
struct AnnouncementFeed: Decodable {
  let items: [SanctuaryAnnouncement]
  let hasMore: Bool
}
struct AnnouncementInboxState: Codable {
  var readIds: [String] = []
  var clearedIds: [String] = []
  var clearedBefore: Double = 0
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
    self.api = api
    self.token = token
    defer { if request == generation { busy = false } }
    do {
      if !more {
        page = 0
        items = []
        undoID = nil
        if let token {
          let loaded = try await api.announcementState(token: token)
          guard request == generation else { return }
          state = loaded
        } else {
          state =
            (UserDefaults.standard.data(forKey: key).flatMap {
              try? JSONDecoder().decode(AnnouncementInboxState.self, from: $0)
            }) ?? AnnouncementInboxState()
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
  func change(_ action: String, id: String? = nil, through: SanctuaryAnnouncement? = nil) async {
    guard !busy, let api else { return }
    let request = generation
    busy = true
    error = nil
    defer { if request == generation { busy = false } }
    do {
      let change = AnnouncementChange(
        action: action, ids: id.map { [$0] } ?? [],
        throughId: action == "clearAll"
          ? (through ?? items.max(by: { $0.published_at < $1.published_at }))?.id : nil)
      if let token {
        let loaded = try await api.changeAnnouncements(change, token: token)
        guard request == generation else { return }
        state = loaded
      } else {
        if action == "read", let id { state.readIds = Array(Set(state.readIds + [id])) }
        if action == "clear", let id { state.clearedIds = Array(Set(state.clearedIds + [id])) }
        if action == "restore", let id { state.clearedIds.removeAll { $0 == id } }
        if action == "clearAll" {
          state.clearedBefore = max(
            state.clearedBefore,
            (through ?? items.max(by: { $0.published_at < $1.published_at }))?.published_at ?? 0)
        }
        UserDefaults.standard.set(try JSONEncoder().encode(state), forKey: key)
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
  @State private var expanded: String?
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
  var body: some View {
    ZStack {
      AppBackdrop()
      ScrollView {
        VStack(alignment: .leading, spacing: 16) {
          HStack {
            Text(localization.t("inbox.title")).font(AppTheme.rounded(32, weight: .bold))
              .foregroundStyle(.white)
            Spacer()
            Button(localization.t("common.close")) { dismiss() }
          }
          HStack {
            Button(localization.t("inbox.refresh")) {
              Task {
                await store.load(
                  api: environment.apiClient, token: await account.authorizationToken())
              }
            }
            Spacer()
            Button(localization.t("inbox.clearAll")) {
              clearThrough = store.items.max(by: { $0.published_at < $1.published_at })
              confirmClear = true
            }.disabled(store.visible.isEmpty)
          }.buttonStyle(.bordered)
          if let error = store.error { Text(error).foregroundStyle(.red) }
          if store.busy { ProgressView() }
          if let id = store.undoID {
            Button(localization.t("inbox.undo")) { Task { await store.change("restore", id: id) } }
              .buttonStyle(.bordered)
          }
          if displayed.isEmpty && !store.busy {
            Text(localization.t("inbox.empty")).foregroundStyle(AppTheme.subtitleText)
          }
          ForEach(displayed) { row in
            VStack(alignment: .leading, spacing: 12) {
              Button {
                expanded = expanded == row.id ? nil : row.id
                Task { await store.change("read", id: row.id) }
              } label: {
                HStack {
                  if !store.state.readIds.contains(row.id) {
                    Circle().fill(AppTheme.tabActive).frame(width: 8, height: 8)
                  }
                  Text(row.title).font(AppTheme.rounded(20, weight: .bold))
                  Spacer()
                  Image(systemName: expanded == row.id ? "chevron.up" : "chevron.down")
                }
              }.buttonStyle(.plain)
              Text(Date(timeIntervalSince1970: row.published_at / 1000), style: .date).font(
                .caption
              ).foregroundStyle(AppTheme.subtitleText)
              Text(row.body).lineLimit(expanded == row.id ? nil : 3).foregroundStyle(
                AppTheme.subtitleText)
              HStack {
                Button(localization.t("inbox.open")) {
                  if let url = URL(string: row.destination), url.scheme == "https",
                    url.host == "mydailysanctuary.com"
                      || (PlatformEnvironment.current() == .dev
                        && url.host == "dev.mydailysanctuary.com")
                  {
                    Task {
                      await store.change("read", id: row.id)
                      open(url)
                    }
                  }
                }
                Spacer()
                Button(localization.t("inbox.clear")) {
                  forcedID = nil
                  Task { await store.change("clear", id: row.id) }
                }
              }.buttonStyle(.bordered)
            }.padding(18).appGlassCard(cornerRadius: 24).foregroundStyle(.white)
          }
          if store.more {
            Button(localization.t("inbox.more")) {
              Task {
                await store.load(
                  api: environment.apiClient, token: await account.authorizationToken(), more: true)
              }
            }.buttonStyle(.bordered)
          }
        }.padding(18)
      }
    }.task {
      await store.load(api: environment.apiClient, token: await account.authorizationToken())
      if let id = initialID, UUID(uuidString: id) != nil {
        do {
          if !store.items.contains(where: { $0.id == id }) {
            store.items.insert(try await environment.apiClient.announcement(id: id), at: 0)
          }
          forcedID = id
          expanded = id
          await store.change("read", id: id)
        } catch { store.error = error.localizedDescription }
      }
    }
    .confirmationDialog(localization.t("inbox.confirmClear"), isPresented: $confirmClear) {
      Button(localization.t("inbox.clearAll"), role: .destructive) {
        forcedID = nil
        Task { await store.change("clearAll", through: clearThrough) }
      }
    }
  }
}
