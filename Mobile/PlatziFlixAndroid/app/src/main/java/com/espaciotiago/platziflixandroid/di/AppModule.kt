package com.espaciotiago.platziflixandroid.di

import com.espaciotiago.platziflixandroid.data.network.ApiService
import com.espaciotiago.platziflixandroid.data.network.NetworkModule
import com.espaciotiago.platziflixandroid.data.repositories.MockCourseRepository
import com.espaciotiago.platziflixandroid.data.repositories.RemoteCourseRepository
import com.espaciotiago.platziflixandroid.data.repositories.RemoteRatingRepository
import com.espaciotiago.platziflixandroid.domain.models.Course
import com.espaciotiago.platziflixandroid.domain.repositories.CourseRepository
import com.espaciotiago.platziflixandroid.domain.repositories.RatingRepository
import com.espaciotiago.platziflixandroid.presentation.coursedetail.viewmodel.CourseDetailViewModel
import com.espaciotiago.platziflixandroid.presentation.courses.viewmodel.CourseListViewModel

/**
 * Simple dependency injection module
 */
object AppModule {
    
    private const val USE_MOCK_DATA = false // Set to false to use real API
    
    private val apiService: ApiService by lazy {
        NetworkModule.provideApiService()
    }
    
    private val courseRepository: CourseRepository by lazy {
        if (USE_MOCK_DATA) {
            MockCourseRepository()
        } else {
            RemoteCourseRepository(apiService)
        }
    }

    private val ratingRepository: RatingRepository by lazy {
        RemoteRatingRepository(apiService)
    }

    /**
     * Provides CourseListViewModel instance
     */
    fun provideCourseListViewModel(): CourseListViewModel {
        return CourseListViewModel(courseRepository)
    }

    /**
     * Provides CourseDetailViewModel instance for the given course
     */
    fun provideCourseDetailViewModel(course: Course): CourseDetailViewModel {
        return CourseDetailViewModel(course, ratingRepository)
    }
}