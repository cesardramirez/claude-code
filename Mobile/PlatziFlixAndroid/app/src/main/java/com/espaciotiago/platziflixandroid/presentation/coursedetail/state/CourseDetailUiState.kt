package com.espaciotiago.platziflixandroid.presentation.coursedetail.state

import com.espaciotiago.platziflixandroid.domain.models.RatingStats

/**
 * Represents the lifecycle of a rating submission
 */
enum class RatingState {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR
}

/**
 * UI State for the Course Detail screen
 */
data class CourseDetailUiState(
    val isLoading: Boolean = true,
    val courseId: Int = 0,
    val courseName: String = "",
    val courseDescription: String = "",
    val courseThumbnail: String = "",
    val ratingStats: RatingStats? = null,
    val userRating: Int? = null,
    val ratingState: RatingState = RatingState.IDLE,
    val error: String? = null
)
