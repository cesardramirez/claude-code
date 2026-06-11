package com.espaciotiago.platziflixandroid.domain.repositories

import com.espaciotiago.platziflixandroid.domain.models.CourseRating
import com.espaciotiago.platziflixandroid.domain.models.RatingStats

/**
 * Repository interface for CourseRating operations
 */
interface RatingRepository {

    /**
     * Retrieves the aggregated rating statistics for a course
     * @return Result containing rating stats or error
     */
    suspend fun getRatingStats(courseId: Int): Result<RatingStats>

    /**
     * Retrieves the rating a user has given to a course, if any
     * @return Result containing the user's rating, null if the user has not rated, or error
     */
    suspend fun getUserRating(courseId: Int, userId: Int): Result<CourseRating?>

    /**
     * Creates or updates (upsert) a user's rating for a course
     * @return Result containing the persisted rating or error
     */
    suspend fun upsertRating(courseId: Int, userId: Int, stars: Int): Result<CourseRating>
}
