import Foundation

enum SanctuaryAPIError: LocalizedError, Sendable {
    case invalidResponse
    case invalidURL
    case missingAccessToken
    case server(message: String)
    case transport(message: String)
    case decoding(message: String)

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "Sanctuary returned an unreadable response."
        case .invalidURL:
            return "Sanctuary is not configured with a valid API URL."
        case .missingAccessToken:
            return "Please sign in to continue."
        case .server(let message), .transport(let message), .decoding(let message):
            return message
        }
    }
}

struct APIAuthRegisterRequest: Encodable, Sendable {
    let firstName: String
    let lastName: String
    let email: String
    let password: String
}

struct APIAuthLoginRequest: Encodable, Sendable {
    let email: String
    let password: String
}

struct APIAuthConfirmRequest: Encodable, Sendable {
    let email: String
    let code: String
}

struct APIAuthResendRequest: Encodable, Sendable {
    let email: String
}

struct APIAuthRegistrationResponse: Decodable, Sendable {
    let email: String
    let displayName: String
    let confirmationRequired: Bool
}

struct APIAuthStatusResponse: Decodable, Sendable {
    let message: String
}

struct APIAuthSessionResponse: Decodable, Sendable {
    let accessToken: String
    let idToken: String
    let refreshToken: String?
    let tokenType: String
    let expiresIn: Int
    let email: String
    let displayName: String
}

struct APIUserProfileResponse: Decodable, Sendable {
    let userId: String
    let email: String?
    let firstName: String?
    let lastName: String?
    let displayName: String?
    let preferredLanguage: String?
    let avatarUrl: String?
    let timeZoneId: String?
    let novenaRemindersEnabled: Bool
    let feastRemindersEnabled: Bool
    let emailUpdatesEnabled: Bool
    let onboardingCompleted: Bool
    let favoriteSaintCount: Int
    let favoriteNovenaCount: Int
    let favoritePrayerCount: Int
    let activeNovenaCount: Int
    let completedNovenaCount: Int
}

struct APIUserFavoriteResponse: Decodable, Sendable {
    let itemType: String
    let itemId: String
    let createdAt: Date
}

struct APIUserNovenaCommitmentResponse: Decodable, Sendable {
    let novenaId: String
    let startedAt: Date
    let currentDay: Int
    let completedDays: [Int]
    let reminderEnabled: Bool
    let reminderMorningHour: Int?
    let reminderEveningHour: Int?
    let reminderTimeZoneId: String
    let status: String
    let updatedAt: Date
}

struct APIUserNovenaCommitmentRequest: Encodable, Sendable {
    let startedAt: Date
    let currentDay: Int
    let completedDays: [Int]
    let reminderEnabled: Bool
    let reminderMorningHour: Int?
    let reminderEveningHour: Int?
    let reminderTimeZoneId: String
    let status: String
}

private struct APIErrorEnvelope: Decodable {
    let message: String
}

actor SanctuaryAPIClient {
    private let baseURL: URL
    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder

    init(baseURL: URL, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.session = session

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        self.decoder = decoder

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        self.encoder = encoder
    }

    func register(_ request: APIAuthRegisterRequest) async throws -> APIAuthRegistrationResponse {
        try await performRequest(path: "/auth/register", method: "POST", body: request, token: nil)
    }

    func confirm(_ request: APIAuthConfirmRequest) async throws -> APIAuthStatusResponse {
        try await performRequest(path: "/auth/confirm", method: "POST", body: request, token: nil)
    }

    func resendConfirmation(email: String) async throws -> APIAuthStatusResponse {
        try await performRequest(path: "/auth/resend-confirmation", method: "POST", body: APIAuthResendRequest(email: email), token: nil)
    }

    func login(_ request: APIAuthLoginRequest) async throws -> APIAuthSessionResponse {
        try await performRequest(path: "/auth/login", method: "POST", body: request, token: nil)
    }

    func me(token: String) async throws -> APIUserProfileResponse {
        try await performRequest(path: "/me", method: "GET", body: Optional<String>.none, token: token)
    }

    func favorites(token: String) async throws -> [APIUserFavoriteResponse] {
        try await performRequest(path: "/me/favorites", method: "GET", body: Optional<String>.none, token: token)
    }

    func saveFavorite(itemType: String, itemId: String, token: String) async throws {
        try await performVoidRequest(path: "/me/favorites/\(itemType)/\(itemId)", method: "PUT", body: Optional<String>.none, token: token)
    }

    func deleteFavorite(itemType: String, itemId: String, token: String) async throws {
        try await performVoidRequest(path: "/me/favorites/\(itemType)/\(itemId)", method: "DELETE", body: Optional<String>.none, token: token)
    }

    func novenaCommitments(token: String) async throws -> [APIUserNovenaCommitmentResponse] {
        try await performRequest(path: "/me/novena-commitments", method: "GET", body: Optional<String>.none, token: token)
    }

    func saveNovenaCommitment(
        novenaId: String,
        request: APIUserNovenaCommitmentRequest,
        token: String
    ) async throws -> APIUserNovenaCommitmentResponse {
        try await performRequest(path: "/me/novena-commitments/\(novenaId)", method: "PUT", body: request, token: token)
    }

    func deleteNovenaCommitment(novenaId: String, token: String) async throws {
        try await performVoidRequest(path: "/me/novena-commitments/\(novenaId)", method: "DELETE", body: Optional<String>.none, token: token)
    }

    private func performRequest<Response: Decodable, Body: Encodable>(
        path: String,
        method: String,
        body: Body?,
        token: String?
    ) async throws -> Response {
        let request = try makeRequest(path: path, method: method, body: body, token: token)
        let (data, response) = try await execute(request)
        try validate(response: response, data: data)

        do {
            return try decoder.decode(Response.self, from: data)
        } catch {
            throw SanctuaryAPIError.decoding(message: "Sanctuary returned data we could not read.")
        }
    }

    private func performVoidRequest<Body: Encodable>(
        path: String,
        method: String,
        body: Body?,
        token: String?
    ) async throws {
        let request = try makeRequest(path: path, method: method, body: body, token: token)
        let (data, response) = try await execute(request)
        try validate(response: response, data: data)
    }

    private func makeRequest<Body: Encodable>(
        path: String,
        method: String,
        body: Body?,
        token: String?
    ) throws -> URLRequest {
        guard let url = URL(string: path, relativeTo: baseURL) else {
            throw SanctuaryAPIError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        if let token, !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let body {
            request.httpBody = try encoder.encode(body)
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        return request
    }

    private func execute(_ request: URLRequest) async throws -> (Data, URLResponse) {
        do {
            return try await session.data(for: request)
        } catch {
            throw SanctuaryAPIError.transport(message: "Sanctuary could not reach the API right now.")
        }
    }

    private func validate(response: URLResponse, data: Data) throws {
        guard let httpResponse = response as? HTTPURLResponse else {
            throw SanctuaryAPIError.invalidResponse
        }

        guard (200 ... 299).contains(httpResponse.statusCode) else {
            if let envelope = try? decoder.decode(APIErrorEnvelope.self, from: data),
               !envelope.message.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                throw SanctuaryAPIError.server(message: envelope.message)
            }

            throw SanctuaryAPIError.server(message: "Sanctuary could not complete that request right now.")
        }
    }
}
