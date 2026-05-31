import Foundation

struct RatingMapper {

    static func toDomain(_ dto: RatingResponseDTO) -> CourseRating {
        return CourseRating(
            id: dto.id,
            courseId: dto.courseId,
            userId: dto.userId,
            rating: dto.rating
        )
    }

    static func toDomain(_ dto: RatingStatsDTO) -> RatingStats {
        return RatingStats(
            averageRating: dto.averageRating,
            totalRatings: dto.totalRatings
        )
    }
}
