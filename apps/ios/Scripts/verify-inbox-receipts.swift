import Foundation
@main struct VerifyInboxReceipts {
 static func main() throws {
  let baseline = AnnouncementInboxState(clearedBefore: 200)
  assert(!baseline.isVisible(id: "historical", publishedAt: 100))
  assert(!baseline.isVisible(id: "boundary", publishedAt: 200))
  assert(!baseline.isVisible(id: "older-page", publishedAt: 50))
  assert(baseline.isVisible(id: "future", publishedAt: 201))
  assert(!baseline.applying("clear", ids: ["future"]).isVisible(id: "future", publishedAt: 201))
  let suite="sanctuary-inbox-test-"+UUID().uuidString
  let defaults=UserDefaults(suiteName:suite)!
  defer { defaults.removePersistentDomain(forName:suite) }
  let fresh = AnnouncementReceiptCache(defaults:defaults,key:"fresh")
  assert(fresh.needsFreshStart)
  fresh.initializeInstallation(latest:200,fresh:fresh.needsFreshStart)
  assert(fresh.guestForDisplay.clearedBefore==200 && fresh.guestForUpload.clearedBefore==0)
  assert(!fresh.guestForDisplay.isVisible(id:"old",publishedAt:199))
  assert(fresh.guestForDisplay.isVisible(id:"new",publishedAt:201))
  fresh.initializeInstallation(latest:300,fresh:false)
  try fresh.rememberMember(.init())
  assert(fresh.forDisplay(.init()).clearedBefore==200)
  assert(fresh.guestForUpload.clearedBefore==0)
  let blank = AnnouncementReceiptCache(defaults:defaults,key:"blank")
  blank.initializeInstallation(latest:0,fresh:true)
  blank.initializeInstallation(latest:500,fresh:false)
  assert(!blank.needsFreshStart && blank.guestForDisplay.clearedBefore==0)
  let key="guest-dev"
  func cache() -> AnnouncementReceiptCache { .init(defaults:defaults,key:key) }
  _ = try cache().changeGuest("clear",ids:["guest-old"],through:0)
  assert(!cache().needsFreshStart)
  cache().initializeInstallation(latest:500,fresh:cache().needsFreshStart)
  assert(cache().guestForDisplay.clearedBefore==0)
  let uploaded=cache().guestForUpload
  try cache().rememberMember(uploaded.merging(.init(readIds:["member-read"],clearedIds:["member-old"])))
  defaults.removeObject(forKey:key) // Guest activity was claimed during sign-in.
  assert(cache().guestForUpload == .init())
  assert(Set(cache().guestForDisplay.clearedIds)==Set(["guest-old","member-old"]))
  // A successful signed-in clear-all remains effective after logout and process recreation.
  try cache().rememberMember(.init(readIds:["member-read"],clearedIds:["member-old"],clearedBefore:200))
  assert(cache().guestForDisplay.clearedBefore==200)
  assert(201>cache().guestForDisplay.clearedBefore) // Newer announcements remain visible.
  let olderRevision = cache().revision
  try cache().rememberMember(.init(clearedBefore:300))
  try cache().rememberMember(.init(), expectedRevision:olderRevision)
  assert(cache().guestForDisplay.clearedBefore==300) // Delayed reads cannot undo a clear.
  assert(cache().guestForUpload == .init()) // No member receipts leak into the next account.
  _ = try cache().changeGuest("clear",ids:["guest-new"],through:0)
  assert(cache().guestForUpload.clearedIds == ["guest-new"])
  _ = try cache().changeGuest("restore",ids:["guest-new"],through:0)
  assert(!cache().guestForDisplay.clearedIds.contains("guest-new"))
  // Signing into a different account uses its own server state, not the last member snapshot.
  try cache().rememberMember(.init())
  assert(!cache().guestForDisplay.clearedIds.contains("member-old"))
  assert(cache().guestForDisplay.clearedBefore==0)
  print("Inbox receipts: guest → member → clear → logout, relaunch, undo, new messages and account isolation passed")
 }
}
