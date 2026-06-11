import Foundation

/// API Endpoints for CourseRating operations
enum RatingAPIEndpoints {
    case upsertRating(courseId: Int, userId: Int, rating: Int)
    case getRatingStats(courseId: Int)
    case getUserRating(courseId: Int, userId: Int)
}

extension RatingAPIEndpoints: APIEndpoint {
    var baseURL: String {
        return "http://localhost:8000"
    }

    var path: String {
        switch self {
        case .upsertRating(let courseId, _, _):
            return "/courses/\(courseId)/ratings"
        case .getRatingStats(let courseId):
            return "/courses/\(courseId)/ratings/stats"
        case .getUserRating(let courseId, let userId):
            return "/courses/\(courseId)/ratings/user/\(userId)"
        }
    }

    var method: HTTPMethod {
        switch self {
        case .upsertRating:
            return .POST
        case .getRatingStats, .getUserRating:
            return .GET
        }
    }

    var headers: [String: String]? {
        return nil // Using default headers from APIEndpoint extension
    }

    var parameters: [String: Any]? {
        return nil
    }

    var body: Data? {
        switch self {
        case .upsertRating(_, let userId, let rating):
            let requestDTO = RatingRequestDTO(userId: userId, rating: rating)
            return try? JSONEncoder().encode(requestDTO)
        case .getRatingStats, .getUserRating:
            return nil
        }
    }
}
