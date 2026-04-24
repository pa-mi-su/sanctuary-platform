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

struct APIAuthForgotPasswordRequest: Encodable, Sendable {
    let email: String
}

struct APIAuthResetPasswordRequest: Encodable, Sendable {
    let email: String
    let code: String
    let newPassword: String
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

struct APIContentSaintSummaryResponse: Decodable, Sendable {
    let id: String
    let slug: String
    let name: String
    let feastMonth: Int
    let feastDay: Int
    let feastLabel: String
    let summary: String?
    let imageUrl: String?
}

struct APIContentSaintRangeDateResponse: Decodable, Sendable {
    let date: String
    let saints: [APIContentSaintSummaryResponse]
}

struct APIContentSaintDetailResponse: Decodable, Sendable {
    let id: String
    let slug: String
    let name: String
    let feastMonth: Int
    let feastDay: Int
    let feastLabel: String
    let summary: String?
    let biography: String?
    let imageUrl: String?
    let sources: [String]
}

struct APIContentNovenaSummaryResponse: Decodable, Sendable {
    let id: String
    let slug: String
    let title: String
    let description: String
    let durationDays: Int
    let intentions: [String]?
    let imageUrl: String?
}

struct APIContentNovenaDayDetailResponse: Decodable, Sendable {
    let dayNumber: Int
    let title: String?
    let scripture: String?
    let prayer: String?
    let reflection: String?
    let body: String?
}

struct APIContentNovenaDetailResponse: Decodable, Sendable {
    let id: String
    let slug: String
    let title: String
    let description: String
    let durationDays: Int
    let imageUrl: String?
    let tags: [String]
    let intentions: [String]
    let days: [APIContentNovenaDayDetailResponse]
}

struct APIContentNovenaCalendarDateResponse: Decodable, Sendable {
    let date: String
    let novenas: [APIContentNovenaSummaryResponse]
    let startingNovena: APIContentNovenaSummaryResponse?
}

struct APINovenaServingWindowResponse: Decodable, Sendable {
    let novenaId: String
    let startDate: String
    let endDate: String
    let feastDate: String
}

struct APILiturgicalDayResponse: Decodable, Sendable {
    let date: String
    let season: String
    let primaryRank: String
    let observances: [String]
    let readingsUrl: String?
}

struct APIPrayerSummaryResponse: Decodable, Sendable {
    let id: String
    let slug: String
    let title: String
    let bodyPreview: String
    let category: String
    let imageUrl: String?
}

struct APIPrayerDetailResponse: Decodable, Sendable {
    let id: String
    let slug: String
    let title: String
    let alternateTitle: String?
    let body: String
    let note: String?
    let category: String
    let imageUrl: String?
    let sourceTitle: String?
    let sourceType: String?
    let tags: [String]
}

private struct APIErrorEnvelope: Decodable {
    let message: String
}

actor SanctuaryAPIClient {
    private let baseURL: URL
    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder
    private let queryDateFormatter: DateFormatter

    init(baseURL: URL, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.session = session

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        self.decoder = decoder

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        self.encoder = encoder

        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyy-MM-dd"
        self.queryDateFormatter = formatter
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

    func forgotPassword(email: String) async throws -> APIAuthStatusResponse {
        try await performRequest(path: "/auth/forgot-password", method: "POST", body: APIAuthForgotPasswordRequest(email: email), token: nil)
    }

    func resetPassword(email: String, code: String, newPassword: String) async throws -> APIAuthStatusResponse {
        try await performRequest(
            path: "/auth/reset-password",
            method: "POST",
            body: APIAuthResetPasswordRequest(email: email, code: code, newPassword: newPassword),
            token: nil
        )
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

    func listSaints(
        locale: ContentLocale,
        feastDate: FeastDateFilter?,
        query: String?
    ) async throws -> [APIContentSaintSummaryResponse] {
        var queryItems = [URLQueryItem(name: "lang", value: locale.rawValue)]
        let path: String
        if let feastDate {
            path = "/content/saints"
            queryItems.append(URLQueryItem(name: "month", value: String(feastDate.month)))
            queryItems.append(URLQueryItem(name: "day", value: String(feastDate.day)))
        } else {
            path = "/content/saints/search"
            if let query {
                queryItems.append(URLQueryItem(name: "query", value: query))
            }
        }

        return try await performRequest(
            path: path,
            queryItems: queryItems,
            method: "GET",
            body: Optional<String>.none,
            token: nil
        )
    }

    func listSaintsInRange(
        locale: ContentLocale,
        startDate: Date,
        endDate: Date
    ) async throws -> [APIContentSaintRangeDateResponse] {
        let queryItems = [
            URLQueryItem(name: "lang", value: locale.rawValue),
            URLQueryItem(name: "start", value: queryDateFormatter.string(from: startDate)),
            URLQueryItem(name: "end", value: queryDateFormatter.string(from: endDate)),
        ]

        return try await performRequest(
            path: "/content/saints/range",
            queryItems: queryItems,
            method: "GET",
            body: Optional<String>.none,
            token: nil
        )
    }

    func listNovenas(
        locale: ContentLocale,
        query: String?
    ) async throws -> [APIContentNovenaSummaryResponse] {
        var queryItems = [URLQueryItem(name: "lang", value: locale.rawValue)]
        if let query, !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            queryItems.append(URLQueryItem(name: "query", value: query))
        }

        return try await performRequest(
            path: "/content/novenas",
            queryItems: queryItems,
            method: "GET",
            body: Optional<String>.none,
            token: nil
        )
    }

    func searchNovenasByIntentions(
        locale: ContentLocale,
        query: String
    ) async throws -> [APIContentNovenaSummaryResponse] {
        let queryItems = [
            URLQueryItem(name: "lang", value: locale.rawValue),
            URLQueryItem(name: "query", value: query)
        ]

        return try await performRequest(
            path: "/content/novenas/intentions",
            queryItems: queryItems,
            method: "GET",
            body: Optional<String>.none,
            token: nil
        )
    }

    func listNovenaCalendarDays(
        locale: ContentLocale,
        startDate: Date,
        endDate: Date
    ) async throws -> [APIContentNovenaCalendarDateResponse] {
        let queryItems = [
            URLQueryItem(name: "lang", value: locale.rawValue),
            URLQueryItem(name: "start", value: queryDateFormatter.string(from: startDate)),
            URLQueryItem(name: "end", value: queryDateFormatter.string(from: endDate))
        ]

        return try await performRequest(
            path: "/content/novenas/calendar",
            queryItems: queryItems,
            method: "GET",
            body: Optional<String>.none,
            token: nil
        )
    }

    func fetchNovena(slug: String, locale: ContentLocale) async throws -> APIContentNovenaDetailResponse? {
        try await performRequest(
            path: "/content/novenas/\(slug)",
            queryItems: [URLQueryItem(name: "lang", value: locale.rawValue)],
            method: "GET",
            body: Optional<String>.none,
            token: nil
        )
    }

    func fetchNovenaServingWindow(novenaID: String, year: Int) async throws -> APINovenaServingWindowResponse? {
        try await performRequest(
            path: "/calendar/novenas/\(novenaID)/window/\(year)",
            method: "GET",
            body: Optional<String>.none,
            token: nil
        )
    }

    func fetchLiturgicalDay(date: Date) async throws -> APILiturgicalDayResponse {
        try await performRequest(
            path: "/calendar/day/\(queryDateFormatter.string(from: date))",
            method: "GET",
            body: Optional<String>.none,
            token: nil
        )
    }

    func listLiturgicalDays(
        startDate: Date,
        endDate: Date
    ) async throws -> [APILiturgicalDayResponse] {
        let queryItems = [
            URLQueryItem(name: "start", value: queryDateFormatter.string(from: startDate)),
            URLQueryItem(name: "end", value: queryDateFormatter.string(from: endDate))
        ]

        return try await performRequest(
            path: "/calendar/range",
            queryItems: queryItems,
            method: "GET",
            body: Optional<String>.none,
            token: nil
        )
    }

    func fetchSaint(
        slug: String,
        locale: ContentLocale
    ) async throws -> APIContentSaintDetailResponse? {
        let request = try makeRequest(
            path: "/content/saints/\(slug)",
            queryItems: [URLQueryItem(name: "lang", value: locale.rawValue)],
            method: "GET",
            body: Optional<String>.none,
            token: nil
        )
        let (data, response) = try await execute(request)

        guard let http = response as? HTTPURLResponse else {
            throw SanctuaryAPIError.invalidResponse
        }

        if http.statusCode == 404 {
            return nil
        }

        try validate(response: response, data: data)

        do {
            return try decoder.decode(APIContentSaintDetailResponse.self, from: data)
        } catch {
            throw SanctuaryAPIError.decoding(message: "Sanctuary returned saint details we could not read.")
        }
    }

    func listPrayers(
        locale: ContentLocale,
        query: String?
    ) async throws -> [APIPrayerSummaryResponse] {
        var queryItems = [URLQueryItem(name: "lang", value: locale.rawValue)]
        if let query, !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            queryItems.append(URLQueryItem(name: "query", value: query))
        }

        return try await performRequest(
            path: "/content/prayers",
            queryItems: queryItems,
            method: "GET",
            body: Optional<String>.none,
            token: nil
        )
    }

    func fetchPrayer(
        slug: String,
        locale: ContentLocale
    ) async throws -> APIPrayerDetailResponse? {
        let request = try makeRequest(
            path: "/content/prayers/\(slug)",
            queryItems: [URLQueryItem(name: "lang", value: locale.rawValue)],
            method: "GET",
            body: Optional<String>.none,
            token: nil
        )
        let (data, response) = try await execute(request)

        guard let http = response as? HTTPURLResponse else {
            throw SanctuaryAPIError.invalidResponse
        }

        if http.statusCode == 404 {
            return nil
        }

        try validate(response: response, data: data)

        do {
            return try decoder.decode(APIPrayerDetailResponse.self, from: data)
        } catch {
            throw SanctuaryAPIError.decoding(message: "Sanctuary returned prayer details we could not read.")
        }
    }

    private func performRequest<Response: Decodable, Body: Encodable>(
        path: String,
        queryItems: [URLQueryItem] = [],
        method: String,
        body: Body?,
        token: String?
    ) async throws -> Response {
        let request = try makeRequest(path: path, queryItems: queryItems, method: method, body: body, token: token)
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
        queryItems: [URLQueryItem] = [],
        method: String,
        body: Body?,
        token: String?
    ) async throws {
        let request = try makeRequest(path: path, queryItems: queryItems, method: method, body: body, token: token)
        let (data, response) = try await execute(request)
        try validate(response: response, data: data)
    }

    private func makeRequest<Body: Encodable>(
        path: String,
        queryItems: [URLQueryItem],
        method: String,
        body: Body?,
        token: String?
    ) throws -> URLRequest {
        guard let resolvedURL = URL(string: path, relativeTo: baseURL),
              var components = URLComponents(url: resolvedURL, resolvingAgainstBaseURL: true)
        else {
            throw SanctuaryAPIError.invalidURL
        }

        if !queryItems.isEmpty {
            components.queryItems = queryItems
        }

        guard let url = components.url else {
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
