// Compile with Entities.swift, RepositoryProtocols.swift and UserProgressStore.swift.
// The repository gates each mutation explicitly: correctness never depends on runner speed.
import Foundation
actor NovenaReminderScheduler {
    static let shared = NovenaReminderScheduler()
    func syncDigestReminder(activeCommitmentCount: Int, novenaEnabled: Bool, generalDailyEnabled: Bool) async {}
}
actor ProgressRepositoryDouble: UserProgressRepository {
    private var calls = 0
    private var upserts = 0
    private var fail = false
    private var saveGate: CheckedContinuation<Void, Never>?
    private var stopGate: CheckedContinuation<Void, Never>?
    let initial = UserNovenaCommitment(userID: "first", novenaID: "novena", startedAt: Date(), currentDay: 4,
        completedDays: [1,2,3], reminder: ReminderConfig(enabled: false, morningHour: nil, eveningHour: nil, timeZoneID: "UTC"), status: .active, updatedAt: Date())
    func setFailure(_ value: Bool) { fail = value }
    func count() -> Int { calls }
    func upsertCount() -> Int { upserts }
    func waitForSave() async { while saveGate == nil { await Task.yield() } }
    func finishSave() { let gate = saveGate; saveGate = nil; gate?.resume() }
    func waitForStop() async { while stopGate == nil { await Task.yield() } }
    func finishStop() { let gate = stopGate; stopGate = nil; gate?.resume() }
    func listFavorites(userID: String) async throws -> [UserFavorite] { [] }
    func addFavorite(userID: String, itemType: FavoriteItemType, itemID: String) async throws {}
    func removeFavorite(userID: String, itemType: FavoriteItemType, itemID: String) async throws {}
    func listNovenaCommitments(userID: String) async throws -> [UserNovenaCommitment] { userID == "first" ? [initial] : [] }
    func upsertNovenaCommitment(_ commitment: UserNovenaCommitment) async throws { upserts += 1 }
    func removeNovenaCommitment(userID: String, novenaID: String) async throws {
        await withCheckedContinuation { stopGate = $0 }
    }
    func completeNovenaDay(userID: String, novenaID: String, day: Int, completedAt: Date) async throws -> UserNovenaCommitment {
        calls += 1
        await withCheckedContinuation { saveGate = $0 }
        if fail { throw URLError(.notConnectedToInternet) }
        return UserNovenaCommitment(userID:userID, novenaID:novenaID, startedAt:initial.startedAt, currentDay:day+1,
            completedDays:Array(1...day), reminder:initial.reminder, status:.active, updatedAt:Date())
    }
}
@main struct ProgressFeedbackCheck {
    @MainActor static func main() async {
        let watchdog = Task.detached {
            do { try await Task.sleep(nanoseconds: 30_000_000_000) } catch { return }
            fatalError("Timed out waiting for a progress mutation to reach the repository")
        }
        defer { watchdog.cancel() }
        let repository = ProgressRepositoryDouble()
        let store = UserProgressStore(userProgressRepository: repository)
        await store.setAuthenticatedUser(id:"first")
        let save = Task { await store.completeCurrentDay(novenaID:"novena",totalDays:9) }
        await repository.waitForSave()
        precondition(store.activeCommitment(for:"novena")?.currentDay == 5, "Feedback must not wait for network")
        await store.completeCurrentDay(novenaID:"novena",totalDays:9)
        await repository.finishSave(); await save.value
        let count = await repository.count(); precondition(count == 1, "Double tap must save once")

        await repository.setFailure(true)
        let failed = Task { await store.completeCurrentDay(novenaID:"novena",totalDays:9) }
        await repository.waitForSave(); await repository.finishSave(); await failed.value
        precondition(store.activeCommitment(for:"novena")?.currentDay == 5, "Failure must roll back")
        precondition(store.actionError != nil, "Failure must be visible")

        let late = Task { await store.completeCurrentDay(novenaID:"novena",totalDays:9) }
        await repository.waitForSave()
        await store.setAuthenticatedUser(id:"second")
        await repository.finishSave(); await late.value
        precondition(store.commitments.isEmpty, "Late response must not leak into another account")

        await store.setAuthenticatedUser(id:"first")
        let finalFailure = Task { await store.completeCurrentDay(novenaID:"novena",totalDays:4) }
        await repository.waitForSave(); await repository.finishSave(); await finalFailure.value
        precondition(store.activeCommitment(for:"novena") != nil, "Failed final day must stay active")
        await repository.setFailure(false)
        let finalSuccess = Task { await store.completeCurrentDay(novenaID:"novena",totalDays:4) }
        await repository.waitForSave(); await repository.finishSave(); await finalSuccess.value
        precondition(store.commitments.first?.status == .completed, "Final day must complete the novena")
        precondition(store.actionError == nil, "Successful retry must clear the old error")

        await store.setAuthenticatedUser(id:"second")
        await store.setAuthenticatedUser(id:"first")
        let beforeStop = await repository.upsertCount()
        let stop = Task { await store.stopNovena(novenaID:"novena") }
        await repository.waitForStop()
        precondition(store.activeCommitment(for:"novena") == nil, "Stop must update immediately")
        await store.startNovena(novenaID:"novena")
        await repository.finishStop(); await stop.value
        let afterStop = await repository.upsertCount()
        precondition(beforeStop == afterStop, "Start must not race an in-flight stop")
        print("Progress feedback: immediate update, double-tap protection, rollback, account isolation and start/stop ordering passed")
    }
}
