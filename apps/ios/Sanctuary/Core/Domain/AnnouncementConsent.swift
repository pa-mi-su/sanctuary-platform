import Foundation

/// A missing preference is not an opt-out. Never infer consent from OS permission.
final class AnnouncementConsent {
    private let defaults: UserDefaults
    private let version: String?
    private let saveBackup: (Data) throws -> Void
    private(set) var choice: Bool?
    private static let key = "sanctuary.announcements"

    init(defaults: UserDefaults, backup: Data?, version: String? = nil, saveBackup: @escaping (Data) throws -> Void) {
        self.version = version
        self.defaults = defaults
        self.saveBackup = saveBackup
        if defaults.object(forKey: Self.key) != nil {
            choice = defaults.bool(forKey: Self.key)
        } else if let backup, let restored = try? JSONDecoder().decode(Bool.self, from: backup) {
            choice = restored
            defaults.set(restored, forKey: Self.key)
        }
        if let choice { try? saveBackup(JSONEncoder().encode(choice)) }
    }
    var needsInvitation: Bool { choice == nil || (choice == false && version != nil && defaults.string(forKey: Self.key + ".invitation-version") != version) }
    func choose(_ enabled: Bool) throws {
        // Opt-outs take effect even when secure storage is unavailable. Opt-ins require persistence.
        if !enabled { defaults.set(false, forKey: Self.key); choice = false }
        try saveBackup(JSONEncoder().encode(enabled))
        defaults.set(enabled, forKey: Self.key)
        choice = enabled
        if let version { defaults.set(version, forKey: Self.key + ".invitation-version") }
    }
}

/// Coalesce concurrent lifecycle/token syncs into one system permission request.
enum AnnouncementPermissionState: Equatable { case request, allowed, blocked }
@MainActor final class AnnouncementPermissionRecovery {
    private var pending: Task<Bool, Never>?
    func resolve(enabled: Bool, state: AnnouncementPermissionState, request: @escaping @MainActor () async -> Bool) async -> Bool {
        guard enabled else { return false }
        switch state {
        case .allowed: return true
        case .blocked: return false
        case .request:
            if let pending { return await pending.value }
            let task = Task { await request() }
            pending = task
            let allowed = await task.value
            pending = nil
            return allowed
        }
    }
}
