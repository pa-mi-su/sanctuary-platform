import Foundation

struct AccountSession: Codable, Sendable {
    let accessToken: String
    let idToken: String
    let refreshToken: String?
    let tokenType: String
    let expiresAt: Date
    let email: String
    let displayName: String
    var accountUserID: String? = nil
}

