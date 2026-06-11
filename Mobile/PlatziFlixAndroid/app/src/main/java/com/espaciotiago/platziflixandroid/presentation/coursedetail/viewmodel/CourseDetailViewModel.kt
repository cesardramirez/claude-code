package com.espaciotiago.platziflixandroid.presentation.coursedetail.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.espaciotiago.platziflixandroid.domain.models.Course
import com.espaciotiago.platziflixandroid.domain.repositories.RatingRepository
import com.espaciotiago.platziflixandroid.presentation.coursedetail.state.CourseDetailUiState
import com.espaciotiago.platziflixandroid.presentation.coursedetail.state.RatingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Course Detail screen.
 * Holds the course information (received via factory to avoid an extra network
 * request) and manages the rating stats / user rating state.
 */
class CourseDetailViewModel(
    private val course: Course,
    private val ratingRepository: RatingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CourseDetailUiState(
            isLoading = true,
            courseId = course.id,
            courseName = course.name,
            courseDescription = course.description,
            courseThumbnail = course.thumbnail
        )
    )
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadRatingStats()
            loadUserRating()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Submits a new rating using the optimistic update pattern:
     * 1. Saves the previous rating in case a rollback is needed
     * 2. Immediately reflects the new rating and a LOADING state
     * 3. Persists the rating in the background
     * 4a. On success: refreshes the stats, shows SUCCESS and resets to IDLE after a delay
     * 4b. On failure: rolls back to the previous rating and shows ERROR (no auto-reset)
     */
    fun submitRating(stars: Int) {
        val previousRating = _uiState.value.userRating

        _uiState.update {
            it.copy(userRating = stars, ratingState = RatingState.LOADING, error = null)
        }

        viewModelScope.launch {
            ratingRepository.upsertRating(course.id, HARDCODED_USER_ID, stars)
                .onSuccess {
                    loadRatingStats()
                    _uiState.update { it.copy(ratingState = RatingState.SUCCESS) }
                    delay(RATING_SUCCESS_RESET_DELAY_MS)
                    _uiState.update { it.copy(ratingState = RatingState.IDLE) }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            userRating = previousRating,
                            ratingState = RatingState.ERROR,
                            error = exception.message ?: "No se pudo guardar la calificación"
                        )
                    }
                }
        }
    }

    private suspend fun loadRatingStats() {
        ratingRepository.getRatingStats(course.id)
            .onSuccess { stats ->
                _uiState.update { it.copy(ratingStats = stats) }
            }
            .onFailure { exception ->
                _uiState.update { it.copy(error = exception.message ?: "No se pudieron cargar las estadísticas") }
            }
    }

    private suspend fun loadUserRating() {
        ratingRepository.getUserRating(course.id, HARDCODED_USER_ID)
            .onSuccess { rating ->
                _uiState.update { it.copy(userRating = rating?.rating) }
            }
            .onFailure { exception ->
                _uiState.update { it.copy(error = exception.message ?: "No se pudo cargar tu calificación") }
            }
    }

    companion object {
        const val HARDCODED_USER_ID = 1
        const val RATING_SUCCESS_RESET_DELAY_MS = 2000L
    }
}
