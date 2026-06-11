package com.espaciotiago.platziflixandroid.presentation.coursedetail.viewmodel

import com.espaciotiago.platziflixandroid.domain.models.Course
import com.espaciotiago.platziflixandroid.domain.models.CourseRating
import com.espaciotiago.platziflixandroid.domain.models.RatingStats
import com.espaciotiago.platziflixandroid.domain.repositories.RatingRepository
import com.espaciotiago.platziflixandroid.presentation.coursedetail.state.RatingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CourseDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val testCourse = Course(
        id = 4,
        name = "Curso de Kotlin",
        description = "Aprende Kotlin desde cero",
        thumbnail = "thumb.jpg",
        slug = "curso-de-kotlin"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has no rating data and is loading`() = runTest {
        val viewModel = CourseDetailViewModel(testCourse, FakeRatingRepository())

        val state = viewModel.uiState.value

        assertTrue("Should be loading initially", state.isLoading)
        assertNull("ratingStats should be null initially", state.ratingStats)
        assertNull("userRating should be null initially", state.userRating)
    }

    @Test
    fun `successful load populates rating stats and user rating`() = runTest {
        val repository = FakeRatingRepository(
            ratingStatsResult = Result.success(RatingStats(averageRating = 4.35, totalRatings = 142)),
            userRatingResult = Result.success(CourseRating(id = 1, courseId = testCourse.id, userId = 1, rating = 4))
        )
        val viewModel = CourseDetailViewModel(testCourse, repository)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Should not be loading after success", state.isLoading)
        assertEquals(RatingStats(4.35, 142), state.ratingStats)
        assertEquals(4, state.userRating)
    }

    @Test
    fun `submitRating applies optimistic update before repository responds`() = runTest {
        val repository = FakeRatingRepository()
        val viewModel = CourseDetailViewModel(testCourse, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.submitRating(5)

        val state = viewModel.uiState.value
        assertEquals("userRating should update immediately", 5, state.userRating)
        assertEquals("ratingState should be LOADING immediately", RatingState.LOADING, state.ratingState)
    }

    @Test
    fun `submitRating rolls back userRating on error`() = runTest {
        val repository = FakeRatingRepository(
            userRatingResult = Result.success(CourseRating(id = 1, courseId = testCourse.id, userId = 1, rating = 3)),
            upsertResult = Result.failure(Exception("network error"))
        )
        val viewModel = CourseDetailViewModel(testCourse, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.submitRating(5)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("userRating should rollback to the previous value", 3, state.userRating)
        assertEquals(RatingState.ERROR, state.ratingState)
        assertEquals("network error", state.error)
    }

    @Test
    fun `submitRating transitions from SUCCESS to IDLE after the reset delay`() = runTest {
        val repository = FakeRatingRepository(
            ratingStatsResult = Result.success(RatingStats(averageRating = 4.0, totalRatings = 10))
        )
        val viewModel = CourseDetailViewModel(testCourse, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.submitRating(5)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(RatingState.SUCCESS, viewModel.uiState.value.ratingState)

        testDispatcher.scheduler.advanceTimeBy(CourseDetailViewModel.RATING_SUCCESS_RESET_DELAY_MS + 1)
        testDispatcher.scheduler.runCurrent()

        assertEquals(RatingState.IDLE, viewModel.uiState.value.ratingState)
    }

    private class FakeRatingRepository(
        private val ratingStatsResult: Result<RatingStats> = Result.success(RatingStats(0.0, 0)),
        private val userRatingResult: Result<CourseRating?> = Result.success(null),
        private val upsertResult: Result<CourseRating>? = null
    ) : RatingRepository {

        override suspend fun getRatingStats(courseId: Int): Result<RatingStats> = ratingStatsResult

        override suspend fun getUserRating(courseId: Int, userId: Int): Result<CourseRating?> = userRatingResult

        override suspend fun upsertRating(courseId: Int, userId: Int, stars: Int): Result<CourseRating> {
            return upsertResult ?: Result.success(
                CourseRating(id = 1, courseId = courseId, userId = userId, rating = stars)
            )
        }
    }
}
