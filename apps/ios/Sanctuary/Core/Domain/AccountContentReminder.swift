import Foundation

struct AccountContentReminder: Codable, Identifiable, Sendable, Equatable {
    let kind: String
    let slug: String
    let title: String
    let hour: Int
    let minute: Int
    var id: String { "\(kind)/\(slug)" }
    func notificationID(owner: String) -> String { "sanctuary.item.\(owner).\(kind).\(slug)" }
    var time: Date {
        Date(timeIntervalSince1970: TimeInterval(hour * 3600 + minute * 60))
    }
    var formattedTime: String {
        let formatter = DateFormatter(); formatter.timeStyle = .short; formatter.timeZone = TimeZone(secondsFromGMT: 0)
        return formatter.string(from: time)
    }
}

