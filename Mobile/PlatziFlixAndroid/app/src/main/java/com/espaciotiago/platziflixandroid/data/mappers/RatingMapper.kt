package com.espaciotiago.platziflixandroid.data.mappers

import com.espaciotiago.platziflixandroid.data.entities.RatingResponseDTO
import com.espaciotiago.platziflixandroid.data.entities.RatingStatsDTO
import com.espaciotiago.platziflixandroid.domain.models.CourseRating
import com.espaciotiago.platziflixandroid.domain.models.RatingStats

object RatingMapper {

    fun fromDTO(dto: RatingResponseDTO): CourseRating {
        return CourseRating(
            id = dto.id,
            courseId = dto.courseId,
            userId = dto.userId,
            rating = dto.rating
        )
    }

    fun fromStatsDTO(dto: RatingStatsDTO): RatingStats {
        return RatingStats(
            averageRating = dto.averageRating,
            totalRatings = dto.totalRatings
        )
    }
}
