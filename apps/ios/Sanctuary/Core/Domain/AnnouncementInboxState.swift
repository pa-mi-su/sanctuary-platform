import Foundation

struct AnnouncementInboxState: Codable, Equatable {
    var readIds: [String] = []
    var clearedIds: [String] = []
    var clearedBefore: Double = 0
    func isVisible(id: String, publishedAt: Double) -> Bool {
        !clearedIds.contains(id) && publishedAt > clearedBefore
    }
    func merging(_ other: Self) -> Self {
        .init(readIds: Array(Set(readIds + other.readIds)), clearedIds: Array(Set(clearedIds + other.clearedIds)), clearedBefore: max(clearedBefore, other.clearedBefore))
    }
    func applying(_ action: String, ids: [String], through: Double = 0) -> Self {
        var next = self
        switch action {
        case "read": next.readIds = Array(Set(readIds + ids))
        case "clear": next.clearedIds = Array(Set(clearedIds + ids))
        case "restore": next.clearedIds.removeAll { ids.contains($0) }
        case "clearAll": next.clearedBefore = max(clearedBefore, through)
        default: break
        }
        return next
    }
}

/// The member display snapshot survives logout, but is never uploaded as guest activity.
final class AnnouncementReceiptCache {
    private let defaults: UserDefaults
    private let key: String
    init(defaults: UserDefaults = .standard, key: String) { self.defaults = defaults; self.key = key }
    private func read(_ name: String) -> AnnouncementInboxState {
        defaults.data(forKey: name).flatMap { try? JSONDecoder().decode(AnnouncementInboxState.self, from: $0) } ?? .init()
    }
    private func write(_ value: AnnouncementInboxState, to name: String) throws { defaults.set(try JSONEncoder().encode(value), forKey: name) }
    var guestForUpload: AnnouncementInboxState { read(key) }
    var guestForDisplay: AnnouncementInboxState { forDisplay(guestForUpload.merging(read(key + "-member-display"))) }
    var needsFreshStart: Bool { defaults.object(forKey: key + "-installation-before") == nil && defaults.data(forKey: key) == nil && defaults.data(forKey: key + "-member-display") == nil }
    func initializeInstallation(latest: Double, fresh: Bool) {
        guard defaults.object(forKey: key + "-installation-before") == nil else { return }
        defaults.set(fresh ? max(0, latest) : 0, forKey: key + "-installation-before")
    }
    func forDisplay(_ state: AnnouncementInboxState) -> AnnouncementInboxState {
        var result = state
        result.clearedBefore = max(result.clearedBefore, defaults.double(forKey: key + "-installation-before"))
        return result
    }
    var revision: Int { defaults.integer(forKey: key + "-member-revision") }
    func rememberMember(_ state: AnnouncementInboxState, expectedRevision: Int? = nil) throws {
        if let expectedRevision, expectedRevision != revision { return }
        try write(state, to: key + "-member-display")
        defaults.set(revision + 1, forKey: key + "-member-revision")
    }
    func changeGuest(_ action: String, ids: [String], through: Double) throws -> AnnouncementInboxState {
        try write(guestForUpload.applying(action, ids: ids, through: through), to: key)
        if action == "restore" {
            try write(read(key + "-member-display").applying(action, ids: ids), to: key + "-member-display")
            defaults.set(revision + 1, forKey: key + "-member-revision")
        }
        return guestForDisplay
    }
}
