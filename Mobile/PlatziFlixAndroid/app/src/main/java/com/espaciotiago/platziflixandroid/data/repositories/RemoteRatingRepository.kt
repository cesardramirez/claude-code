package com.espaciotiago.platziflixandroid.data.repositories

import com.espaciotiago.platziflixandroid.data.entities.RatingRequestDTO
import com.espaciotiago.platziflixandroid.data.mappers.RatingMapper
import com.espaciotiago.platziflixandroid.data.network.ApiService
import com.espaciotiago.platziflixandroid.domain.models.CourseRating
import com.espaciotiago.platziflixandroid.domain.models.RatingStats
import com.espaciotiago.platziflixandroid.domain.repositories.RatingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of RatingRepository that fetches data from remote API
 */
class RemoteRatingRepository(
    private val apiService: ApiService
) : RatingRepository {

    override suspend fun getRatingStats(courseId: Int): Result<RatingStats> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRatingStats(courseId)
                if (response.isSuccessful) {
                    val statsDTO = response.body()
                    if (statsDTO != null) {
                        Result.success(RatingMapper.fromStatsDTO(statsDTO))
                    } else {
                        Result.failure(Exception("Empty response body for rating stats"))
                    }
                } else {
                    Result.failure(
                        Exception("Failed to fetch rating stats: ${response.code()} ${response.message()}")
                    )
                }
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }
    }

    override suspend fun getUserRating(courseId: Int, userId: Int): Result<CourseRating?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getUserRating(courseId, userId)
                when {
                    response.code() == 204 -> Result.success(null)
                    response.isSuccessful -> {
                        val ratingDTO = response.body()
                        if (ratingDTO != null) {
                            Result.success(RatingMapper.fromDTO(ratingDTO))
                        } else {
                            Result.success(null)
                        }
                    }
                    else -> Result.failure(
                        Exception("Failed to fetch user rating: ${response.code()} ${response.message()}")
                    )
                }
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }
    }

    override suspend fun upsertRating(courseId: Int, userId: Int, stars: Int): Result<CourseRating> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.upsertRating(
                    courseId = courseId,
                    request = RatingRequestDTO(userId = userId, rating = stars)
                )
                if (response.code() == 201) {
                    val ratingDTO = response.body()
                    if (ratingDTO != null) {
                        Result.success(RatingMapper.fromDTO(ratingDTO))
                    } else {
                        Result.failure(Exception("Empty response body for upsert rating"))
                    }
                } else {
                    Result.failure(
                        Exception("Failed to upsert rating: ${response.code()} ${response.message()}")
                    )
                }
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }
    }
}
