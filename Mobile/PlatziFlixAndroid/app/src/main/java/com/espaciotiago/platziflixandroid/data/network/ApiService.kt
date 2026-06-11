package com.espaciotiago.platziflixandroid.data.network

import com.espaciotiago.platziflixandroid.data.entities.CourseDTO
import com.espaciotiago.platziflixandroid.data.entities.RatingRequestDTO
import com.espaciotiago.platziflixandroid.data.entities.RatingResponseDTO
import com.espaciotiago.platziflixandroid.data.entities.RatingStatsDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * API service interface for course-related endpoints
 */
interface ApiService {

    /**
     * Fetches all courses from the API
     * @return Response containing list of courses
     */
    @GET("courses")
    suspend fun getAllCourses(): Response<List<CourseDTO>>

    /**
     * Creates or updates (upsert) a rating for a course
     * @return Response containing the persisted rating, always 201 on success
     */
    @POST("courses/{courseId}/ratings")
    suspend fun upsertRating(
        @Path("courseId") courseId: Int,
        @Body request: RatingRequestDTO
    ): Response<RatingResponseDTO>

    /**
     * Fetches the aggregated rating statistics for a course
     * @return Response containing rating stats
     */
    @GET("courses/{courseId}/ratings/stats")
    suspend fun getRatingStats(
        @Path("courseId") courseId: Int
    ): Response<RatingStatsDTO>

    /**
     * Fetches the rating a user has given to a course
     * @return Response containing the user's rating, or 204 if the user has not rated
     */
    @GET("courses/{courseId}/ratings/user/{userId}")
    suspend fun getUserRating(
        @Path("courseId") courseId: Int,
        @Path("userId") userId: Int
    ): Response<RatingResponseDTO>
}