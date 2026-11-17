// Compile with Entities.swift, RepositoryProtocols.swift and UserProgressStore.swift.
// This scheduler double avoids scheduling any real device notifications.
import Foundation
actor NovenaReminderScheduler {
    static let shared = NovenaReminderScheduler()
    func syncDigestReminder(activeCommitmentCount: Int, novenaEnabled: Bool, generalDailyEnabled: Bool) async {}
}
actor ProgressRepositoryDouble: UserProgressRepository {
    var calls = 0
    var upserts = 0
    var fail = false
    let initial = UserNovenaCommitment(userID: "first", novenaID: "novena", startedAt: Date(), currentDay: 4,
        completedDays: [1,2,3], reminder: ReminderConfig(enabled: false, morningHour: nil, eveningHour: nil, timeZoneID: "UTC"), status: .active, updatedAt: Date())
    func setFailure() { fail = true }
    func clearFailure() { fail = false }
    func count() -> Int { calls }
    func upsertCount() -> Int { upserts }
    func listFavorites(userID: String) async throws -> [UserFavorite] { [] }
    func addFavorite(userID: String, itemType: FavoriteItemType, itemID: String) async throws {}
    func removeFavorite(userID: String, itemType: FavoriteItemType, itemID: String) async throws {}
    func listNovenaCommitments(userID: String) async throws -> [UserNovenaCommitment] { userID == "first" ? [initial] : [] }
    func upsertNovenaCommitment(_ commitment: UserNovenaCommitment) async throws { upserts += 1 }
    func removeNovenaCommitment(userID: String, novenaID: String) async throws { try await Task.sleep(nanoseconds:200_000_000) }
    func completeNovenaDay(userID: String, novenaID: String, day: Int, completedAt: Date) async throws -> UserNovenaCommitment {
        calls += 1
        try await Task.sleep(nanoseconds: 200_000_000)
        if fail { throw URLError(.notConnectedToInternet) }
        return UserNovenaCommitment(userID:userID, novenaID:novenaID, startedAt:initial.startedAt, currentDay:day+1,
            completedDays:Array(1...day), reminder:initial.reminder, status:.active, updatedAt:Date())
    }
}
@main struct ProgressFeedbackCheck {
    @MainActor static func main() async {
        let repository = ProgressRepositoryDouble()
        let store = UserProgressStore(userProgressRepository: repository)
        await store.setAuthenticatedUser(id:"first")
        let save = Task { await store.completeCurrentDay(novenaID:"novena",totalDays:9) }
        try? await Task.sleep(nanoseconds:30_000_000)
        precondition(store.activeCommitment(for:"novena")?.currentDay == 5, "Feedback must not wait for network")
        await store.completeCurrentDay(novenaID:"novena",totalDays:9)
        await save.value
        let count = await repository.count(); precondition(count == 1, "Double tap must save once")
        await repository.setFailure()
        await store.completeCurrentDay(novenaID:"novena",totalDays:9)
        precondition(store.activeCommitment(for:"novena")?.currentDay == 5, "Failure must roll back")
        precondition(store.actionError != nil, "Failure must be visible")
        let late = Task { await store.completeCurrentDay(novenaID:"novena",totalDays:9) }
        try? await Task.sleep(nanoseconds:30_000_000)
        await store.setAuthenticatedUser(id:"second")
        await late.value
        precondition(store.commitments.isEmpty, "Late response must not leak into another account")
        await store.setAuthenticatedUser(id:"first")
        await store.completeCurrentDay(novenaID:"novena",totalDays:4)
        precondition(store.activeCommitment(for:"novena") != nil, "Failed final day must stay active")
        await repository.clearFailure()
        await store.completeCurrentDay(novenaID:"novena",totalDays:4)
        precondition(store.commitments.first?.status == .completed, "Final day must complete the novena")
        precondition(store.actionError == nil, "Successful retry must clear the old error")
        await store.setAuthenticatedUser(id:"second")
        await store.setAuthenticatedUser(id:"first")
        let beforeStop = await repository.upsertCount()
        let stop = Task { await store.stopNovena(novenaID:"novena") }
        try? await Task.sleep(nanoseconds:30_000_000)
        precondition(store.activeCommitment(for:"novena") == nil, "Stop must update immediately")
        await store.startNovena(novenaID:"novena")
        await stop.value
        let afterStop = await repository.upsertCount()
        precondition(beforeStop == afterStop, "Start must not race an in-flight stop")
        print("Progress feedback: immediate update, double-tap protection, rollback and account isolation passed")
    }
}
