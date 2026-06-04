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
