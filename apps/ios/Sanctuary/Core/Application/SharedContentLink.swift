import Foundation

enum SharedContentKind: String {
    case saint = "saints"
    case novena = "novenas"
    case prayer = "prayers"
}

struct SharedContentLink: Identifiable, Equatable {
    let kind: SharedContentKind
    let slug: String

    var id: String { "\(kind.rawValue)/\(slug)" }

    var url: URL {
        URL(string: "https://mydailysanctuary.com/\(kind.rawValue)/\(slug)")!
    }

    func shareText(title: String) -> String {
        let message: String
        switch kind {
        case .saint:
            message = "Look at this saint in Sanctuary: \(title)"
        case .novena:
            message = "Pray this novena with me in Sanctuary: \(title)"
        case .prayer:
            message = "Pray this with me in Sanctuary: \(title)"
        }

        return "\(message)\n\(url.absoluteString)"
    }

    static func parse(_ url: URL) -> SharedContentLink? {
        guard url.host?.lowercased() == "mydailysanctuary.com" else {
            return nil
        }

        let components = url.pathComponents.filter { $0 != "/" }
        guard components.count >= 2,
              let kind = SharedContentKind(rawValue: components[0]),
              !components[1].isEmpty else {
            return nil
        }

        return SharedContentLink(kind: kind, slug: components[1])
    }
}
