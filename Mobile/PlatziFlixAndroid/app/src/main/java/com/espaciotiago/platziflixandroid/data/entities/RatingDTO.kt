package com.espaciotiago.platziflixandroid.data.entities

import com.google.gson.annotations.SerializedName

data class RatingRequestDTO(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("rating") val rating: Int
)

data class RatingResponseDTO(
    @SerializedName("id") val id: Int,
    @SerializedName("course_id") val courseId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("rating") val rating: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class RatingStatsDTO(
    @SerializedName("average_rating") val averageRating: Double,
    @SerializedName("total_ratings") val totalRatings: Int,
    @SerializedName("rating_distribution") val ratingDistribution: Map<String, Int>
)
