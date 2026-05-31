package com.espaciotiago.platziflixandroid.domain.models

data class CourseRating(
    val id: Int,
    val courseId: Int,
    val userId: Int,
    val rating: Int
)
