import Foundation

/// Remote implementation of RatingRepository
/// Fetches and persists course rating data using NetworkService
final class RemoteRatingRepository: RatingRepository {

    // MARK: - Properties
    private let networkService: NetworkService

    // MARK: - Initialization
    init(networkService: NetworkService = NetworkManager.shared) {
        self.networkService = networkService
    }

    // MARK: - RatingRepository Implementation

    /// Fetches the aggregated rating statistics for a course.
    /// `average_rating == 0.0` is a valid response when the course has no ratings.
    func getRatingStats(courseId: Int) async throws -> RatingStats {
        let endpoint = RatingAPIEndpoints.getRatingStats(courseId: courseId)
        let dto = try await networkService.request(endpoint, responseType: RatingStatsDTO.self)
        return RatingMapper.toDomain(dto)
    }

    /// Fetches the rating a user has given to a course.
    /// The backend responds `204 No Content` (empty body) when the user has not rated yet.
    func getUserRating(courseId: Int, userId: Int) async throws -> CourseRating? {
        let endpoint = RatingAPIEndpoints.getUserRating(courseId: courseId, userId: userId)
        let data = try await networkService.request(endpoint)

        guard !data.isEmpty else { return nil }

        do {
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            let dto = try decoder.decode(RatingResponseDTO.self, from: data)
            return RatingMapper.toDomain(dto)
        } catch {
            throw NetworkError.decodingError(error)
        }
    }

    /// Creates or updates (upsert) a user's rating for a course.
    /// The backend always responds `201 Created`, even when updating an existing rating.
    func upsertRating(courseId: Int, userId: Int, stars: Int) async throws -> CourseRating {
        let endpoint = RatingAPIEndpoints.upsertRating(courseId: courseId, userId: userId, rating: stars)
        let dto = try await networkService.request(endpoint, responseType: RatingResponseDTO.self)
        return RatingMapper.toDomain(dto)
    }
}
