import Foundation
@main struct VerifyConsent {
 @MainActor static func main() async throws {
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
  let recovery = AnnouncementPermissionRecovery()
  var requests = 0
  let off = await recovery.resolve(enabled:false,state:.request) { requests += 1; return true }
  assert(!off && requests == 0)
  let permitted = await recovery.resolve(enabled:true,state:.allowed) { requests += 1; return false }
  assert(permitted && requests == 0)
  let denied = await recovery.resolve(enabled:true,state:.blocked) { requests += 1; return true }
  assert(!denied && requests == 0)
  let restored = await recovery.resolve(enabled:true,state:.request) { requests += 1; return true }
  assert(restored && requests == 1)
  let refused = await recovery.resolve(enabled:true,state:.request) { requests += 1; return false }
  assert(!refused && requests == 2)
  var resume: CheckedContinuation<Bool,Never>?
  let first = Task { await recovery.resolve(enabled:true,state:.request) {
    requests += 1
    return await withCheckedContinuation { resume = $0 }
  } }
  while resume == nil { await Task.yield() }
  var secondStarted = false
  let second = Task { secondStarted = true; return await recovery.resolve(enabled:true,state:.request) { fatalError("Concurrent sync requested permission twice") } }
  while !secondStarted { await Task.yield() }
  resume?.resume(returning:true)
  let firstResult = await first.value
  let secondResult = await second.value
  assert(firstResult && secondResult && requests == 3)
  print("Permission recovery: restored ON, OFF, allowed, denied and concurrent requests passed")
  print("Announcement consent: unknown, migration, update, recovery, opt-out and storage failure checks passed")
 }
}
