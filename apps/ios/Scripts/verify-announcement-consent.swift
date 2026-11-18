import Foundation
@main struct VerifyConsent {
 static func main() throws {
  let name = "sanctuary-consent-test-" + UUID().uuidString
  let defaults = UserDefaults(suiteName: name)!
  defer { defaults.removePersistentDomain(forName: name) }
  var backup: Data?
  func open() -> AnnouncementConsent { AnnouncementConsent(defaults: defaults, backup: backup) { backup = $0 } }
  assert(open().needsInvitation) // Existing app with missing consent must be invited.
  try open().choose(true)
  assert(open().choice == true && !open().needsInvitation) // Update/relaunch preserves opt-in.
  defaults.removeObject(forKey: "sanctuary.announcements")
  assert(open().choice == true) // Recover missing primary preference.
  try open().choose(false)
  assert(open().choice == false && !open().needsInvitation) // Never override explicit opt-out.
  defaults.removeObject(forKey: "sanctuary.announcements")
  assert(open().choice == false) // Recovery also preserves opt-out.
  defaults.set(true, forKey: "sanctuary.announcements")
  assert(open().choice == true) // Legacy choice migrates ahead of old recovery state.
  let upgrade = AnnouncementConsent(defaults:defaults,backup:backup,version:"1.0.37") { backup = $0 }
  try upgrade.choose(false)
  assert(!upgrade.needsInvitation)
  assert(!AnnouncementConsent(defaults:defaults,backup:backup,version:"1.0.37") { backup = $0 }.needsInvitation)
  assert(AnnouncementConsent(defaults:defaults,backup:backup,version:"1.0.38") { backup = $0 }.needsInvitation)
  try upgrade.choose(true)
  assert(!AnnouncementConsent(defaults:defaults,backup:backup,version:"1.0.38") { backup = $0 }.needsInvitation)
  enum Failed: Error { case storage }
  let failed = AnnouncementConsent(defaults: defaults, backup: backup) { _ in throw Failed.storage }
  do { try failed.choose(false); fatalError("Storage failure must not appear successful") } catch {}
  assert(failed.choice == false && !defaults.bool(forKey: "sanctuary.announcements"))
  do { try failed.choose(true); fatalError("Failed opt-in must not enable alerts") } catch {}
  assert(failed.choice == false && !defaults.bool(forKey: "sanctuary.announcements"))
  print("Announcement consent: unknown, migration, update, recovery, opt-out and storage failure checks passed")
 }
}
