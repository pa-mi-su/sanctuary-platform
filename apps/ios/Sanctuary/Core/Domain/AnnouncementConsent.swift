import Foundation

/// A missing preference is not an opt-out. Never infer consent from OS permission.
final class AnnouncementConsent {
    private let defaults: UserDefaults
    private let saveBackup: (Data) throws -> Void
    private(set) var choice: Bool?
    private static let key = "sanctuary.announcements"

    init(defaults: UserDefaults, backup: Data?, saveBackup: @escaping (Data) throws -> Void) {
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
    var needsInvitation: Bool { choice == nil }
    func choose(_ enabled: Bool) throws {
        // Opt-outs take effect even when secure storage is unavailable. Opt-ins require persistence.
        if !enabled { defaults.set(false, forKey: Self.key); choice = false }
        try saveBackup(JSONEncoder().encode(enabled))
        defaults.set(enabled, forKey: Self.key)
        choice = enabled
    }
}
