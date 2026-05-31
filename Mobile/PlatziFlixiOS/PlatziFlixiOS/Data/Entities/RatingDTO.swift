import Foundation

struct RatingRequestDTO: Encodable {
    let userId: Int
    let rating: Int

    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
        case rating
    }
}

struct RatingResponseDTO: Codable {
    let id: Int
    let courseId: Int
    let userId: Int
    let rating: Int
    let createdAt: String
    let updatedAt: String

    enum CodingKeys: String, CodingKey {
        case id
        case courseId = "course_id"
        case userId = "user_id"
        case rating
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
}

struct RatingStatsDTO: Codable {
    let averageRating: Double
    let totalRatings: Int
    let ratingDistribution: [String: Int]

    enum CodingKeys: String, CodingKey {
        case averageRating = "average_rating"
        case totalRatings = "total_ratings"
        case ratingDistribution = "rating_distribution"
    }
}
