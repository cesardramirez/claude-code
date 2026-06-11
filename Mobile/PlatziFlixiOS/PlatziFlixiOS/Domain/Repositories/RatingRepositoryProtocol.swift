import Foundation

/// Protocol defining the contract for course rating data operations
protocol RatingRepository {

    /// Fetches the aggregated rating statistics for a course
    /// - Parameter courseId: The course identifier
    /// - Returns: Rating stats or throws an error
    func getRatingStats(courseId: Int) async throws -> RatingStats

    /// Fetches the rating a user has given to a course, if any
    /// - Parameters:
    ///   - courseId: The course identifier
    ///   - userId: The user identifier
    /// - Returns: The user's rating, or `nil` if the user has not rated the course
    func getUserRating(courseId: Int, userId: Int) async throws -> CourseRating?

    /// Creates or updates (upsert) a user's rating for a course
    /// - Parameters:
    ///   - courseId: The course identifier
    ///   - userId: The user identifier
    ///   - stars: The rating value (1-5)
    /// - Returns: The persisted rating
    func upsertRating(courseId: Int, userId: Int, stars: Int) async throws -> CourseRating
}
