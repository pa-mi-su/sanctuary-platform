import Foundation

struct SaintDocument: Decodable {
    let id: String
    let name: String?
    let mmdd: String?
    let feast: String?
    let summary: String?
    let biography: String?
    let prayers: [String]?
    let sources: [String]?
    let photoUrl: String?
    let name_es: String?
    let name_pl: String?
    let feast_es: String?
    let feast_pl: String?
    let summary_es: String?
    let summary_pl: String?
    let biography_es: String?
    let biography_pl: String?
}

struct NovenaDocument: Decodable {
    let id: String
    let title: String?
    let title_es: String?
    let title_pl: String?
    let description: String?
    let description_es: String?
    let description_pl: String?
    let intentions: [String]?
    let intentions_es: [String]?
    let intentions_pl: [String]?
    let durationDays: Int?
    let tags: [String]?
    let image: String?
    let days: [NovenaDayDocument]?
}

struct NovenaDayDocument: Decodable {
    let day: Int?
    let title: String?
    let title_es: String?
    let title_pl: String?
    let scripture: String?
    let scripture_es: String?
    let scripture_pl: String?
    let prayer: String?
    let prayer_es: String?
    let prayer_pl: String?
    let reflection: String?
    let reflection_es: String?
    let reflection_pl: String?
}
